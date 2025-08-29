package io.gatling.gradle

import io.gatling.plugin.io.JavaPluginScanner
import io.gatling.plugin.io.PluginIO
import io.gatling.plugin.io.PluginLogger
import io.gatling.plugin.io.PluginScanner
import org.gradle.api.logging.Logger

final class GradlePluginIO implements PluginIO {
    Logger gradleLogger
    Scanner scannerIn = new Scanner(System.in)

    GradlePluginIO(Logger logger) {
        this.gradleLogger = logger
    }

    @Override
    PluginLogger getLogger() {
        return new PluginLogger() {
            @Override
            void debug(String message) {
                gradleLogger.debug(message)
            }

            @Override
            void info(String message) {
                gradleLogger.lifecycle(message)
            }

            @Override
            void error(String message) {
                gradleLogger.error(message)
            }
        }
    }

    @Override
    PluginScanner getScanner() {
        return new JavaPluginScanner(scannerIn)
    }
}
