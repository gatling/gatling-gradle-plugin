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

import helper.GatlingFuncTestBase;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

class WhenGroovyConfigureExtensionTest extends GatlingFuncTestBase {

  @Test
  void shouldConfigureEnterpriseSettingsWithTheDsl() {
    // Gradle generates the closure overload of enterprise(Action) only in the Groovy DSL
    prepareGroovyTestWithJava("/gradle-layout");
    appendToBuildFile(
        """
        gatling {
          enterprise {
            simulationName 'my-simulation'
            runTitle = 'my-title'
            waitForRunEnd true
          }
        }
        gatling.enterprise.packageDescriptorFilename 'my-descriptor.conf'

        println "@@@@ simulationName=${gatling.enterprise.simulationName}"
        println "@@@@ runTitle=${gatling.enterprise.runTitle}"
        println "@@@@ waitForRunEnd=${gatling.enterprise.waitForRunEnd}"
        println "@@@@ packageDescriptorFilename=${gatling.enterprise.packageDescriptorFilename}"
        """);

    BuildResult result = executeGradle("help");

    assertThat(result.getOutput())
        .contains(
            "@@@@ simulationName=my-simulation",
            "@@@@ runTitle=my-title",
            "@@@@ waitForRunEnd=true",
            "@@@@ packageDescriptorFilename=my-descriptor.conf");
  }
}
