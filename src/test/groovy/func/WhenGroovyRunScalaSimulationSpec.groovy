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
import static org.gradle.testkit.runner.TaskOutcome.FAILED
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE

import helper.GatlingFuncSpec
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.UnexpectedBuildFailure
import spock.lang.Unroll

class WhenGroovyRunScalaSimulationSpec extends GatlingFuncSpec {

  @Unroll
  def "should execute all simulations for gradle version #gradleVersion when forced by --all option"() {
    setup:
    prepareGroovyTestWithScala("/gradle-layout")
    when:
    BuildResult result = createRunner(GATLING_RUN_TASK_NAME, "--non-interactive", "--all")
            .withGradleVersion(gradleVersion)
            .build()
    then: "default tasks were executed successfully"
    result.task(":$GATLING_RUN_TASK_NAME").outcome == SUCCESS
    result.task(":gatlingClasses").outcome == SUCCESS
    and: "all simulations were run"
    def reports = new File(buildDir, "reports/gatling")
    reports.exists() && reports.listFiles().size() == 2
    and: "logs doesn't contain INFO"
    !result.output.split().any { it.contains("INFO") }
    where:
    gradleVersion << SUPPORTED_GRADLE_VERSIONS
  }

  def "should execute only #simulation when forced by --simulation option"() {
    setup:
    prepareGroovyTestWithScala("/gradle-layout")
    when:
    BuildResult result = executeGradle(GATLING_RUN_TASK_NAME, "--non-interactive", "--simulation=example.BasicSimulation")
    then: "custom task was run successfully"
    result.task(":$GATLING_RUN_TASK_NAME").outcome == SUCCESS
    and: "only one simulation was executed"
    new File(buildDir, "reports/gatling").listFiles().size() == 1
    and: "logs doesn't contain INFO"
    !result.output.split().any { it.contains("INFO") }
  }

  def "fail when no simulation is found"() {
    setup:
    prepareGroovyTestWithScala(null)
    when:
    executeGradle(GATLING_RUN_TASK_NAME, "--non-interactive", "--all")
    then:
    UnexpectedBuildFailure ex = thrown(UnexpectedBuildFailure)
    ex.buildResult.task(":$GATLING_RUN_TASK_NAME").outcome == FAILED
  }

  def "should run gatling twice even if no changes to source code"() {
    setup:
    prepareGroovyTestWithScala("/gradle-layout")
    buildFile << """
gatling { includes = ['example.BasicSimulation'] }
"""
    when: '1st time'
    BuildResult result = executeGradle(GATLING_RUN_TASK_NAME, "--non-interactive", "--all")
    then:
    result.task(":compileGatlingScala").outcome == SUCCESS
    result.task(":$GATLING_RUN_TASK_NAME").outcome == SUCCESS

    when: '2nd time no changes'
    result = executeGradle("$GATLING_RUN_TASK_NAME")
    then:
    result.task(":compileGatlingScala").outcome == UP_TO_DATE
    result.task(":$GATLING_RUN_TASK_NAME").outcome == SUCCESS

    when: '3r time with changes'
    new File(new File(srcDir, "example"), "BasicSimulation.scala") << """
case class MyClz(str: String) // some fake code to change source file
"""
    result = executeGradle("$GATLING_RUN_TASK_NAME", )
    then:
    result.task(":compileGatlingScala").outcome == SUCCESS
    result.task(":$GATLING_RUN_TASK_NAME").outcome == SUCCESS
  }
}
