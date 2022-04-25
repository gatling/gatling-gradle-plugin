package io.gatling.gradle

import io.gatling.plugin.model.Simulation
import org.gradle.api.logging.Logger


class CommonLogMessage {

    static void logSimulationCreated(Simulation simulation, Logger logger) {
        logger.lifecycle("Created simulation named ${simulation.name} with ID '${simulation.id}'")
    }

    static void logSimulationConfiguration(Simulation simulation, Logger logger) {
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
