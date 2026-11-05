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
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

class WhenKotlinCompileScalaSimulationTest extends GatlingFuncTestBase {

  private static final String GATLING_CLASSES_TASK_NAME = "gatlingClasses";

  @Test
  void shouldCompile() {
    prepareKotlinTestWithScala("/gradle-layout");

    BuildResult result = executeGradle(GATLING_CLASSES_TASK_NAME);

    assertThat(result.task(":" + GATLING_CLASSES_TASK_NAME).getOutcome()).isEqualTo(SUCCESS);
    assertThat(new File(buildDir, "classes/scala/gatling")).isDirectory();
    File resourcesDir = new File(buildDir, "resources/gatling");
    assertThat(resourcesDir).isDirectory();
    assertThat(new File(resourcesDir, "search.csv")).exists();
    assertThat(new File(buildDir, "classes/java/main")).isDirectory();
    assertThat(new File(buildDir, "classes/java/test")).isDirectory();
  }
}
