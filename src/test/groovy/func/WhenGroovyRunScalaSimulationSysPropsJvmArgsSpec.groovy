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

import helper.GatlingDebug
import helper.GatlingFuncSpec
import io.gatling.gradle.GatlingPluginExtension
import io.gatling.plugin.GatlingConstants
import org.gradle.testkit.runner.BuildResult
import org.junit.Rule
import org.junit.contrib.java.lang.system.EnvironmentVariables
import org.junit.contrib.java.lang.system.RestoreSystemProperties

class WhenGroovyRunScalaSimulationSysPropsJvmArgsSpec extends GatlingFuncSpec {

  @Rule
  public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties()

  @Rule
  public final EnvironmentVariables environmentVariables = new EnvironmentVariables()

  def setup() {
    prepareGroovyTestWithScala("/gatling-debug")
    new File(new File(projectDir.root, "src/gatling/resources"), "gatling.conf").text = "gatling.data.writers = []"
  }

  def "should set memory limits from jvmArgs of gatling extension"() {
    when: "default Xmx from gatling extension"
    BuildResult result = executeGradle(GATLING_RUN_TASK_NAME)
    then:
    with(new GatlingDebug(result)) {
      heap.max == 1024 * 1024 * 1024// 1GB
    }

    when: "override via gatling extension"
    buildFile << """
gatling {
    jvmArgs = ['-Xms32m', '--add-opens=java.base/java.lang=ALL-UNNAMED', '--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED']
}
"""
    and:
    result = executeGradle("--rerun-tasks", GATLING_RUN_TASK_NAME)
    then:
    with(new GatlingDebug(result)) {
      heap.min == 32 * 1024 * 1024
    }
  }

  def "should configure jvmArgs from extension"() {
    when: "default jvmArgs from gatling extension"
    BuildResult result = executeGradle(GATLING_RUN_TASK_NAME)
    then:
    with(new GatlingDebug(result)) {
      // remove -server of the comparison because gradle removes it?!
      jvmArgs.sort() == GatlingConstants.DEFAULT_JVM_OPTIONS_GATLING.findAll { it != "-server" }.sort()
    }

    when: "override via gatling extension"
    buildFile << """
gatling {
    jvmArgs = ['-Xms32m', '-XX:+UseG1GC', '--add-opens=java.base/java.lang=ALL-UNNAMED', '--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED']
}
"""
    and:
    result = executeGradle("--rerun-tasks", GATLING_RUN_TASK_NAME)
    then:
    with(new GatlingDebug(result)) {
      jvmArgs.sort() == [
        '-Xms32m',
        '-XX:+UseG1GC',
        '--add-opens=java.base/java.lang=ALL-UNNAMED',
        '--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED'
      ].sort()
    }
  }

  def "should override jvmArgs from gatlingRun properties"() {
    when: "override via gatlingRun"
    buildFile << """
gatling {
    jvmArgs = ['-Xms32m', '-XX:+AggressiveOpts']
}
gatlingRun {
    jvmArgs = ['-Xms128m', '-XX:+UseG1GC', '--add-opens=java.base/java.lang=ALL-UNNAMED', '--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED']
}
"""
    and:
    def result = executeGradle(GATLING_RUN_TASK_NAME)
    then:
    with(new GatlingDebug(result)) {
      jvmArgs.sort() == [
        '-Xms128m',
        '-XX:+UseG1GC',
        '--add-opens=java.base/java.lang=ALL-UNNAMED',
        '--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED'
      ].sort()
    }
  }

  def "should configure system properties from extension"() {
    when: "defaults from extension"
    def result = executeGradle(GATLING_RUN_TASK_NAME)
    then:
    with(new GatlingDebug(result)) {
      systemProperties.keySet().intersect(GatlingPluginExtension.DEFAULT_SYSTEM_PROPS.keySet()).size() == GatlingPluginExtension.DEFAULT_SYSTEM_PROPS.size()
    }

    when: "override via extension"
    buildFile << """
gatling {
    systemProperties = ['gradle_gatling_1': 'aaa', 'gradle_gatling_2' : 'bbb']
}
"""
    and:
    result = executeGradle("--rerun-tasks", GATLING_RUN_TASK_NAME)
    then:
    with(new GatlingDebug(result)) {
      systemProperties.keySet().findAll { it.startsWith("gradle_gatling_") } == ['gradle_gatling_1', 'gradle_gatling_2'] as Set
      systemProperties["gradle_gatling_1"] == "aaa"
      systemProperties["gradle_gatling_2"] == "bbb"

      systemProperties.keySet().intersect(GatlingPluginExtension.DEFAULT_SYSTEM_PROPS.keySet()).size() == 0
    }
  }

  def "should extend system properties from extension"() {
  }

  def "should extend system properties from gatlingRun properties"() {
    given: "override via gatlingRun"
    buildFile << """
gatling {
    systemProperties = ['gradle_gatling_1': 'aaa', 'gradle_gatling_2' : 'bbb']
}
gatlingRun {
    systemProperties = ['gradle_gatling_2' : 'qwerty', 'gradle_gatling_3' : 'ccc']
}
"""
    when:
    def result = executeGradle(GATLING_RUN_TASK_NAME)
    then:
    with(new GatlingDebug(result)) {
      systemProperties.keySet().findAll { it.startsWith("gradle_gatling_") } == ['gradle_gatling_2', 'gradle_gatling_3'] as Set
      systemProperties["gradle_gatling_2"] == "qwerty"
    }
  }

  def "should set additional env vars from extension"() {
    given: "set via extension"
    buildFile << """
gatling {
    environment['env1'] = 'env1_value'
}
"""
    when:
    def result = executeGradle(GATLING_RUN_TASK_NAME)
    then:
    with(new GatlingDebug(result)) {
      env["env1"] == "env1_value"
    }
  }

  def "should override gatling env vars with gatlingRun"() {
    given: "override via gatlingRun"
    buildFile << """
gatling {
    environment = ['env1': 'aaa', 'env2': 'bbb']
}
gatlingRun {
    environment = ['env2': 'ddd', 'env3': 'ccc']
}
"""
    when:
    def result = executeGradle(GATLING_RUN_TASK_NAME)
    then:
    with(new GatlingDebug(result)) {
      env["env1"] == "aaa"
      env["env2"] == "ddd"
      env["env3"] == "ccc"
    }
  }

  def "should pass env vars upstream"() {
    given:
    environmentVariables.set("GRADLE_GATLING_ENV_UPSTREAM", "env_upstream_value")
    when:
    def result = executeGradle(GATLING_RUN_TASK_NAME)
    then:
    with(new GatlingDebug(result)) {
      env["GRADLE_GATLING_ENV_UPSTREAM"] == "env_upstream_value"
    }
  }

  def "should pass system properties upstream"() {
    given:
    System.setProperty("gradle_gatling_sys_upstream", "sys_upstream_value")
    when:
    def result = executeGradle(GATLING_RUN_TASK_NAME)
    then:
    with(new GatlingDebug(result)) {
      systemProperties["gradle_gatling_sys_upstream"] == "sys_upstream_value"
    }
  }
}
