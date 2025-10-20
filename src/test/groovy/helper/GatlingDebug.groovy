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
package helper

import groovy.json.JsonSlurper
import io.gatling.gradle.GatlingPlugin
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

class GatlingDebug {

  Map systemProperties
  Map env

  List<String> jvmArgs
  List<String> argv

  Map heap

  GatlingDebug(BuildResult buildResult) {
    assert buildResult.task(":$GatlingPlugin.GATLING_RUN_TASK_NAME").outcome == TaskOutcome.SUCCESS

    def lines = buildResult.output.readLines().findAll { it.startsWith("@@@@") }
    assert lines.size() == 4

    def jsonSlurper = new JsonSlurper()

    this.heap = jsonSlurper.parseText(lines.find { it.startsWith("@@@@.heap") } - "@@@@.heap ")
    this.jvmArgs = jsonSlurper.parseText(lines.find { it.startsWith("@@@@.jvm") } - "@@@@.jvm ")
    this.systemProperties = jsonSlurper.parseText((lines.find { it.startsWith("@@@@.sys") } - "@@@@.sys "))
    this.env = jsonSlurper.parseText((lines.find { it.startsWith("@@@@.env") } - "@@@@.env "))

    this.argv = this.systemProperties["sun.java.command"].split("\\s") as List
  }
}
