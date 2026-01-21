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

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner

abstract class GatlingFuncSpec extends GatlingSpec {

  static def GATLING_HOST_NAME_SYS_PROP = "-Dtest.gatling.hostName=https://api-ecomm.gatling.io"

  void prepareGroovyTestWithScala(String fixtureDir) {
    createBuildFolder(fixtureDir, SimulationLanguage.SCALA)
    generateBuildScript(GradleScriptingLanguage.GROOVY, SimulationLanguage.SCALA)
  }

  void prepareGroovyTestWithJava(String fixtureDir) {
    createBuildFolder(fixtureDir, SimulationLanguage.JAVA)
    generateBuildScript(GradleScriptingLanguage.GROOVY, SimulationLanguage.JAVA)
  }

  void prepareKotlinTestWitKotlin(String fixtureDir) {
    createBuildFolder(fixtureDir, SimulationLanguage.KOTLIN)
    generateBuildScript(GradleScriptingLanguage.KOTLIN, SimulationLanguage.KOTLIN)
  }

  void prepareKotlinTestWithScala(String fixtureDir) {
    createBuildFolder(fixtureDir, SimulationLanguage.SCALA)
    generateBuildScript(GradleScriptingLanguage.KOTLIN, SimulationLanguage.SCALA)
  }

  protected GradleRunner createRunner(String... gradleArgs) {
    GradleRunner.create().forwardOutput()
            .withProjectDir(projectDir.getRoot())
            .withArguments(["--stacktrace", GATLING_HOST_NAME_SYS_PROP] + (gradleArgs as List))
            .withPluginClasspath()
            .withDebug(true)
  }

  BuildResult executeGradle(String... gradleArgs) {
    createRunner(gradleArgs).build()
  }

  protected static final List<String> SUPPORTED_GRADLE_VERSIONS = ["8.4", "8.14.3", "9.3.0"]
}
