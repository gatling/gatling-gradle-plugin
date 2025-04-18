package io.gatling.gradle

import io.gatling.plugin.pkg.Dependency
import io.gatling.plugin.pkg.EnterprisePackager
import java.util.stream.Collectors
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.*
import org.gradle.jvm.tasks.Jar

@CacheableTask
class GatlingEnterprisePackageTask extends Jar {

    private static final Set<String> EXCLUDED_NETTY_ARTIFACTS = ["netty-all", "netty-resolver-dns-classes-macos", "netty-resolver-dns-native-macos"].asUnmodifiable()

    private static Set<Dependency> toDependencies(Set<ResolvedDependency> deps) {
        deps.collectMany { resolvedDependency ->
            resolvedDependency.getModuleArtifacts().collect(moduleArtifact ->
                new Dependency(
                    resolvedDependency.module.id.group,
                    resolvedDependency.module.id.name,
                    resolvedDependency.module.id.version,
                    moduleArtifact.file
                ))
        }.toSet()
    }

    @Classpath
    @Optional
    List<Configuration> configurations

    @TaskAction
    @Override
    protected void copy() {
        EnterprisePackager packager = new EnterprisePackager(new GradlePluginIO(logger).getLogger())
        Set<ResolvedDependency> firstLevelModuleDependencies = project.configurations.gatlingRuntimeClasspath.resolvedConfiguration.getFirstLevelModuleDependencies()
        packager.createEnterprisePackage(
            getClassDirectories(),
            collectGatlingDependencies(firstLevelModuleDependencies),
            collectExtraDependencies(firstLevelModuleDependencies),
            project.group,
            project.name,
            project.version,
            'gradle',
            getClass().getPackage().getImplementationVersion(),
            getArchiveFile().get().asFile
        )
    }

    private List<File> getClassDirectories() {
        project.getAllprojects().collect { p ->
            JavaPluginExtension javaPluginExtension = p.getExtensions().getByType(JavaPluginExtension.class)
            javaPluginExtension.getSourceSets().collect {
                sourceSet -> sourceSet.output.asList()
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
            .stream()
            // exclude protobuf from Gatling provided deps as only the user knows if he wants to use protobuf 3 or 4
            .filter { it.groupId != "com.google.protobuf" || it.artifactId != "protobuf-java" }
            .collect(Collectors.toSet())
    }

    private Set<Dependency> collectExtraDependencies(Set<ResolvedDependency> firstLevelDependencies) {
        return collectAllDependencies(firstLevelDependencies) - collectGatlingDependencies(firstLevelDependencies)
    }
}
