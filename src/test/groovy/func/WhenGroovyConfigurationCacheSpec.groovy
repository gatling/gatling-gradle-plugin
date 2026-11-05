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

import static io.gatling.gradle.GatlingPlugin.ENTERPRISE_DEPLOY_TASK_NAME
import static io.gatling.gradle.GatlingPlugin.ENTERPRISE_START_TASK_NAME
import static io.gatling.gradle.GatlingPlugin.GATLING_RECORDER_TASK_NAME
import static io.gatling.gradle.GatlingPlugin.GATLING_RUN_TASK_NAME
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

import helper.GatlingFuncSpec
import org.gradle.testkit.runner.BuildResult
import spock.lang.Unroll

// Gradle 8.4 is excluded: its own ScalaCompile task can't be restored from the configuration cache
class WhenGroovyConfigurationCacheSpec extends GatlingFuncSpec {

  def setup() {
    prepareGroovyTestWithScala("/gradle-layout")
  }

  @Unroll
  def "run task is compatible with the configuration cache #gradleVersion"() {
    when: "running all simulations twice"
    build(gradleVersion, [GATLING_RUN_TASK_NAME, "--non-interactive", "--all"])
    def result = build(gradleVersion, [GATLING_RUN_TASK_NAME, "--non-interactive", "--all"])

    then: "the run task is successful and the configuration cache is reused"
    result.task(":$GATLING_RUN_TASK_NAME").outcome == SUCCESS
    result.output.contains('Reusing configuration cache')

    where:
    gradleVersion << (SUPPORTED_GRADLE_VERSIONS - ['8.4'])
  }

  // dry-run: tasks are stored in and restored from the configuration cache without being executed
  // (the recorder is interactive, deploy and start require a Gatling Enterprise API token)
  @Unroll
  def "#taskName task can be stored in the configuration cache #gradleVersion"() {
    when: "computing the task graph twice"
    build(gradleVersion, [taskName, "--dry-run"])
    def result = build(gradleVersion, [taskName, "--dry-run"])

    then: "no configuration cache problem is reported and the configuration cache is reused"
    result.output.contains('Reusing configuration cache')

    where:
    [taskName, gradleVersion] << [
      [
        GATLING_RECORDER_TASK_NAME,
        ENTERPRISE_DEPLOY_TASK_NAME,
        ENTERPRISE_START_TASK_NAME
      ],
      SUPPORTED_GRADLE_VERSIONS - ['8.4']
    ].combinations()
  }

  private BuildResult build(String gradleVersion, List<String> gradleArgs) {
    createRunner(*(gradleArgs + ['--configuration-cache']))
    .withGradleVersion(gradleVersion)
    .build()
  }
}
