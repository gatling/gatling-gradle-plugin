package io.gatling.gradle

import io.gatling.plugin.BatchEnterprisePlugin
import io.gatling.plugin.deployment.DeploymentConfiguration
import io.gatling.plugin.model.BuildTool
import io.gatling.plugin.model.DeploymentInfo
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class GatlingEnterpriseDeployTask extends DefaultTask {

    protected DeploymentInfo deploymentInfo

    @TaskAction
    void deploy() {
        final GatlingPluginExtension gatlingPlugin = project.extensions.getByType(GatlingPluginExtension)
        final String version = project.version.toString()
        final BatchEnterprisePlugin enterprisePlugin = gatlingPlugin.enterprise.initBatchEnterprisePlugin(version, logger)

        final File descriptorFile = DeploymentConfiguration.fromBaseDirectory(project.rootDir)
        final File packageFile = inputs.files.singleFile
        final Boolean isPrivateRepositoryEnabled = gatlingPlugin.enterprise.controlPlaneUrl != null
        final String artifactId = project.name
        final String implementationVersion = (getClass().getPackage().getImplementationVersion() == null) ? "undefined" : getClass().getPackage().getImplementationVersion()
        deploymentInfo = enterprisePlugin.deployFromDescriptor(descriptorFile, packageFile, artifactId, isPrivateRepositoryEnabled, BuildTool.GRADLE, implementationVersion)
    }
}
