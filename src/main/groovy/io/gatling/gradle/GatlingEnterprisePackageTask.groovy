/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.gradle

import groovy.transform.EqualsAndHashCode
import io.gatling.plugin.pkg.Dependency
import io.gatling.plugin.pkg.EnterprisePackager
import java.util.stream.Collectors
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.jvm.tasks.Jar

@CacheableTask
// abstract because https://docs.gradle.org/current/userguide/upgrading_major_version_9.html#injection_getters_are_now_abstract
abstract class GatlingEnterprisePackageTask extends Jar {

  private static final Set<String> EXCLUDED_NETTY_ARTIFACTS = ["netty-all", "netty-resolver-dns-classes-macos", "netty-resolver-dns-native-macos"].asUnmodifiable()

  @Input
  String groupId = project.group.toString()

  @Input
  String artifactId = project.name

  @Input
  String artifactVersion = project.version.toString()

  @Classpath
  List<File> classDirectories = getClassDirectories(project)

  @Internal("only used to determine Git metadata (branch name and sha) which should not require task re-execution")
  File projectDir = project.getProjectDir()

  @Nested
  abstract Property<Dependencies> getDependencies()

  GatlingEnterprisePackageTask() {
    def envPackagePath = System.getenv("GATLING_ENTERPRISE_BUILDER_PACKAGE_PATH")
    if (envPackagePath != null) {
      def packagePath = new File(envPackagePath).absoluteFile
      destinationDirectory.fileValue(packagePath.parentFile)
      archiveFileName.set(packagePath.name)
    }
    def configuration = project.configurations.gatlingRuntimeClasspath
    dependencies.set(project.provider { new Dependencies(configuration) })
  }

  @TaskAction
  @Override
  protected void copy() {
    def deps = dependencies.get()
    EnterprisePackager packager = new EnterprisePackager(new GradlePluginIO(logger).getLogger())
    packager.createEnterprisePackage(
            classDirectories,
            deps.gatling.stream().map { it.toDependency() }.collect(Collectors.toSet()),
            deps.extra.stream().map { it.toDependency() }.collect(Collectors.toSet()),
            groupId,
            artifactId,
            artifactVersion,
            'gradle',
            getClass().getPackage().getImplementationVersion(),
            getArchiveFile().get().asFile,
            projectDir
            )
  }

  static List<File> getClassDirectories(Project rootProject) {
    rootProject.getAllprojects().collect { p ->
      JavaPluginExtension javaPluginExtension = p.getExtensions().getByType(JavaPluginExtension.class)
      javaPluginExtension.getSourceSets().collect { sourceSet ->
        sourceSet.output.asList()
      }
    }.flatten() as List<File>
  }

  static class Dependencies implements Serializable {

    @Nested
    Set<SerializableDependency> gatling

    @Nested
    Set<SerializableDependency> extra

    Dependencies(Configuration configuration) {
      Set<ResolvedDependency> firstLevelModuleDependencies = configuration.resolvedConfiguration.getFirstLevelModuleDependencies()

      Map<SerializableDependency.Id, SerializableDependency> allDependenciesById = collectAllDependencies(firstLevelModuleDependencies)
      Set<SerializableDependency.Id> gatlingDependencyTree = collectGatlingDependencyTree(firstLevelModuleDependencies)

      gatling =
              gatlingDependencyTree
              .stream()
              .map { allDependenciesById.get(it) }
              .collect(Collectors.toSet())

      extra =
              allDependenciesById
              .entrySet()
              .stream()
              .filter { entry -> !gatlingDependencyTree.contains(entry.key)}
              .map { entry -> entry.value }
              .collect(Collectors.toSet())
    }

    private static Map<SerializableDependency.Id, SerializableDependency> collectAllDependencies(Set<ResolvedDependency> firstLevelModuleDependencies) {
      Map<SerializableDependency.Id, SerializableDependency> acc = new LinkedHashMap<>()
      collectAllDependenciesRec(firstLevelModuleDependencies, acc, new HashSet<ResolvedDependency>())
      return acc
    }

