package io.gatling.gradle

import io.gatling.plugin.BatchEnterprisePlugin
import io.gatling.plugin.deployment.DeploymentConfiguration
import io.gatling.plugin.model.DeploymentInfo
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class GatlingEnterpriseDeployTask extends DefaultTask {

    protected DeploymentInfo deploymentInfo

    @TaskAction
    void deploy() {
        final GatlingPluginExtension gatlingPlugin = project.extensions.getByType(GatlingPluginExtension)
        final BatchEnterprisePlugin enterprisePlugin = gatlingPlugin.enterprise.initBatchEnterprisePlugin(logger)
        final File descriptorFile = DeploymentConfiguration.fromBaseDirectory(project.rootDir, gatlingPlugin.enterprise.packageDescriptorFilename)
        final File packageFile = inputs.files.singleFile
        final Boolean isPrivateRepositoryEnabled = gatlingPlugin.enterprise.controlPlaneUrl != null
        final String validateSimulationId = gatlingPlugin.enterprise.validateSimulationId;
        final String artifactId = project.name

        deploymentInfo = validateSimulationId != null
            ? enterprisePlugin.deployFromDescriptor(descriptorFile, packageFile, artifactId, isPrivateRepositoryEnabled, validateSimulationId)
            : enterprisePlugin.deployFromDescriptor(descriptorFile, packageFile, artifactId, isPrivateRepositoryEnabled)
    }
}
