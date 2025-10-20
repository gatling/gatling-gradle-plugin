/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