    private static void collectAllDependenciesRec(
            Set<ResolvedDependency> dependencies,
            Map<SerializableDependency.Id, SerializableDependency> acc,
            Set<ResolvedDependency> alreadyVisited) {

      for (dependency in dependencies)
        if (dependency.module?.id != null) {
          if (!alreadyVisited.contains(dependency)) {
            alreadyVisited.add(dependency)
            for (moduleArtifact in dependency.moduleArtifacts) {
              SerializableDependency dep = new SerializableDependency(
                      new SerializableDependency.Id(
                      dependency.module.id.group,
                      dependency.module.id.name,
                      dependency.module.id.version,
                      moduleArtifact.classifier
                      ),
                      moduleArtifact.file.path
                      )
              acc.put(dep.id, dep)
            }
            collectAllDependenciesRec(dependency.children, acc, alreadyVisited)
          }
        }
    }

    private static Set<SerializableDependency.Id> collectGatlingDependencyTree(Set<ResolvedDependency> firstLevelModuleDependencies) {
      var acc = new LinkedHashSet<SerializableDependency.Id>()
      collectGatlingDependencyTreeRec(
              firstLevelModuleDependencies,
              acc,
              new HashSet<ModuleVersionIdentifier>()
              )
      return acc
    }

    private static void collectGatlingDependencyTreeRec(
            Set<ResolvedDependency> dependencies,
            Set<SerializableDependency.Id> acc,
            Set<ModuleVersionIdentifier> alreadyVisited) {

      for (dependency in dependencies) {
        if (dependency.module?.id != null) {
          if (!alreadyVisited.contains(dependency.module.id)) {
            alreadyVisited.add(dependency.module.id)
            if (dependency.module.id.group in ["io.gatling", "io.gatling.highcharts"]) {
              collectAllDependencyTree(dependency, acc)
            } else if (dependency.module.id.group == "io.netty" && EXCLUDED_NETTY_ARTIFACTS.contains(dependency.module.id.module)) {
              // so we don't ship them
              addAllArtifacts(dependency, acc)
            } else {
              collectGatlingDependencyTreeRec(dependency.children, acc, alreadyVisited)
            }
          }
        }
      }
    }

    private static void addAllArtifacts(ResolvedDependency dependency, Set<SerializableDependency.Id> acc) {
      acc.addAll(
              dependency.moduleArtifacts
              .stream()
              .map { artifact ->
                new SerializableDependency.Id(
                        dependency.module.id.group,
                        dependency.module.id.name,
                        dependency.module.id.version,
                        artifact.classifier
                        )
              }.collect(Collectors.toList()))
    }

    private static void collectAllDependencyTree(ResolvedDependency dependency, Set<SerializableDependency.Id> acc) {
      addAllArtifacts(dependency, acc)
      for (child in dependency.children) {
        collectAllDependencyTree(child, acc)
      }
    }
  }

  @EqualsAndHashCode
  static class SerializableDependency implements Serializable {

    @EqualsAndHashCode
    static class Id implements Serializable {
      String groupId
      String artifactId
      String version
      String classifier

      Id(
      String groupId,
      String artifactId,
      String version,
      String classifier
      ) {
        this.groupId = Objects.requireNonNull(groupId)
        this.artifactId = Objects.requireNonNull(artifactId)
        this.version = Objects.requireNonNull(version)
        this.classifier = classifier
      }
    }

    @Input
    Id id

    @Classpath
    String file

    SerializableDependency(
    Id id,
    String file
    ) {
      this.id = Objects.requireNonNull(id)
      this.file = Objects.requireNonNull(file)
    }

    Dependency toDependency() {
      return new Dependency(
              new Dependency.Id(
              id.groupId,
              id.artifactId,
              id.version,
              id.classifier
              ),
              new File(file)
              )
    }
  }
}
