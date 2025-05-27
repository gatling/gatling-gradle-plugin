package io.gatling.gradle

import io.gatling.plugin.BatchEnterprisePlugin
import io.gatling.plugin.deployment.DeploymentConfiguration
import io.gatling.plugin.model.DeploymentInfo
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class GatlingEnterpriseDeployTask extends DefaultTask {

    protected DeploymentInfo deploymentInfo
    protected final GatlingPluginExtension gatlingExt = project.extensions.getByType(GatlingPluginExtension)
    protected final String artifactId = project.name
    protected final File rootDir = project.rootDir

    @TaskAction
    void deploy() {
        final BatchEnterprisePlugin enterprisePlugin = gatlingExt.enterprise.initBatchEnterprisePlugin(logger)
        final File descriptorFile = DeploymentConfiguration.fromBaseDirectory(rootDir, gatlingExt.enterprise.packageDescriptorFilename)
        final File packageFile = inputs.files.singleFile
        final Boolean isPrivateRepositoryEnabled = gatlingExt.enterprise.controlPlaneUrl != null
        final String validateSimulationId = gatlingExt.enterprise.validateSimulationId;

        deploymentInfo = validateSimulationId != null
            ? enterprisePlugin.deployFromDescriptor(descriptorFile, packageFile, artifactId, isPrivateRepositoryEnabled, validateSimulationId)
            : enterprisePlugin.deployFromDescriptor(descriptorFile, packageFile, artifactId, isPrivateRepositoryEnabled)
    }
}
