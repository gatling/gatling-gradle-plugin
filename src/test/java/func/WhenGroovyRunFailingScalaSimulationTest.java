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

import helper.GatlingFuncTestBase;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WhenGroovyRunFailingScalaSimulationTest extends GatlingFuncTestBase {

  @BeforeEach
  void setUp() {
    prepareGroovyTestWithScala("/gradle-layout");
  }

  private void writeFailedSimulation(String content) {
    writeFile(new File(srcDir, "example/AFailedSimulation.scala"), content);
  }

  private File report(String simulationPrefix) {
    File[] reports = new File(buildDir, "reports/gatling").listFiles();
    return Arrays.stream(reports)
        .filter(report -> report.getName().startsWith(simulationPrefix))
        .findFirst()
        .orElseThrow(() -> new AssertionError("No report for " + simulationPrefix));
  }

  // simulation.log is binary: ISO-8859-1 maps each byte to a char, so ASCII text can be searched
  private static String binaryContent(File file) {
    try {
      return new String(Files.readAllBytes(file.toPath()), StandardCharsets.ISO_8859_1);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void assertBothSimulationsRanAndFailedOneHitUnknownHost() {
    assertThat(new File(buildDir, "reports/gatling").listFiles()).hasSize(2);
    report("basicsimulation");
    assertThat(binaryContent(new File(report("afailedsimulation"), "simulation.log")))
        .contains("UnknownHostException: qwe.asd.io");
  }

  @Test
  void shouldExecuteAllSimulationsEvenIfOneFailsBecauseOfGatlingAssertions() {
    appendToBuildFile(
        """
        gatling {
          includes = ['example.AFailedSimulation', 'example.BasicSimulation']
        }
        """);
    writeFailedSimulation(
        """
        package example
        import io.gatling.core.Predef._
        import io.gatling.http.Predef._
        class AFailedSimulation extends Simulation {
          val httpConf = http.baseUrl("http://qwe.asd.io")
          val scn = scenario("Scenario Name").exec(http("request_1").get("/"))
          setUp(scn.inject(atOnceUsers(1)).protocols(httpConf)).assertions(
            global.successfulRequests.percent.gt(99)
          )
        }
        """);

    BuildResult result = executeGradleAndFail(GATLING_RUN_TASK_NAME, "--non-interactive", "--all");

    assertThat(result.task(":" + GATLING_RUN_TASK_NAME).getOutcome()).isEqualTo(FAILED);
    // only the failing simulation is reported
    assertThat(result.getOutput().lines())
        .anyMatch(line ->
            line.endsWith("Some of the simulations failed assertions: example.AFailedSimulation"));
    assertBothSimulationsRanAndFailedOneHitUnknownHost();
  }

  @Test
  void shouldIgnoreIfSimulationWithoutAssertionsFailsWithHttpRequests() {
    appendToBuildFile(
        """
        gatling {
          includes = ['example.AFailedSimulation', 'example.BasicSimulation']
        }
        """);
    writeFailedSimulation(
        """
        package example
        import io.gatling.core.Predef._
        import io.gatling.http.Predef._
        class AFailedSimulation extends Simulation {
          val httpConf = http.baseUrl("http://qwe.asd.io")
          val scn = scenario("Scenario Name").exec(http("request_1").get("/"))
          setUp(scn.inject(atOnceUsers(1)).protocols(httpConf))
        }
        """);

    BuildResult result = executeGradle(GATLING_RUN_TASK_NAME, "--non-interactive", "--all");

    assertThat(result.task(":" + GATLING_RUN_TASK_NAME).getOutcome()).isEqualTo(SUCCESS);
    assertBothSimulationsRanAndFailedOneHitUnknownHost();
  }

  @Test
  void shouldExecuteAllSimulationsEvenIfOneFailsBecauseOfGatlingRuntime() {
    appendToBuildFile(
        """
        gatling {
          includes = ['example.BasicSimulation', 'example.AFailedSimulation']
        }
        """);
    writeFailedSimulation(
        """
        package example
        import io.gatling.core.Predef._
        import io.gatling.http.Predef._
        class AFailedSimulation extends Simulation {}
        """);

    BuildResult result = executeGradleAndFail(GATLING_RUN_TASK_NAME, "--non-interactive", "--all");

    assertThat(result.task(":" + GATLING_RUN_TASK_NAME).getOutcome()).isEqualTo(FAILED);
    // only the crashed simulation is reported
    assertThat(result.getOutput().lines())
        .anyMatch(
            line -> line.endsWith("Some of the simulations crashed: example.AFailedSimulation"));
    assertThat(new File(buildDir, "reports/gatling").listFiles()).hasSize(1);
    report("basicsimulation");
  }
}
