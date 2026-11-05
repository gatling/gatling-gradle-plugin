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

import static io.gatling.gradle.GatlingPlugin.GATLING_RUN_TASK_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.testkit.runner.TaskOutcome.FAILED;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE;

import helper.GatlingFuncTestBase;
import java.io.File;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class WhenGroovyRunScalaSimulationTest extends GatlingFuncTestBase {

  @ParameterizedTest(name = "Gradle {0}")
  @MethodSource("supportedGradleVersions")
  void shouldExecuteAllSimulationsWhenForcedByAllOption(String gradleVersion) {
    prepareGroovyTestWithScala("/gradle-layout");

    BuildResult result =
        createRunner(GATLING_RUN_TASK_NAME, "--non-interactive", "--all")
            .withGradleVersion(gradleVersion)
            .build();

    assertThat(result.task(":" + GATLING_RUN_TASK_NAME).getOutcome()).isEqualTo(SUCCESS);
    assertThat(result.task(":gatlingClasses").getOutcome()).isEqualTo(SUCCESS);
    assertThat(new File(buildDir, "reports/gatling").listFiles()).hasSize(2);
    assertThat(result.getOutput()).doesNotContain("INFO");
  }

  @Test
  void shouldExecuteOnlyTheSimulationForcedBySimulationOption() {
    prepareGroovyTestWithScala("/gradle-layout");

    BuildResult result =
        executeGradle(
            GATLING_RUN_TASK_NAME, "--non-interactive", "--simulation=example.BasicSimulation");

    assertThat(result.task(":" + GATLING_RUN_TASK_NAME).getOutcome()).isEqualTo(SUCCESS);
    assertThat(new File(buildDir, "reports/gatling").listFiles()).hasSize(1);
    assertThat(result.getOutput()).doesNotContain("INFO");
  }

  @Test
  void shouldFailWhenNoSimulationIsFound() {
    prepareGroovyTestWithScala(null);

    BuildResult result = executeGradleAndFail(GATLING_RUN_TASK_NAME, "--non-interactive", "--all");

    assertThat(result.task(":" + GATLING_RUN_TASK_NAME).getOutcome()).isEqualTo(FAILED);
  }

  @Test
  void shouldRunGatlingTwiceEvenIfNoChangesToSourceCode() {
    prepareGroovyTestWithScala("/gradle-layout");
    appendToBuildFile("gatling { includes = ['example.BasicSimulation'] }\n");

    // 1st time
    BuildResult result = executeGradle(GATLING_RUN_TASK_NAME, "--non-interactive", "--all");
    assertThat(result.task(":compileGatlingScala").getOutcome()).isEqualTo(SUCCESS);
    assertThat(result.task(":" + GATLING_RUN_TASK_NAME).getOutcome()).isEqualTo(SUCCESS);

    // 2nd time, no changes
    result = executeGradle(GATLING_RUN_TASK_NAME);
    assertThat(result.task(":compileGatlingScala").getOutcome()).isEqualTo(UP_TO_DATE);
    assertThat(result.task(":" + GATLING_RUN_TASK_NAME).getOutcome()).isEqualTo(SUCCESS);

    // 3rd time, with changes
    appendToFile(
        new File(srcDir, "example/BasicSimulation.scala"),
        "\ncase class MyClz(str: String) // some fake code to change source file\n");
    result = executeGradle(GATLING_RUN_TASK_NAME);
    assertThat(result.task(":compileGatlingScala").getOutcome()).isEqualTo(SUCCESS);
    assertThat(result.task(":" + GATLING_RUN_TASK_NAME).getOutcome()).isEqualTo(SUCCESS);
  }
}
