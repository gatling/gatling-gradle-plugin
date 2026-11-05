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

import static io.gatling.gradle.GatlingPlugin.ENTERPRISE_DEPLOY_TASK_NAME;
import static io.gatling.gradle.GatlingPlugin.ENTERPRISE_START_TASK_NAME;
import static io.gatling.gradle.GatlingPlugin.GATLING_RECORDER_TASK_NAME;
import static io.gatling.gradle.GatlingPlugin.GATLING_RUN_TASK_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;

import helper.GatlingFuncTestBase;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

// Gradle 8.4 is excluded: its own ScalaCompile task can't be restored from the configuration cache
class WhenGroovyConfigurationCacheTest extends GatlingFuncTestBase {

  @BeforeEach
  void setUp() {
    prepareGroovyTestWithScala("/gradle-layout");
  }

  private BuildResult build(String gradleVersion, String... gradleArgs) {
    List<String> arguments = new ArrayList<>(List.of(gradleArgs));
    arguments.add("--configuration-cache");
    return createRunner(arguments.toArray(new String[0])).withGradleVersion(gradleVersion).build();
  }

  @ParameterizedTest(name = "Gradle {0}")
  @MethodSource("supportedGradleVersionsForScalaConfigurationCache")
  void runTaskIsCompatibleWithTheConfigurationCache(String gradleVersion) {
    build(gradleVersion, GATLING_RUN_TASK_NAME, "--non-interactive", "--all");
    BuildResult result = build(gradleVersion, GATLING_RUN_TASK_NAME, "--non-interactive", "--all");

    assertThat(result.task(":" + GATLING_RUN_TASK_NAME).getOutcome()).isEqualTo(SUCCESS);
    assertThat(result.getOutput()).contains("Reusing configuration cache");
  }

  static Stream<Arguments> dryRunTasksAndGradleVersions() {
    return Stream.of(
            GATLING_RECORDER_TASK_NAME, ENTERPRISE_DEPLOY_TASK_NAME, ENTERPRISE_START_TASK_NAME)
        .flatMap(
            taskName ->
                supportedGradleVersionsForScalaConfigurationCache()
                    .map(gradleVersion -> Arguments.of(taskName, gradleVersion)));
  }

  // dry-run: tasks are stored in and restored from the configuration cache without being executed
  // (the recorder is interactive, deploy and start require a Gatling Enterprise API token)
  @ParameterizedTest(name = "{0} on Gradle {1}")
  @MethodSource("dryRunTasksAndGradleVersions")
  void taskCanBeStoredInTheConfigurationCache(String taskName, String gradleVersion) {
    build(gradleVersion, taskName, "--dry-run");
    BuildResult result = build(gradleVersion, taskName, "--dry-run");

    assertThat(result.getOutput()).contains("Reusing configuration cache");
  }
}
