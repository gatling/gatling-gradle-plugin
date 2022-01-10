package io.gatling.gradle

import io.gatling.plugin.EnterprisePlugin
import io.gatling.plugin.InteractiveEnterprisePlugin
import io.gatling.plugin.exceptions.SeveralTeamsFoundException
import io.gatling.plugin.exceptions.SimulationStartException
import io.gatling.plugin.model.Simulation
import io.gatling.plugin.model.SimulationStartResult
import org.gradle.api.DefaultTask
import org.gradle.api.logging.LogLevel
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

        def systemProps = gatling.enterprise.systemProps ?: [:]

        try {
            def simulationStartResult = gatling.enterprise.simulationId
                ? startNonInteractive(gatling, systemProps)
                : gatling.enterprise.batchMode
                ? createNonInteractive(gatling, systemProps)
                : createOrStartInteractive(gatling, systemProps)

            logger.lifecycle("""
                         |Simulation ${simulationStartResult.simulation.name} successfully started.
                         |Once running, reports will be available at: ${gatling.enterprise.url.toExternalForm() + simulationStartResult.runSummary.reportsPath}
                         |""".stripMargin())
        } catch (SimulationStartException e) {
            logger.lifecycle(getLogCreatedSimulation(e.simulation, true))
            throw e
        }
    }

    private SimulationStartResult startNonInteractive(GatlingPluginExtension gatling, Map<String, String> systemProps) {
        EnterprisePlugin enterprisePlugin = gatling.enterprise.initEnterprisePlugin(project.version.toString(), logger)
        return enterprisePlugin.uploadPackageAndStartSimulation(gatling.enterprise.simulationId, systemProps, inputs.files.singleFile)
    }

    private SimulationStartResult createNonInteractive(GatlingPluginExtension gatling, Map<String, String> systemProps) {
        List<String> classes = SimulationFilesUtils.resolveSimulations(project, null).toList()
        def chosenSimulation = gatling.enterprise.simulationClass ?: classes.size() == 1 ? classes.get(0) : null
        if (!gatling.enterprise.simulationId && !chosenSimulation) {
            throw new TaskExecutionException(this,
                new IllegalArgumentException("""
                    |Specify gatling.enterprise.simulationId in your build.gradle if you want to start a simulation,
                    |or gatling.enterprise.simulationClass if you want to create a new simulation.
                    |You can also use -Dgatling.enterprise.simulationId= and -Dgatling.enterprise.simulationClass=
                    |See https://gatling.io/docs/gatling/reference/current/extensions/gradle_plugin/#working-with-gatling-enterprise-cloud for more information.
                    """.stripMargin()
            ))
        }
        EnterprisePlugin enterprisePlugin = gatling.enterprise.initEnterprisePlugin(project.version.toString(), logger)
        logger.lifecycle("No simulationId configured, creating a new simulation in batch mode")
        try {
            def simulationStartResult = enterprisePlugin.createAndStartSimulation(gatling.enterprise.teamId, project.group.toString(), project.name,
                chosenSimulation, gatling.enterprise.packageId, systemProps, inputs.files.singleFile)
            logger.lifecycle(getLogCreatedSimulation(simulationStartResult.simulation, true))
            return simulationStartResult
        } catch (SeveralTeamsFoundException e) {
            final String teams = e.getAvailableTeams().collect {String.format("- %s (%s)\n", it.id, it.name)}.join()
            final String teamExample = e.getAvailableTeams().get(0).id.toString()
            final String msg ="""
                |More than 1 team were found while creating a simulation.
                |Available teams:
                |${teams}
                |Specify the team you want to use by adding this configuration to your build.gradle, e.g.:
                |gatling.enterprise.teamId "${teamExample}"
                |Or specify -Dgatling.enterprise.teamId= on the command
                |
                |See https://gatling.io/docs/gatling/reference/current/extensions/gradle_plugin/#working-with-gatling-enterprise-cloud for more information.
                """.stripMargin()
            throw new TaskExecutionException(this, new IllegalArgumentException(msg))
        }
    }

    private SimulationStartResult createOrStartInteractive(GatlingPluginExtension gatling, Map<String, String> systemProps) {
        if (!logger.isEnabled(LogLevel.LIFECYCLE)) {
            logger.error("Please activate the LIFECYCLE log level if you want to interact with the gatlingEnterpriseStart task.")
        }

        InteractiveEnterprisePlugin enterprisePlugin = gatling.enterprise.initInteractiveEnterprisePlugin(project.version.toString(), logger)
        Iterable<String> classes = SimulationFilesUtils.resolveSimulations(project, null)
        def simulationStartResult = enterprisePlugin.createOrStartSimulation(
            gatling.enterprise.teamId, project.group.toString(), project.name, gatling.enterprise.simulationClass,
            classes.toList(), gatling.enterprise.packageId, systemProps, inputs.files.singleFile)
        def simulation = simulationStartResult.simulation
        logger.lifecycle(getLogCreatedSimulation(simulation, simulationStartResult.createdSimulation))
        return simulationStartResult
    }

    private static String getLogCreatedSimulation(Simulation simulation, boolean create) {
        def verb = create ? "Created" : "Started"
        return """
               |$verb simulation ${simulation.name} with ID ${simulation.id}
               |
               |To start directly the same simulation, add the following Gradle configuration:
               |gatling.enterprise.simulationId "${simulation.id}"
               |gatling.enterprise.packageId "${simulation.pkgId}"
               |Or specify -Dgatling.enterprise.simulationId=${simulation.id} -Dgatling.enterprise.packageId "${simulation.pkgId}"
               |
               |See https://gatling.io/docs/gatling/reference/current/extensions/gradle_plugin/#working-with-gatling-enterprise-cloud for more information.
               |""".stripMargin()
    }

}
