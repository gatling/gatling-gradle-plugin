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
package io.gatling.gradle;

import io.gatling.plugin.io.JavaPluginScanner;
import io.gatling.plugin.io.PluginIO;
import io.gatling.plugin.io.PluginLogger;
import io.gatling.plugin.io.PluginScanner;
import java.util.Scanner;
import org.gradle.api.logging.Logger;

public final class GradlePluginIO implements PluginIO {
  private final Logger gradleLogger;
  private final Scanner scannerIn = new Scanner(System.in);

  public GradlePluginIO(Logger logger) {
    this.gradleLogger = logger;
  }

  @Override
  public PluginLogger getLogger() {
    return new PluginLogger() {
      @Override
      public void debug(String message) {
        gradleLogger.debug(message);
      }

      @Override
      public void info(String message) {
        gradleLogger.lifecycle(message);
      }

      @Override
      public void error(String message) {
        gradleLogger.error(message);
      }
    };
  }

  @Override
  public PluginScanner getScanner() {
    return new JavaPluginScanner(scannerIn);
  }
}
