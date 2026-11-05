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
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;

import helper.GatlingFuncTestBase;
import java.io.File;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WhenGroovyRunJavaSimulationTest extends GatlingFuncTestBase {

  @BeforeEach
  void setUp() {
    prepareGroovyTestWithJava("/gradle-layout");
  }

  @Test
  void shouldExecuteOnlyTheSimulationForcedBySimulationOption() {
    BuildResult result =
        executeGradle(
            GATLING_RUN_TASK_NAME, "--non-interactive", "--simulation=example.BasicSimulation");

    assertThat(result.task(":" + GATLING_RUN_TASK_NAME).getOutcome()).isEqualTo(SUCCESS);
    assertThat(new File(buildDir, "reports/gatling").listFiles()).hasSize(1);
    assertThat(result.getOutput()).doesNotContain("INFO");
  }

  @Test
  void shouldSupportCreatingACustomTaskBasedOnGatlingRunTask() {
    appendToBuildFile(
        """
        tasks.register('gatlingRunTaskCustom', io.gatling.gradle.GatlingRunTask) {
          dependsOn(project.tasks.named("gatlingClasses"))
          simulationClassName = "example.BasicSimulation"
        }
        """);

    BuildResult result = executeGradle("gatlingRunTaskCustom", "--non-interactive");

    assertThat(result.task(":gatlingRunTaskCustom").getOutcome()).isEqualTo(SUCCESS);
    assertThat(new File(buildDir, "reports/gatling").listFiles()).hasSize(1);
    assertThat(result.getOutput()).doesNotContain("INFO");
  }
}
