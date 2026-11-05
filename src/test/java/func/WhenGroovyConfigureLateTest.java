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

import helper.GatlingFuncTestBase;
import java.io.File;
import java.io.IOException;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// settings changed after the tasks are realized (e.g. version set by a release plugin in
// afterEvaluate) must still be honored
class WhenGroovyConfigureLateTest extends GatlingFuncTestBase {

  @BeforeEach
  void setUp() {
    prepareGroovyTestWithScala("/gradle-layout");
  }

  @Test
  void shouldHonorProjectSettingsChangedAfterTasksAreRealized() throws IOException {
    appendToBuildFile(
        """
        def packageTask = tasks.gatlingEnterprisePackage
        def runTask = tasks.gatlingRun
        def recorderTask = tasks.gatlingRecorder

        afterEvaluate {
          group = 'com.example.late'
          version = '1.2.3'
          layout.buildDirectory = file('late-build')
        }

        tasks.register('printLateSettings') {
          doLast {
            println "@@@@ groupId=${packageTask.groupId}"
            println "@@@@ artifactVersion=${packageTask.artifactVersion}"
            println "@@@@ runReportDir=${runTask.gatlingReportDir}"
            println "@@@@ recorderReportDir=${recorderTask.gatlingReportDir}"
            println "@@@@ recorderArgs=${recorderTask.createRecorderArgs()}"
          }
        }
        """);

    BuildResult result = executeGradle("printLateSettings");

    String lateReportDir =
        new File(projectDir.getCanonicalFile(), "late-build/reports/gatling").getPath();
    assertThat(result.getOutput())
        .contains(
            "@@@@ groupId=com.example.late",
            "@@@@ artifactVersion=1.2.3",
            "@@@@ runReportDir=" + lateReportDir,
            "@@@@ recorderReportDir=" + lateReportDir)
        .containsPattern("@@@@ recorderArgs=\\[.*com\\.example\\.late.*]");
  }

  @Test
  void shouldHonorIncludesChangedAfterTheRunTaskIsRealized() {
    appendToBuildFile(
        """
        tasks.gatlingRun {
          // force creation of task
        }
        afterEvaluate {
          gatling { includes = ['example.DoesNotExist'] }
        }
        """);

    // without the late includes, the fixture's several simulations would fail with
    // "more than 1 simulation" in non-interactive mode
    BuildResult result = executeGradleAndFail(GATLING_RUN_TASK_NAME, "--non-interactive");

    assertThat(result.getOutput()).contains("No simulations to run");
  }
}
