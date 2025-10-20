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
package unit

import static io.gatling.gradle.GatlingPlugin.GATLING_RUN_TASK_NAME

import helper.GatlingUnitSpec
import io.gatling.gradle.GatlingPluginExtension
import io.gatling.gradle.GatlingRunTask
import io.gatling.plugin.GatlingConstants
import org.gradle.api.tasks.TaskProvider
import org.gradle.language.jvm.tasks.ProcessResources

class GatlingPluginTest extends GatlingUnitSpec {

  def "should create gatling configurations"() {
    expect:
    [
      'gatling',
      'gatlingCompileOnly',
      'gatlingImplementation',
      'gatlingRuntimeOnly'
    ].every {
      project.configurations.getByName(it) != null
    }
  }

  def "should create gatling extension for project "() {
    expect:
    with(gatlingExt) {
      it instanceof GatlingPluginExtension
      it.jvmArgs == GatlingConstants.DEFAULT_JVM_OPTIONS_GATLING
      it.systemProperties == DEFAULT_SYSTEM_PROPS
    }
  }

  def "should add gatling dependencies with default version"() {
    when:
    project.evaluate()
    then:
    project.configurations.gatling.allDependencies.find {
      it.name == "gatling-charts-highcharts" && it.version == GatlingPluginExtension.GATLING_VERSION
    }
    project.configurations.gatlingImplementation.allDependencies.find {
      it.name == "scala-library" && it.version == GatlingPluginExtension.SCALA_VERSION
    }
  }

  def "should allow overriding gatling version via extension"() {
    when:
    project.gatling { gatlingVersion = '3.5.1' }
    and:
    project.evaluate()
    then:
    project.configurations.gatling.allDependencies.find {
      it.name == "gatling-charts-highcharts" && it.version == "3.5.1"
    }
  }

  def "should allow overriding scala version via extension"() {
    when:
    project.gatling { scalaVersion = '2.11.3' }
    and:
    project.evaluate()
    then:
    project.configurations.gatlingImplementation.allDependencies.find {
      it.name == "scala-library" && it.version == "2.11.3"
    }
  }

  def "should create gatlingRun task"() {
    expect:
    with(project.tasks.getByName(GATLING_RUN_TASK_NAME)) {
      it instanceof GatlingRunTask
      it.jvmArgs == null
      it.systemProperties == null
      it.dependsOn.size() == 1
      def taskNames = it.dependsOn.collect {TaskProvider element -> return element.name}
      taskNames.contains("gatlingClasses")
    }
  }

  def "should create processGatlingResources task"() {
    expect:
    with(project.tasks.processGatlingResources) {
      it instanceof ProcessResources
    }
  }
}
