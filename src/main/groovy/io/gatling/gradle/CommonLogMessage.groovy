package io.gatling.gradle

import io.gatling.plugin.model.Simulation
import org.gradle.api.logging.Logger


final class CommonLogMessage {

    static void logSimulationCreated(Simulation simulation, Logger logger) {
        logger.lifecycle("Created simulation named ${simulation.name} with ID '${simulation.id}'")
    }

    static void logSimulationConfiguration(Logger logger, Simulation simulation, UUID simulationIdSetting, boolean waitForRunEndSetting) {
        if (simulationIdSetting == null || !waitForRunEndSetting) {
            StringBuilder builder = new StringBuilder("\n")
            if (simulationIdSetting == null) {
                builder.append(
                    """To start directly the same simulation, add the following Gradle configuration:
                      |gatling.enterprise.simulationId "${simulation.id}"
                      |gatling.enterprispackageId "${simulation.pkgId}"
                      |Or specify -Dgatling.enterprissimulationId=${simulation.id} -Dgatling.enterprispackageId=${simulation.pkgId}
                      |
                      |""".stripMargin()
                )
            }
            if (!waitForRunEndSetting) {
                builder.append(
                    """To wait for the end of the run when starting a simulation on Gatling Enterprise, add the following configuration:
                      |gatling.enterprise.waitForRunEnd true
                      |Or specify -Dgatling.enterprise.waitForRunEnd=true
                      |
                      |""".stripMargin())
            }
            builder.append("See https://gatling.io/docs/gatling/reference/current/extensions/gradle_plugin/#working-with-gatling-enterprise-cloud for more information.\n")
            logger.lifecycle(builder.toString())
        }
    }
}
