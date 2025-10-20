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
package func

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

import helper.GatlingFuncSpec
import org.gradle.testkit.runner.BuildResult

class WhenKotlinCompileScalaSimulationSpec extends GatlingFuncSpec {
  static def GATLING_CLASSES_TASK_NAME = "gatlingClasses"

  def setup() {
    prepareKotlinTestWithScala("/gradle-layout")
  }

  def "should compile"() {
    when:
    BuildResult result = executeGradle(GATLING_CLASSES_TASK_NAME)
    then: "compiled successfully"
    result.task(":$GATLING_CLASSES_TASK_NAME").outcome == SUCCESS
    and: "only layout specific simulations were compiled"
    def classesDir = new File(buildDir, "classes/scala/gatling")
    classesDir.exists()
    and: "only layout specific resources are copied"
    def resourcesDir = new File(buildDir, "resources/gatling")
    resourcesDir.exists()
    new File(resourcesDir, "search.csv").exists()
    and: "main classes are compiled"
    def mainDir = new File(buildDir, "classes/java/main")
    mainDir.exists()
    and: "test classes are compiled"
    def testDir = new File(buildDir, "classes/java/test")
    testDir.exists()
  }
}
