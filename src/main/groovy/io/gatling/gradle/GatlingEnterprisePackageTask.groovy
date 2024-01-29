package io.gatling.gradle

import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedConfiguration
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.file.copy.CopyAction
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Jar

@CacheableTask
class GatlingEnterprisePackageTask extends Jar {

    private static final Set<String> EXCLUDED_NETTY_ARTIFACTS = [ "netty-all", "netty-resolver-dns-classes-macos", "netty-resolver-dns-native-macos" ].asUnmodifiable()

    @Classpath @Optional
    List<Configuration> configurations

    GatlingEnterprisePackageTask() {
        super()
        setDuplicatesStrategy(DuplicatesStrategy.INCLUDE)
    }

    @TaskAction
    void copy() {
        String gatlingVersion = gatlingVersion()
        manifest {
            attributes("Manifest-Version": "1.0",
                "Implementation-Title": project.name,
                "Implementation-Version": project.version,
                "Implementation-Vendor": project.group,
                "Specification-Vendor": "GatlingCorp",
                "Gatling-Version": gatlingVersion,
                "Gatling-Packager": "gradle")
        }
        from(getIncludedDependencies())
        super.copy()
    }

    @Classpath
    FileCollection getIncludedDependencies() {
        ResolvedConfiguration resolvedConfiguration = getResolvedConfiguration()

        Set<ResolvedDependency> gatlingDependencies = new HashSet<ResolvedDependency>()
        collectGatlingDepsRec(resolvedConfiguration.getFirstLevelModuleDependencies(), gatlingDependencies)

        project.files(resolvedConfiguration.files) - project.files(gatlingDependencies.collect {
            it.moduleArtifacts*.file
        }.flatten())
    }

    @Override
    protected CopyAction createCopyAction() {
        return new GatlingEnterpriseCopyAction(
            this.archiveFile.get().asFile,
            getMetadataCharset(),
            getMainSpec().buildRootResolver().getPatternSet(),
            isPreserveFileTimestamps(),
            getEntryCompression(),
            isZip64()
        )
    }

    private String gatlingVersion() {
        for (artifact in getResolvedConfiguration().resolvedArtifacts.flatten()) {
            def id = artifact.moduleVersion.id
            if (id.group == "io.gatling" && id.name == "gatling-app") {
                logger.debug("Detected Gatling compile version: {}", id.version)
                return id.version
            }
        }
        throw new IllegalArgumentException("Couldn't locate io.gatling:gatling-app in dependencies")
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
}
