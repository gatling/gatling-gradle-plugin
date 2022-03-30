package io.gatling.gradle

import io.gatling.plugin.exceptions.SeveralSimulationClassNamesFoundException
import io.gatling.plugin.exceptions.SeveralTeamsFoundException
import io.gatling.plugin.exceptions.SimulationStartException
import io.gatling.plugin.exceptions.UserQuitException
import io.gatling.plugin.model.Simulation
import org.gradle.api.BuildCancelledException
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException

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

        try {
            def enterprisePlugin =
                gatling.enterprise.batchMode ?
                    gatling.enterprise.initBatchEnterprisePlugin(version, logger) :
                    gatling.enterprise.initInteractiveEnterprisePlugin(version, logger)

            def simulationStartResult = gatling.enterprise.simulationId ?
                enterprisePlugin.uploadPackageAndStartSimulation(simulationId, systemProperties, simulationClass, file) :
                enterprisePlugin.createAndStartSimulation(teamId, groupId, artifactId, simulationClass, packageId, systemProperties, file)

            if (simulationStartResult.createdSimulation) {
                logCreatedSimulation(simulationStartResult.simulation)
            }

            if (simulationId == null) {
                logSimulationConfiguration(simulationStartResult.simulation)
            }

            logger.lifecycle("""
                         |Simulation ${simulationStartResult.simulation.name} successfully started.
                         |Once running, reports will be available at: ${gatling.enterprise.url.toExternalForm() + simulationStartResult.runSummary.reportsPath}
                         |""".stripMargin())
        } catch (UserQuitException e) {
            throw new BuildCancelledException(e.getMessage(), e)
        } catch (SeveralTeamsFoundException e) {
            final String teams = e.getAvailableTeams().collect { String.format("- %s (%s)\n", it.id, it.name) }.join()
            final String teamExample = e.getAvailableTeams().head().id.toString()
            throwTaskExecutionException("""
                |More than 1 team were found while creating a simulation.
                |Available teams:
                |${teams}
                |Specify the team you want to use by adding this configuration to your build.gradle, e.g.:
                |gatling.enterprise.teamId "${teamExample}"
                |Or specify -Dgatling.enterprise.teamId= on the command
                |
                |See https://gatling.io/docs/gatling/reference/current/extensions/gradle_plugin/#working-with-gatling-enterprise-cloud for more information.
                """.stripMargin())
        } catch (SeveralSimulationClassNamesFoundException e) {
            throwTaskExecutionException("""Several simulation classes were found
             |${e.availableSimulationClassNames.map { name -> "- " + name }.mkString("\n")}
             |Specify the simulation you want to use by adding this configuration to your build.gradle, e.g.:
             |gatling.enterprise.teamId ${e.availableSimulationClassNames.head()}
             |Or specify -Dgatling.enterprise.simulationClass=<className> on the command
             |""".stripMargin())
        } catch (SimulationStartException e) {
            if (e.created) {
                logCreatedSimulation(e.simulation)
            }
            logSimulationConfiguration(e.simulation)
            throw e.getCause()
        }
    }

    private void throwTaskExecutionException(String message) {
        throw new TaskExecutionException(this, new IllegalArgumentException(message))
    }

    private void logCreatedSimulation(Simulation simulation) {
        logger.lifecycle("Created simulation named ${simulation.name} with ID '${simulation.id}'")
    }

    private void logSimulationConfiguration(Simulation simulation) {
        logger.lifecycle("""
           |To start directly the same simulation, add the following Gradle configuration:
           |gatling.enterprise.simulationId "${simulation.id}"
           |gatling.enterprispackageId "${simulation.pkgId}"
           |Or specify -Dgatling.enterprissimulationId=${simulation.id} -Dgatling.enterprispackageId=${simulation.pkgId}
           |
           |See https://gatling.io/docs/gatling/reference/current/extensions/gradle_plugin/#working-with-gatling-enterprise-cloud for more information.
           |""".stripMargin())
    }
}
