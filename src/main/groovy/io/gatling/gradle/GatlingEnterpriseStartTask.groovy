package io.gatling.gradle

import io.gatling.plugin.util.EnterpriseClient
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException

@CacheableTask
class GatlingEnterpriseStartTask extends DefaultTask {

    @Internal
    Closure simulations

    @TaskAction
    void publish() {
        def gatling = project.extensions.getByType(GatlingPluginExtension)
        EnterpriseClient enterpriseClient = gatling.enterprise.getEnterpriseClient(project.version.toString())

        def systemProps = gatling.enterprise.systemProps ?: [:]

        if (!gatling.enterprise.simulationId && !gatling.enterprise.simulationClass) {
            throw new TaskExecutionException(this,
                new IllegalArgumentException("You need to either configure gatling.enterprise.simulationId if you want to start an existing simulation," +
                "or gatling.enterprise.simulationClass if you want to create a new simulation")
            )
        }

        def simulationAndRunSummary
        if (gatling.enterprise.simulationId) {
            simulationAndRunSummary = enterpriseClient.startSimulation(gatling.enterprise.simulationId, systemProps, inputs.files.singleFile)
        } else {
            simulationAndRunSummary = enterpriseClient.createAndStartSimulation(
                gatling.enterprise.teamId,
                project.group.toString(),
                project.name,
                gatling.enterprise.simulationClass,
                systemProps, inputs.files.singleFile)
            def simulation = simulationAndRunSummary.simulation
            getLogger().info("""
                         |Created simulation ${simulation.name} with ID ${simulation.id}
                         |
                         |To start again the same simulation, add the following Gradle configuration:
                         |gatling.enterprise.simulationId "${simulation.id}"
                         |gatling.enterprise.packageId "${simulation.pkgId}"
                         |""".stripMargin())
        }
        getLogger().info("""
                         |Simulation ${simulationAndRunSummary.simulation.name} successfully started
                         |Once running, reports will be available at: ${gatling.enterprise.url.toExternalForm() + simulationAndRunSummary.runSummary.reportsPath}
                         |""".stripMargin())
    }
}
