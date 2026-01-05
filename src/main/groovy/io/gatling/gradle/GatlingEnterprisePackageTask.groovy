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
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Must always run, target file is configured environment variable in enterprise packager")
// abstract because https://docs.gradle.org/current/userguide/upgrading_major_version_9.html#injection_getters_are_now_abstract
abstract class GatlingEnterprisePackageTask extends Jar {

  private static final Set<String> EXCLUDED_NETTY_ARTIFACTS = ["netty-all", "netty-resolver-dns-classes-macos", "netty-resolver-dns-native-macos"].asUnmodifiable()

  @Input
  String groupId = project.group.toString()

  @Input
  String artifactId = project.name

  @Input
  String artifactVersion = project.version.toString()

  @Input
  List<File> classDirectories = getClassDirectories(project)

  @InputDirectory
  File projectDir = project.getProjectDir()

  @Input
  Set<SerializableDependency> gatlingDependencies

  @Input
  Set<SerializableDependency> extraDependencies

  GatlingEnterprisePackageTask() {
    outputs.upToDateWhen { false }
  }

  void init() {
    Set<ResolvedDependency> firstLevelModuleDependencies = project.configurations.gatlingRuntimeClasspath.resolvedConfiguration.getFirstLevelModuleDependencies()

    Map<SerializableDependency.Id, SerializableDependency> allDependenciesById = collectAllDependencies(firstLevelModuleDependencies)
    Set<SerializableDependency.Id> gatlingDependencyTree = collectGatlingDependencyTree(firstLevelModuleDependencies)

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

  private static Map<SerializableDependency.Id, SerializableDependency> collectAllDependencies(Set<ResolvedDependency> firstLevelModuleDependencies) {
    Map<SerializableDependency.Id, SerializableDependency> acc = new HashMap<>()
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
            SerializableDependency.Id id = new SerializableDependency.Id(
                    dependency.module.id.group,
                    dependency.module.id.name,
                    moduleArtifact.classifier,
                    dependency.module.id.version
                    )
            acc.put(id, new SerializableDependency(id, moduleArtifact.file.path))
          }
          collectAllDependenciesRec(dependency.children, acc, alreadyVisited)
        }
      }
  }

  private static Set<SerializableDependency.Id> collectGatlingDependencyTree(Set<ResolvedDependency> firstLevelModuleDependencies) {
    var acc = new HashSet<SerializableDependency.Id>()
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
                      artifact.classifier,
                      dependency.module.id.version
                      )
            }.collect(Collectors.toList()))
  }

  private static void collectAllDependencyTree(ResolvedDependency dependency, Set<SerializableDependency.Id> acc) {
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
            gatlingDependencies.stream().map { it.toDependency() }.collect(Collectors.toSet()),
            extraDependencies.stream().map { it.toDependency() }.collect(Collectors.toSet()),
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
    }.flatten()
  }

  static class SerializableDependency implements Serializable {
    static class Id implements Serializable {
      String groupId
      String artifactId
      String classifier
      String version

      Id(
      String groupId,
      String artifactId,
      String classifier,
      String version
      ) {
        this.groupId = Objects.requireNonNull(groupId)
        this.artifactId = Objects.requireNonNull(artifactId)
        this.classifier = classifier
        this.version = Objects.requireNonNull(version)
      }

      boolean equals(o) {
        if (this.is(o)) return true
        if (!(o instanceof Id)) return false

        Id that = (Id) o

        if (artifactId != that.artifactId) return false
        if (classifier != that.classifier) return false
        if (groupId != that.groupId) return false
        if (version != that.version) return false

        return true
      }

      int hashCode() {
        int result
        result = groupId.hashCode()
        result = 31 * result + artifactId.hashCode()
        result = 31 * result + (classifier != null ? classifier.hashCode() : 0)
        result = 31 * result + version.hashCode()
        return result
      }
    }

    Id id
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
              id.groupId,
              id.artifactId,
              id.version,
              new File(file)
              )
    }

    boolean equals(o) {
      if (this.is(o)) return true
      if (!(o instanceof SerializableDependency)) return false

      SerializableDependency that = (SerializableDependency) o

      if (id != that.id) return false
      if (file != that.file) return false
      return true
    }

    int hashCode() {
      int result
      result = id.hashCode()
      result = 31 * result + file.hashCode()
      return result
    }
  }
}
