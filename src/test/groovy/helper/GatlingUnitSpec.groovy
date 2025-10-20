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

import io.gatling.gradle.GatlingPluginExtension
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

abstract class GatlingUnitSpec extends GatlingSpec {
  Project project

  GatlingPluginExtension gatlingExt

  def setup() {
    createBuildFolder("/gradle-layout", SimulationLanguage.SCALA)

    project = ProjectBuilder.builder().withProjectDir(projectDir.root).build()
    project.pluginManager.apply 'scala'
    project.pluginManager.apply 'io.gatling.gradle'
    project.repositories { mavenCentral(name: "gatlingMavenCentral") }

    gatlingExt = project.extensions.getByType(GatlingPluginExtension)
  }
}
