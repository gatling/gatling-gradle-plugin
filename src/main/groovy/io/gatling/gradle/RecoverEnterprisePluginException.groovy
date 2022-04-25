package io.gatling.gradle


import io.gatling.plugin.exceptions.*
import org.gradle.api.BuildCancelledException
import org.gradle.api.Task
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.TaskExecutionException

class RecoverEnterprisePluginException {

    /**
     * @param f Closure defined inside a Task
     * @param logger used to report EnterprisePluginException
     * @return closure result
     */
    static <R> R handle(Logger logger, Closure<R> f) {
        try {
            return f.doCall()
        }  catch (UserQuitException e) {
            throw new BuildCancelledException(e.getMessage(), e)
        } catch (SeveralTeamsFoundException e) {
            final String teams = e.getAvailableTeams().collect { String.format("- %s (%s)\n", it.id, it.name) }.join()
            final String teamExample = e.getAvailableTeams().head().id.toString()
            throwTaskExecutionException(f.getThisObject(), """
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
            throwTaskExecutionException(f.getThisObject(), """Several simulation classes were found
             |${e.availableSimulationClassNames.map { name -> "- " + name }.mkString("\n")}
             |Specify the simulation you want to use by adding this configuration to your build.gradle, e.g.:
             |gatling.enterprise.teamId ${e.availableSimulationClassNames.head()}
             |Or specify -Dgatling.enterprise.simulationClass=<className> on the command
             |""".stripMargin())
        } catch (SimulationStartException e) {
            if (e.created) {
                CommonLogMessage.logSimulationCreated(e.simulation, logger)
            }
            CommonLogMessage.logSimulationConfiguration(e.simulation, logger)
            throw e.getCause()
        }
    }

    private static void throwTaskExecutionException(Object subject, String message) throws TaskExecutionException {
        throw new TaskExecutionException(subject as Task, new IllegalArgumentException(message))
    }

}
