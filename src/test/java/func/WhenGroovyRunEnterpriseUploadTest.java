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

import static io.gatling.gradle.GatlingPlugin.ENTERPRISE_UPLOAD_TASK_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.testkit.runner.TaskOutcome.FAILED;

import helper.GatlingFuncTestBase;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

class WhenGroovyRunEnterpriseUploadTest extends GatlingFuncTestBase {

  @Test
  void shouldFailWithAPointerToTheDeployCommand() {
    prepareGroovyTestWithJava("/gradle-layout");

    BuildResult result = executeGradleAndFail(ENTERPRISE_UPLOAD_TASK_NAME);

    assertThat(result.task(":" + ENTERPRISE_UPLOAD_TASK_NAME).getOutcome()).isEqualTo(FAILED);
    assertThat(result.getOutput())
        .contains("The enterprise upload command is no longer supported")
        .contains("replaced by the enterprise deploy command");
  }
}
