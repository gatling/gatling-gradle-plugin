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

import static io.gatling.gradle.GatlingPlugin.GATLING_RUN_TASK_NAME
import static org.gradle.testkit.runner.TaskOutcome.*

import helper.GatlingFuncSpec
import org.gradle.testkit.runner.BuildResult

class WhenGroovyRunJavaSimulationSpec extends GatlingFuncSpec {

  def "should execute only #simulation when forced by --simulation option"() {
    setup:
    prepareGroovyTestWithJava("/gradle-layout")
    when:
    BuildResult result = executeGradle(GATLING_RUN_TASK_NAME, "--non-interactive", "--simulation=example.BasicSimulation")
    then: "task with args was run successfully"
    result.task(":$GATLING_RUN_TASK_NAME").outcome == SUCCESS
    and: "only one simulation was executed"
    new File(buildDir, "reports/gatling").listFiles().size() == 1
    and: "logs doesn't contain INFO"
    !result.output.split().any { it.contains("INFO") }
  }

  def "should support creating a custom task based on GatlingRunTask"() {
    var gatlingRunTaskCustomName = "gatlingRunTaskCustom"

    setup:
    prepareGroovyTestWithJava("/gradle-layout")
    buildFile.append """
tasks.register('$gatlingRunTaskCustomName', io.gatling.gradle.GatlingRunTask) {
    dependsOn(project.tasks.named("gatlingClasses"))
    simulationClassName = "example.BasicSimulation"
}
"""
    when:
    BuildResult result = executeGradle(gatlingRunTaskCustomName, "--non-interactive")
    then: "custom task was run successfully"
    result.task(":$gatlingRunTaskCustomName").outcome == SUCCESS
    and: "only one simulation was executed"
    new File(buildDir, "reports/gatling").listFiles().size() == 1
    and: "logs doesn't contain INFO"
    !result.output.split().any { it.contains("INFO") }
  }
}
