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

import helper.GatlingFuncSpec
import org.gradle.testkit.runner.UnexpectedBuildFailure

class WhenGroovyRunFailingScalaSimulationSpec extends GatlingFuncSpec {

  def setup() {
    prepareGroovyTestWithScala("/gradle-layout")
  }

  def "should execute all simulations even if one fails because of gatling assertions"() {
    given:
    buildFile << """
gatling {
  includes = ['example.AFailedSimulation', 'example.BasicSimulation']
}
"""
    and: "add incorrect simulation"
    new File(srcDir, "example/AFailedSimulation.scala").text = """
package example
import io.gatling.core.Predef._
import io.gatling.http.Predef._
class AFailedSimulation extends Simulation {
  val httpConf = http.baseUrl("http://qwe.asd.io")
  val scn = scenario("Scenario Name").exec(http("request_1").get("/"))
  setUp(scn.inject(atOnceUsers(1)).protocols(httpConf)).assertions(
    global.successfulRequests.percent.gt(99)
  )
}
"""
    when:
    executeGradle(GATLING_RUN_TASK_NAME, "--non-interactive", "--all")
    then:
    UnexpectedBuildFailure ex = thrown(UnexpectedBuildFailure)
    ex.buildResult.task(":$GATLING_RUN_TASK_NAME").outcome == FAILED
    and: "only single simulation reported as failed"
    ex.buildResult.output.readLines().any {
      it.endsWith("Some of the simulations failed assertions: example.AFailedSimulation")
    }
    and: "all simulations were run"
    with(new File(buildDir, "reports/gatling")) { reports ->
      reports.exists() && reports.listFiles().size() == 2
      reports.listFiles().find { it.name.startsWith("basicsimulation") } != null
      reports.listFiles().find { it.name.startsWith("afailedsimulation") } != null
      new File(reports.listFiles().find { it.name.startsWith("afailedsimulation") }, "simulation.log").text.contains("UnknownHostException: qwe.asd.io")
    }
  }

  def "should ignore if simulation without assertions fails with HTTP requests"() {
    given:
    buildFile << """
gatling {
  includes = ['example.AFailedSimulation', 'example.BasicSimulation']
}
"""
    and: "add incorrect simulation"
    new File(srcDir, "example/AFailedSimulation.scala").text = """
package example
import io.gatling.core.Predef._
import io.gatling.http.Predef._
class AFailedSimulation extends Simulation {
  val httpConf = http.baseUrl("http://qwe.asd.io")
  val scn = scenario("Scenario Name").exec(http("request_1").get("/"))
  setUp(scn.inject(atOnceUsers(1)).protocols(httpConf))
}
"""
    when:
    def buildResult = executeGradle(GATLING_RUN_TASK_NAME, "--non-interactive", "--all")
    then:
    buildResult.task(":$GATLING_RUN_TASK_NAME").outcome == SUCCESS
    and:
    with(new File(buildDir, "reports/gatling")) { reports ->
      reports.exists() && reports.listFiles().size() == 2
      reports.listFiles().find { it.name.startsWith("basicsimulation") } != null
      reports.listFiles().find { it.name.startsWith("afailedsimulation") } != null
      new File(reports.listFiles().find { it.name.startsWith("afailedsimulation") }, "simulation.log").text.contains("UnknownHostException: qwe.asd.io")
    }
  }

  def "should execute all simulations even if one fails because of gatling runtime"() {
    given:
    buildFile << """
gatling {
  includes = ['example.BasicSimulation', 'example.AFailedSimulation']
}
"""
    and: "add incorrect simulation"
    new File(srcDir, "example/AFailedSimulation.scala").text = """
package example
import io.gatling.core.Predef._
import io.gatling.http.Predef._
class AFailedSimulation extends Simulation {}
"""
    when:
    executeGradle(GATLING_RUN_TASK_NAME, "--non-interactive", "--all")
    then:
    UnexpectedBuildFailure ex = thrown(UnexpectedBuildFailure)
    ex.buildResult.task(":$GATLING_RUN_TASK_NAME").outcome == FAILED
    and: "only single simulation reported as failed"
    ex.buildResult.output.readLines().any {
      it.endsWith("Some of the simulations crashed: example.AFailedSimulation")
    }
    and:
    with(new File(buildDir, "reports/gatling")) { reports ->
      reports.exists() && reports.listFiles().size() == 1
      reports.listFiles().find { it.name.startsWith("basicsimulation") } != null
    }
  }
}
