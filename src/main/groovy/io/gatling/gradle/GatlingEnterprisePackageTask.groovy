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

import io.gatling.plugin.pkg.Dependency
import io.gatling.plugin.pkg.EnterprisePackager
import java.util.stream.Collectors
import org.gradle.api.Project
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.plugins.JavaPluginExtension
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
  Set<Dependency> gatlingDependencies

  @Nested
  Set<Dependency> extraDependencies

  GatlingEnterprisePackageTask() {
    def envPackagePath = System.getenv("GATLING_ENTERPRISE_BUILDER_PACKAGE_PATH")
    if (envPackagePath != null) {
      def packagePath = new File(envPackagePath).absoluteFile
      destinationDirectory.fileValue(packagePath.parentFile)
      archiveFileName.set(packagePath.name)
    }
  }

  void init() {
    Set<ResolvedDependency> firstLevelModuleDependencies = project.configurations.gatlingRuntimeClasspath.resolvedConfiguration.getFirstLevelModuleDependencies()

    Map<Dependency.Id, Dependency> allDependenciesById = collectAllDependencies(firstLevelModuleDependencies)
    Set<Dependency.Id> gatlingDependencyTree = collectGatlingDependencyTree(firstLevelModuleDependencies)

    gatlingDependencies =
            gatlingDependencyTree
            .stream()
            .map { allDependenciesById.get(it) }
            .collect(Collectors.toSet())

    extraDependencies =
            allDependenciesById
            .entrySet()
            .stream()
            .filter { entry -> !gatlingDependencyTree.contains(entry.key)}
            .map { entry -> entry.value }
            .collect(Collectors.toSet())
  }

  private static Map<Dependency.Id, Dependency> collectAllDependencies(Set<ResolvedDependency> firstLevelModuleDependencies) {
    Map<Dependency.Id, Dependency> acc = new LinkedHashMap<>()
    collectAllDependenciesRec(firstLevelModuleDependencies, acc, new HashSet<ResolvedDependency>())
    return acc
  }

  private static void collectAllDependenciesRec(
          Set<ResolvedDependency> dependencies,
          Map<Dependency.Id, Dependency> acc,
          Set<ResolvedDependency> alreadyVisited) {

    for (dependency in dependencies)
      if (dependency.module?.id != null) {
        if (!alreadyVisited.contains(dependency)) {
          alreadyVisited.add(dependency)
          for (moduleArtifact in dependency.moduleArtifacts) {
            Dependency dep = new Dependency(
                    dependency.module.id.group,
                    dependency.module.id.name,
                    dependency.module.id.version,
                    moduleArtifact.classifier,
                    moduleArtifact.file
                    )
            acc.put(dep.id, dep)
          }
          collectAllDependenciesRec(dependency.children, acc, alreadyVisited)
        }
      }
  }

  private static Set<Dependency.Id> collectGatlingDependencyTree(Set<ResolvedDependency> firstLevelModuleDependencies) {
    var acc = new LinkedHashSet<Dependency.Id>()
    collectGatlingDependencyTreeRec(
            firstLevelModuleDependencies,
            acc,
            new HashSet<ModuleVersionIdentifier>()
            )
    return acc
  }

  private static void collectGatlingDependencyTreeRec(
          Set<ResolvedDependency> dependencies,
          Set<Dependency.Id> acc,
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

  private static void addAllArtifacts(ResolvedDependency dependency, Set<Dependency.Id> acc) {
    acc.addAll(
            dependency.moduleArtifacts
            .stream()
            .map { artifact ->
              new Dependency.Id(
                      dependency.module.id.group,
                      dependency.module.id.name,
                      dependency.module.id.version,
                      artifact.classifier
                      )
            }.collect(Collectors.toList()))
  }

  private static void collectAllDependencyTree(ResolvedDependency dependency, Set<Dependency.Id> acc) {
    addAllArtifacts(dependency, acc)
    for (child in dependency.children) {
      collectAllDependencyTree(child, acc)
    }
  }

  @TaskAction
  @Override
  protected void copy() {
    EnterprisePackager packager = new EnterprisePackager(new GradlePluginIO(logger).getLogger())
    packager.createEnterprisePackage(
            classDirectories,
            gatlingDependencies,
            extraDependencies,
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
}
