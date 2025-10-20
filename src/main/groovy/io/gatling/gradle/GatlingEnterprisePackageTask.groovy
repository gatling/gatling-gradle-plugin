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
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.*
import org.gradle.jvm.tasks.Jar
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Must always run, target file is configured environment variable in enterprise packager")
// abstract because https://docs.gradle.org/current/userguide/upgrading_major_version_9.html#injection_getters_are_now_abstract
abstract class GatlingEnterprisePackageTask extends Jar {

  private static final Set<String> EXCLUDED_NETTY_ARTIFACTS = ["netty-all", "netty-resolver-dns-classes-macos", "netty-resolver-dns-native-macos"].asUnmodifiable()

  private static Set<Dependency> toDependencies(Set<ResolvedDependency> deps) {
    deps.collectMany { resolvedDependency ->
      resolvedDependency.getModuleArtifacts().collect { moduleArtifact ->
        new Dependency(
                resolvedDependency.module.id.group,
                resolvedDependency.module.id.name,
                resolvedDependency.module.id.version,
                moduleArtifact.file
                )
      }
    }.toSet()
  }

  @Classpath
  @Optional
  List<Configuration> configurations

  protected final Configuration gatlingRuntimeClasspathConfiguration = project.configurations.gatlingRuntimeClasspath
  protected final String groupId = project.group.toString()
  protected final String artifactId = project.name
  protected final String artifactVersion = project.version.toString()
  protected final Set<Project> allProjects = project.getAllprojects()

  GatlingEnterprisePackageTask() {
    outputs.upToDateWhen { false }
  }

  @TaskAction
  @Override
  protected void copy() {
    EnterprisePackager packager = new EnterprisePackager(new GradlePluginIO(logger).getLogger())
    Set<ResolvedDependency> firstLevelModuleDependencies = gatlingRuntimeClasspathConfiguration.resolvedConfiguration.getFirstLevelModuleDependencies()
    packager.createEnterprisePackage(
            getClassDirectories(),
            collectGatlingDependencies(firstLevelModuleDependencies),
            collectExtraDependencies(firstLevelModuleDependencies),
            groupId,
            artifactId,
            artifactVersion,
            'gradle',
            getClass().getPackage().getImplementationVersion(),
            getArchiveFile().get().asFile,
            project.getProjectDir()
            )
  }

  private List<File> getClassDirectories() {
    allProjects.collect { p ->
      JavaPluginExtension javaPluginExtension = p.getExtensions().getByType(JavaPluginExtension.class)
      javaPluginExtension.getSourceSets().collect { sourceSet ->
        sourceSet.output.asList()
      }
    }.flatten()
  }

  private void collectGatlingDepsRec(Set<ResolvedDependency> deps, Set<ResolvedDependency> alreadyVisited, Set<ResolvedDependency> gatlingDeps) {
    for (dep in deps) {
      if (!alreadyVisited.contains(dep)) {
        alreadyVisited.add(dep)
        if (dep?.module?.id?.group in ["io.gatling", "io.gatling.highcharts"]) {
          collectDepAndChildren(dep, gatlingDeps)
        } else if (dep?.module?.id?.group == "io.netty" && EXCLUDED_NETTY_ARTIFACTS.contains(dep?.module?.id?.name)) {
          gatlingDeps.add(dep)
        } else {
          collectGatlingDepsRec(dep.children, alreadyVisited, gatlingDeps)
        }
      }
    }
  }

  private void collectDepAndChildren(ResolvedDependency dep, Set<ResolvedDependency> gatlingDeps) {
    if (!gatlingDeps.contains(dep)) {
      gatlingDeps.add(dep)
      for (child in dep.children) {
        collectDepAndChildren(child, gatlingDeps)
      }
    }
  }

  private Set<Dependency> collectAllDependencies(Set<ResolvedDependency> firstLevelDependencies) {
    Set<ResolvedDependency> deps = new HashSet<>()
    for (dependency in firstLevelDependencies) {
      collectDepAndChildren(dependency, deps)
    }
    return toDependencies(deps)
  }

  private Set<Dependency> collectGatlingDependencies(Set<ResolvedDependency> firstLevelDependencies) {
    Set<ResolvedDependency> deps = new HashSet<>()
    collectGatlingDepsRec(firstLevelDependencies, new HashSet<>(), deps)
    toDependencies(deps)
  }

  private Set<Dependency> collectExtraDependencies(Set<ResolvedDependency> firstLevelDependencies) {
    return collectAllDependencies(firstLevelDependencies) - collectGatlingDependencies(firstLevelDependencies)
  }
}
