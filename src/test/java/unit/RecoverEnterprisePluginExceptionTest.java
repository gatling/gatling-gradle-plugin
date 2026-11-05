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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import helper.GatlingUnitTestBase;
import io.gatling.gradle.RecoverEnterprisePluginException;
import io.gatling.plugin.exceptions.UnsupportedJavaVersionException;
import io.gatling.plugin.exceptions.UserQuitException;
import java.io.IOException;
import org.gradle.api.BuildCancelledException;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RecoverEnterprisePluginExceptionTest extends GatlingUnitTestBase {

  private Task task;

  @BeforeEach
  void setUpTask() {
    task = project.getTasks().getByName(ENTERPRISE_DEPLOY_TASK_NAME);
  }

  @Test
  void shouldReturnTheResult() throws Exception {
    assertThat(RecoverEnterprisePluginException.handle(task, () -> "result")).isEqualTo("result");
  }

  @Test
  void shouldTurnUnsupportedJavaVersionIntoGuidance() {
    assertThatThrownBy(
            () ->
                RecoverEnterprisePluginException.handle(
                    task,
                    () -> {
                      throw new UnsupportedJavaVersionException(25, 21);
                    }))
        .isInstanceOf(TaskExecutionException.class)
        .hasRootCauseInstanceOf(IllegalArgumentException.class)
        .rootCause()
        .hasMessageContaining("options.release = 21")
        .hasMessageContaining("published targeting Java 25");
  }

  @Test
  void shouldTurnUserQuitIntoBuildCancellation() {
    assertThatThrownBy(
            () ->
                RecoverEnterprisePluginException.handle(
                    task,
                    () -> {
                      throw new UserQuitException();
                    }))
        .isInstanceOf(BuildCancelledException.class);
  }

  @Test
  void shouldRethrowOtherExceptionsUnchanged() {
    IOException failure = new IOException("boom");
    assertThatThrownBy(
            () ->
                RecoverEnterprisePluginException.handle(
                    task,
                    () -> {
                      throw failure;
                    }))
        .isSameAs(failure);
  }
}
