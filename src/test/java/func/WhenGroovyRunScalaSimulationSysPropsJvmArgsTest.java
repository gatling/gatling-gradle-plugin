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

import helper.GatlingDebug;
import helper.GatlingFuncTestBase;
import io.gatling.gradle.GatlingPluginExtension;
import io.gatling.plugin.GatlingConstants;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WhenGroovyRunScalaSimulationSysPropsJvmArgsTest extends GatlingFuncTestBase {

  @BeforeEach
  void setUp() {
    prepareGroovyTestWithScala("/gatling-debug");
    writeFile(
        new File(projectDir, "src/gatling/resources/gatling.conf"), "gatling.data.writers = []");
  }

  private static Map<String, String> gradleGatlingSystemProperties(GatlingDebug debug) {
    return debug.systemProperties.entrySet().stream()
        .filter(entry -> entry.getKey().startsWith("gradle_gatling_"))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  @Test
  void shouldSetMemoryLimitsFromJvmArgsOfGatlingExtension() {
    // default Xmx from gatling extension
    GatlingDebug debug = new GatlingDebug(executeGradle(GATLING_RUN_TASK_NAME));
    assertThat(debug.heap.get("max")).isEqualTo(1024L * 1024 * 1024);

    // override via gatling extension
    appendToBuildFile(
        """
        gatling {
          jvmArgs = ['-Xms32m', '--add-opens=java.base/java.lang=ALL-UNNAMED', '--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED']
        }
        """);
    debug = new GatlingDebug(executeGradle("--rerun-tasks", GATLING_RUN_TASK_NAME));
    assertThat(debug.heap.get("min")).isEqualTo(32L * 1024 * 1024);
  }

  @Test
  void shouldConfigureJvmArgsFromExtension() {
    // default jvmArgs from gatling extension
    GatlingDebug debug = new GatlingDebug(executeGradle(GATLING_RUN_TASK_NAME));
    // Gradle drops -server
    assertThat(debug.jvmArgs)
        .containsExactlyInAnyOrderElementsOf(
            GatlingConstants.DEFAULT_JVM_OPTIONS_GATLING.stream()
                .filter(arg -> !arg.equals("-server"))
                .collect(Collectors.toList()));

    // override via gatling extension
    appendToBuildFile(
        """
        gatling {
          jvmArgs = ['-Xms32m', '-XX:+UseG1GC', '--add-opens=java.base/java.lang=ALL-UNNAMED', '--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED']
        }
        """);
    debug = new GatlingDebug(executeGradle("--rerun-tasks", GATLING_RUN_TASK_NAME));
    assertThat(debug.jvmArgs)
        .containsExactlyInAnyOrder(
            "-Xms32m",
            "-XX:+UseG1GC",
            "--add-opens=java.base/java.lang=ALL-UNNAMED",
            "--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED");
  }

  @Test
  void shouldOverrideJvmArgsFromGatlingRunProperties() {
    appendToBuildFile(
        """
        gatling {
          jvmArgs = ['-Xms32m', '-XX:+AggressiveOpts']
        }
        gatlingRun {
          jvmArgs = ['-Xms128m', '-XX:+UseG1GC', '--add-opens=java.base/java.lang=ALL-UNNAMED', '--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED']
        }
        """);

    GatlingDebug debug = new GatlingDebug(executeGradle(GATLING_RUN_TASK_NAME));

    assertThat(debug.jvmArgs)
        .containsExactlyInAnyOrder(
            "-Xms128m",
            "-XX:+UseG1GC",
            "--add-opens=java.base/java.lang=ALL-UNNAMED",
            "--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED");
  }

  @Test
  void shouldConfigureSystemPropertiesFromExtension() {
    // defaults from extension
    GatlingDebug debug = new GatlingDebug(executeGradle(GATLING_RUN_TASK_NAME));
    assertThat(debug.systemProperties.keySet())
        .containsAll(GatlingPluginExtension.DEFAULT_SYSTEM_PROPS.keySet());

    // override via extension
    appendToBuildFile(
        """
        gatling {
          systemProperties = ['gradle_gatling_1': 'aaa', 'gradle_gatling_2' : 'bbb']
        }
        """);
    debug = new GatlingDebug(executeGradle("--rerun-tasks", GATLING_RUN_TASK_NAME));
    assertThat(gradleGatlingSystemProperties(debug))
        .containsExactlyInAnyOrderEntriesOf(
            Map.of("gradle_gatling_1", "aaa", "gradle_gatling_2", "bbb"));
    // the extension's systemProperties replace the defaults
    assertThat(debug.systemProperties.keySet())
        .filteredOn(GatlingPluginExtension.DEFAULT_SYSTEM_PROPS::containsKey)
        .isEmpty();
  }

  @Test
  void shouldExtendSystemPropertiesFromGatlingRunProperties() {
    appendToBuildFile(
        """
        gatling {
          systemProperties = ['gradle_gatling_1': 'aaa', 'gradle_gatling_2' : 'bbb']
        }
        gatlingRun {
          systemProperties = ['gradle_gatling_2' : 'qwerty', 'gradle_gatling_3' : 'ccc']
        }
        """);

    GatlingDebug debug = new GatlingDebug(executeGradle(GATLING_RUN_TASK_NAME));

    assertThat(gradleGatlingSystemProperties(debug).keySet())
        .containsExactlyInAnyOrder("gradle_gatling_2", "gradle_gatling_3");
    assertThat(debug.systemProperties).containsEntry("gradle_gatling_2", "qwerty");
  }

  @Test
  void shouldSetAdditionalEnvVarsFromExtension() {
    appendToBuildFile(
        """
        gatling {
          environment['env1'] = 'env1_value'
        }
        """);

    GatlingDebug debug = new GatlingDebug(executeGradle(GATLING_RUN_TASK_NAME));

    assertThat(debug.env).containsEntry("env1", "env1_value");
  }

  @Test
  void shouldOverrideGatlingEnvVarsWithGatlingRun() {
    appendToBuildFile(
        """
        gatling {
          environment = ['env1': 'aaa', 'env2': 'bbb']
        }
        gatlingRun {
          environment = ['env2': 'ddd', 'env3': 'ccc']
        }
        """);

    GatlingDebug debug = new GatlingDebug(executeGradle(GATLING_RUN_TASK_NAME));

    assertThat(debug.env)
        .containsEntry("env1", "aaa")
        .containsEntry("env2", "ddd")
        .containsEntry("env3", "ccc");
  }

  @Test
  void shouldPassEnvVarsUpstream() {
    // not in-process: TestKit ignores withEnvironment in debug mode
    // withEnvironment replaces the whole environment, hence System.getenv() (PATH, JAVA_HOME...)
    Map<String, String> environment = new HashMap<>(System.getenv());
    environment.put("GRADLE_GATLING_ENV_UPSTREAM", "env_upstream_value");
    BuildResult result =
        createRunner(GATLING_RUN_TASK_NAME).withDebug(false).withEnvironment(environment).build();

    GatlingDebug debug = new GatlingDebug(result);

    assertThat(debug.env).containsEntry("GRADLE_GATLING_ENV_UPSTREAM", "env_upstream_value");
  }

  @Test
  void shouldPassSystemPropertiesUpstream() {
    GatlingDebug debug =
        new GatlingDebug(
            executeGradle(
                "-Dgradle_gatling_sys_upstream=sys_upstream_value", GATLING_RUN_TASK_NAME));

    assertThat(debug.systemProperties)
        .containsEntry("gradle_gatling_sys_upstream", "sys_upstream_value");
  }
}
