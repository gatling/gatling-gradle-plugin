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
package unit;

import static io.gatling.gradle.GatlingPlugin.ENTERPRISE_DEPLOY_TASK_NAME;
import static io.gatling.gradle.GatlingPlugin.ENTERPRISE_START_TASK_NAME;
import static io.gatling.gradle.GatlingPlugin.GATLING_RUN_TASK_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import helper.GatlingUnitTestBase;
import io.gatling.gradle.GatlingPluginExtension;
import io.gatling.gradle.GatlingRunTask;
import io.gatling.plugin.GatlingConstants;
import java.util.List;
import java.util.stream.Collectors;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.language.jvm.tasks.ProcessResources;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GatlingPluginTest extends GatlingUnitTestBase {

  private List<String> dependencies(String configuration) {
    return project.getConfigurations().getByName(configuration).getAllDependencies().stream()
        .map(dependency -> dependency.getName() + ":" + dependency.getVersion())
        .collect(Collectors.toList());
  }

  @Test
  void shouldCreateGatlingConfigurations() {
    for (String name :
        List.of("gatling", "gatlingCompileOnly", "gatlingImplementation", "gatlingRuntimeOnly")) {
      assertThat(project.getConfigurations().findByName(name)).as(name).isNotNull();
    }
  }

  @Test
  void shouldCreateGatlingExtension() {
    assertThat(gatlingExt.getJvmArgs()).isEqualTo(GatlingConstants.DEFAULT_JVM_OPTIONS_GATLING);
    assertThat(gatlingExt.getSystemProperties())
        .isEqualTo(GatlingPluginExtension.DEFAULT_SYSTEM_PROPS);
  }

  @Test
  void shouldAddGatlingDependenciesWithDefaultVersion() {
    evaluate();
    assertThat(dependencies("gatling"))
        .contains("gatling-charts-highcharts:" + GatlingPluginExtension.GATLING_VERSION);
    assertThat(dependencies("gatlingImplementation"))
        .contains("scala-library:" + GatlingPluginExtension.SCALA_VERSION);
  }

  @Test
  void shouldAllowOverridingGatlingVersionViaExtension() {
    gatlingExt.setGatlingVersion("3.5.1");
    evaluate();
    assertThat(dependencies("gatling")).contains("gatling-charts-highcharts:3.5.1");
  }

  @Test
  void shouldAllowOverridingScalaVersionViaExtension() {
    gatlingExt.setScalaVersion("2.11.3");
    evaluate();
    assertThat(dependencies("gatlingImplementation")).contains("scala-library:2.11.3");
  }

  @Test
  void shouldCreateGatlingRunTask() {
    GatlingRunTask task = (GatlingRunTask) project.getTasks().getByName(GATLING_RUN_TASK_NAME);
    assertThat(task.getJvmArgs()).isNull();
    assertThat(task.getSystemProperties()).isNull();
    assertThat(task.getDependsOn())
        .singleElement()
        .isInstanceOfSatisfying(
            TaskProvider.class,
            provider -> assertThat(provider.getName()).isEqualTo("gatlingClasses"));
  }

  @Test
  void shouldCreateProcessGatlingResourcesTask() {
    assertThat(project.getTasks().getByName("processGatlingResources"))
        .isInstanceOf(ProcessResources.class);
  }

  @ParameterizedTest
  @ValueSource(strings = {ENTERPRISE_DEPLOY_TASK_NAME, ENTERPRISE_START_TASK_NAME})
  void taskShouldHaveASingleAction(String taskName) {
    // start mustn't run the deploy action a second time
    assertThat(project.getTasks().getByName(taskName).getActions()).hasSize(1);
  }
}
