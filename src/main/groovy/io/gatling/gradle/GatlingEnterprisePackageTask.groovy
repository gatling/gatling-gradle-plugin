package io.gatling.gradle

import io.gatling.plugin.pkg.Dependency
import io.gatling.plugin.pkg.EnterprisePackager
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedConfiguration
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
        ResolvedConfiguration resolvedConfiguration = getResolvedConfiguration()
        packager.createEnterprisePackage(
            getClassDirectories(),
            collectGatlingDependencies(resolvedConfiguration.getFirstLevelModuleDependencies()),
            collectExtraDependencies(resolvedConfiguration.getFirstLevelModuleDependencies()),
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

    private ResolvedConfiguration getResolvedConfiguration() {
        project.configurations.gatlingRuntimeClasspath.resolvedConfiguration
    }

    private void collectGatlingDepsRec(Set<ResolvedDependency> deps, Set<ResolvedDependency> acc) {
        for (dep in deps) {
            if (dep?.module?.id?.group in ["io.gatling", "io.gatling.highcharts", "io.gatling.frontline"]) {
                collectDepAndChildren(dep, acc)
            } else if (dep?.module?.id?.group == "io.netty" && EXCLUDED_NETTY_ARTIFACTS.contains(dep?.module?.id?.name)) {
                acc.add(dep)
            } else {
                collectGatlingDepsRec(dep.children, acc)
            }
        }
    }

    private void collectDepAndChildren(ResolvedDependency dep, Set<ResolvedDependency> acc) {
        if (!acc.contains(dep)) {
            acc.add(dep)
            for (child in dep.children) {
                collectDepAndChildren(child, acc)
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
        collectGatlingDepsRec(firstLevelDependencies, deps)
        return toDependencies(deps)
    }

    private Set<Dependency> collectExtraDependencies(Set<ResolvedDependency> firstLevelDependencies) {
        return collectAllDependencies(firstLevelDependencies) - collectGatlingDependencies(firstLevelDependencies)
    }
}
