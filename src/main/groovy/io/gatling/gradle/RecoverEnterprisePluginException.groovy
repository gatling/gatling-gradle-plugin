package io.gatling.gradle


import io.gatling.plugin.exceptions.*
import org.gradle.api.BuildCancelledException
import org.gradle.api.Task
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.TaskExecutionException

final class RecoverEnterprisePluginException {

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
        } catch (UnsupportedJavaVersionException e) {
            throwTaskExecutionException(f.getThisObject(), """
                |${e.getMessage()}
                |In order to target the supported Java version, please use the following Gradle setting (requires Gradle 6.6 or later):
                |compileJava {
                |    options.release = ${e.supportedVersion}
                |}
                |See also the Gradle documentation: https://docs.gradle.org/current/userguide/building_java_projects.html#sec:java_cross_compilation
                |Another solution is to configure a Java toolchain to use Java ${e.supportedVersion}; see https://docs.gradle.org/current/userguide/toolchains.html
                |Alternatively, the reported class may come from your project's dependencies, published targeting Java ${e.version}. In this case you need to use dependencies which target Java ${e.supportedVersion} or lower."
                """.stripMargin())
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
