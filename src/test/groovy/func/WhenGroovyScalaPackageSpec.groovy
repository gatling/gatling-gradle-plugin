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

import static io.gatling.gradle.GatlingPlugin.ENTERPRISE_PACKAGE_TASK_NAME
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

import helper.GatlingFuncSpec
import org.gradle.testkit.runner.BuildResult
import spock.lang.Unroll

class WhenGroovyScalaPackageSpec extends GatlingFuncSpec {

  def setup() {
    prepareGroovyTestWithScala("/gradle-layout")
  }

  @Unroll
  def "should successfully create a package for gradle version #gradleVersion"() {
    when:
    BuildResult result = createRunner(ENTERPRISE_PACKAGE_TASK_NAME)
            .withGradleVersion(gradleVersion)
            .build()
    then: "default tasks were executed successfully"
    result.task(":$ENTERPRISE_PACKAGE_TASK_NAME").outcome == SUCCESS
    result.task(":gatlingClasses").outcome == SUCCESS
    def artifactId = projectDir.root.getName()
    def artifact = new File(buildDir, "libs/${artifactId}-tests.jar")
    and: "artifact was created"
    artifact.isFile()
    where:
    gradleVersion << SUPPORTED_GRADLE_VERSIONS
  }
}
