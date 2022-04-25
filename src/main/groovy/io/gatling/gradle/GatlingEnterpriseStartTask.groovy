package io.gatling.gradle

import io.gatling.plugin.EnterprisePlugin
import io.gatling.plugin.model.SimulationStartResult
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.TaskAction

@CacheableTask
class GatlingEnterpriseStartTask extends DefaultTask {

    @TaskAction
    void publish() {
        final GatlingPluginExtension gatling = project.extensions.getByType(GatlingPluginExtension)
        final Map<String, String> systemProperties = gatling.enterprise.systemProps ?: [:]
        final String version = project.version.toString()
        final UUID simulationId = gatling.enterprise.simulationId
        final String simulationClass = gatling.enterprise.simulationClass
        final File file = inputs.files.singleFile
        final UUID teamId = gatling.enterprise.teamId
        final String groupId = project.group.toString()
        final String artifactId = project.name
        final UUID packageId = gatling.enterprise.packageId

        final EnterprisePlugin enterprisePlugin =
            gatling.enterprise.batchMode ?
                gatling.enterprise.initBatchEnterprisePlugin(version, logger) :
                gatling.enterprise.initInteractiveEnterprisePlugin(version, logger)

        final SimulationStartResult simulationStartResult = RecoverEnterprisePluginException.handle(logger) {
            gatling.enterprise.simulationId ?
                    enterprisePlugin.uploadPackageAndStartSimulation(simulationId, systemProperties, simulationClass, file) :
                    enterprisePlugin.createAndStartSimulation(teamId, groupId, artifactId, simulationClass, packageId, systemProperties, file)
        }

        if (simulationStartResult.createdSimulation) {
            CommonLogMessage.logSimulationCreated(simulationStartResult.simulation, logger)
        }

        if (simulationId == null) {
            CommonLogMessage.logSimulationConfiguration(simulationStartResult.simulation, logger)
        }

        logger.lifecycle("""
                         |Simulation ${simulationStartResult.simulation.name} successfully started.
                         |Once running, reports will be available at: ${gatling.enterprise.url.toExternalForm() + simulationStartResult.runSummary.reportsPath}
                         |""".stripMargin())
    }
}
