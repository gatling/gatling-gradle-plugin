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
package func;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;

import helper.GatlingFuncTestBase;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class WhenGroovyCompileScalaSimulationTest extends GatlingFuncTestBase {

  private static final String GATLING_CLASSES_TASK_NAME = "gatlingClasses";

  @BeforeEach
  void setUp() {
    prepareGroovyTestWithScala("/gradle-layout");
  }

  @Test
  void shouldCompile() {
    BuildResult result = executeGradle(GATLING_CLASSES_TASK_NAME);

    assertThat(result.task(":" + GATLING_CLASSES_TASK_NAME).getOutcome()).isEqualTo(SUCCESS);
    assertThat(new File(buildDir, "classes/scala/gatling")).isDirectory();
    File resourcesDir = new File(buildDir, "resources/gatling");
    assertThat(resourcesDir).isDirectory();
    assertThat(new File(resourcesDir, "search.csv")).exists();
    assertThat(new File(buildDir, "classes/java/main")).isDirectory();
    assertThat(new File(buildDir, "classes/java/test")).isDirectory();
  }

  @ParameterizedTest(name = "without {0}")
  @CsvSource({
    "src/main, TestUtils.java:5: error: cannot find symbol",
    "src/test, not found: value TestUtils"
  })
  void shouldNotCompileWithout(String dir, String message) throws IOException {
    File sourceDir = new File(projectDir, dir);
    assertThat(sourceDir).isDirectory();
    FileUtils.deleteDirectory(sourceDir);

    BuildResult result = executeGradleAndFail(GATLING_CLASSES_TASK_NAME);

    assertThat(result.getOutput()).contains(message);
  }

  @Test
  void shouldFailToCompileWhenRequiredDependenciesAreMissing() {
    writeFile(
        buildFile,
        """
        plugins {
          id 'scala'
          id 'io.gatling.gradle'
        }

        repositories {
          mavenCentral()
        }
        """);

    BuildResult result = executeGradleAndFail(GATLING_CLASSES_TASK_NAME);

    assertThat(result.getOutput())
        .contains("object lang is not a member of package org.apache.commons");
  }

  @Test
  void shouldNotCompileWithMainAndTestOutputExcluded() {
    appendToBuildFile(
        """
        gatling {
          includeMainOutput = false
          includeTestOutput = false
        }
        """);

    BuildResult result = executeGradleAndFail(GATLING_CLASSES_TASK_NAME);

    assertThat(result.getOutput()).contains("object MainUtils is not a member of package example");
  }

  @Test
  void shouldNotCompileWithTestOutputExcluded() {
    appendToBuildFile("gatling { includeTestOutput = false }");

    BuildResult result = executeGradleAndFail(GATLING_CLASSES_TASK_NAME);

    assertThat(result.getOutput()).contains("not found: value TestUtils");
  }
}
