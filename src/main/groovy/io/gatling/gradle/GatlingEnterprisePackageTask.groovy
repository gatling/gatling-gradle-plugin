package io.gatling.gradle

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.artifacts.ResolvedConfiguration
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.TaskAction

@CacheableTask
class GatlingEnterprisePackageTask extends ShadowJar {

    private ResolvedConfiguration getResolvedConfiguration() {
        project.configurations.gatlingRuntimeClasspath.resolvedConfiguration
    }

    private String gatlingVersion() {
        for (artifact in getResolvedConfiguration().resolvedArtifacts.flatten()) {
            def id = artifact.moduleVersion.id
            if (id.group == "io.gatling" && id.name == "gatling-app") {
                getLogger().debug("Detected Gatling compile version: {}", id.version)
                return id.version
            }
        }
        throw new IllegalArgumentException("Couldn't locate io.gatling:gatling-app in dependencies")
    }

    private void collectDepAndChildren(ResolvedDependency dep, Set<ResolvedDependency> acc) {
        acc.add(dep)
        for (child in dep.children) {
            collectDepAndChildren(child, acc)
        }
    }

    private void collectGatlingDepsRec(Set<ResolvedDependency> deps, Set<ResolvedDependency> acc) {
        for (dep in deps) {
            if (dep?.module?.id?.group in ["io.gatling", "io.gatling.highcharts", "io.gatling.frontline"]) {
                collectDepAndChildren(dep, acc)
            } else if (dep?.module?.id?.group == "io.netty" && dep?.module?.id?.name == "netty-all") {
                acc.add(dep)
            } else {
                collectGatlingDepsRec(dep.children, acc)
            }
        }
    }

    @Override
    @Classpath
    FileCollection getIncludedDependencies() {
        ResolvedConfiguration resolvedConfiguration = getResolvedConfiguration()

        Set<ResolvedDependency> gatlingDependencies = new HashSet<ResolvedDependency>()
        collectGatlingDepsRec(resolvedConfiguration.getFirstLevelModuleDependencies(), gatlingDependencies)

        project.files(resolvedConfiguration.files) - project.files(gatlingDependencies.collect {
            it.moduleArtifacts*.file
        }.flatten())
    }

    @TaskAction
    protected void copy() {
        String gatlingVersion = gatlingVersion()
        manifest {
            attributes("Manifest-Version": "1.0",
                "Implementation-Title": project.name,
                "Implementation-Version": project.version,
                "Implementation-Vendor": project.group,
                "Specification-Vendor": "GatlingCorp",
                "Gatling-Version": gatlingVersion)
        }
        super.copy()
    }
}
