package io.gatling.gradle

import io.gatling.plugin.BatchEnterprisePlugin
import io.gatling.plugin.ConfigurationConstants
import org.gradle.api.DefaultTask
import org.gradle.api.InvalidUserDataException
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.TaskAction

@CacheableTask
class GatlingEnterpriseUploadTask extends DefaultTask {

    @TaskAction
    void publish() {
        def gatling = project.extensions.getByType(GatlingPluginExtension)
        RecoverEnterprisePluginException.handle(logger) {
            BatchEnterprisePlugin enterprisePlugin = gatling.enterprise.initBatchEnterprisePlugin(logger)
            UUID packageUUID = gatling.enterprise.packageId

            if (packageUUID) {
                logger.lifecycle("Uploading package with packageId " + packageUUID)
                enterprisePlugin.uploadPackage(packageUUID, inputs.files.singleFile)
            } else if (gatling.enterprise.simulationId) {
                logger.lifecycle("Uploading package belonging to the simulation " + gatling.enterprise.simulationId)
                enterprisePlugin.uploadPackageWithSimulationId(gatling.enterprise.simulationId, inputs.files.singleFile)
            } else {
                throw new InvalidUserDataException("You need to either configure gatling.enterprise.packageId (or pass it with '-D${ConfigurationConstants.UploadOptions.PackageId.SYS_PROP}=<PACKAGE_ID>') " +
                    "or gatling.enterprise.simulationId (or pass it with '-D${ConfigurationConstants.UploadOptions.SimulationId.SYS_PROP}=<SIMULATION_ID>') to upload a package." +
                    "Please see https://docs.gatling.io/reference/integrations/build-tools/gradle-plugin/#running-your-simulations-on-gatling-enterprise-cloud for more information.")
            }
        }
    }
}
