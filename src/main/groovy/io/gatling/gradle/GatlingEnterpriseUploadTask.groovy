package io.gatling.gradle

import io.gatling.plugin.EnterprisePlugin
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
            EnterprisePlugin enterprisePlugin = gatling.enterprise.initBatchEnterprisePlugin(project.version.toString(), logger)
            if (gatling.enterprise.packageId) {
                logger.lifecycle("Uploading package with packageId " + gatling.enterprise.packageId)
                enterpriseClient.uploadPackage(gatling.enterprise.packageId, inputs.files.singleFile)
            } else if (gatling.enterprise.simulationId) {
                logger.lifecycle("Uploading package belonging to the simulation " + gatling.enterprise.simulationId)
                enterpriseClient.uploadPackageWithSimulationId(gatling.enterprise.simulationId, inputs.files.singleFile)
            } else {
                throw new InvalidUserDataException("You need to either configure gatling.enterprise.packageId (or pass it with '-Dgatling.enterprise.packageId=<PACKAGE_ID>') " +
                    "or gatling.enterprise.simulation (or pass it with '-Dgatling.enterprise.simulationId=<SIMULATION_ID>') to upload a package." +
                    "Please see https://gatling.io/docs/gatling/reference/current/extensions/gradle_plugin/#working-with-gatling-enterprise-cloud for more information.")
            }
        }
    }
}
