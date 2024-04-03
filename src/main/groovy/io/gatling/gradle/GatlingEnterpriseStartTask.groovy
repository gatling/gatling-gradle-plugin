package io.gatling.gradle

import io.gatling.plugin.EnterprisePlugin
import io.gatling.plugin.model.RunSummary
import io.gatling.plugin.model.SimulationEndResult
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

class GatlingEnterpriseStartTask extends GatlingEnterpriseDeployTask {

    @TaskAction
    void publish() {
        super.deploy()

        final GatlingPluginExtension gatling = project.extensions.getByType(GatlingPluginExtension)
        final String version = project.version.toString()
        final boolean waitForRunEnd = gatling.enterprise.waitForRunEnd

        final EnterprisePlugin enterprisePlugin =
            gatling.enterprise.batchMode ?
                gatling.enterprise.initBatchEnterprisePlugin(version, logger) :
                gatling.enterprise.initInteractiveEnterprisePlugin(version, logger)

        RecoverEnterprisePluginException.handle(logger) {
            RunSummary runSummary = enterprisePlugin.startSimulation(gatling.enterprise.simulationName, deploymentInfo)
            logger.lifecycle("""
                         |Simulation successfully started.
                         |Reports are available at: ${gatling.enterprise.url.toExternalForm() + runSummary.reportsPath}
                         |""".stripMargin())
            if (waitForRunEnd) {
                SimulationEndResult finishedRun = enterprisePlugin.waitForRunEnd(runSummary)
                if (!finishedRun.status.successful) {
                    throw new GradleException("Simulation failed.")
                }
            }
        }
    }
}
