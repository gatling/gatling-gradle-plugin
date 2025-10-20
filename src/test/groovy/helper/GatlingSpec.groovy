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

import static org.apache.commons.io.FileUtils.copyDirectory

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

abstract class GatlingSpec extends Specification {
  @Rule
  public final TemporaryFolder projectDir = new TemporaryFolder()

  File srcDir

  File buildDir

  File buildFile

  def createBuildFolder(String fixtureDir, SimulationLanguage simulationLanguage) {
    String suffix = simulationLanguage.name().toLowerCase(Locale.ROOT)
    if (fixtureDir) {
      copyDirectory(new File(this.class.getResource("$fixtureDir-$suffix").file), projectDir.root)
    }
    srcDir = new File(projectDir.root, "src/gatling/$suffix")
    buildDir = new File(projectDir.root, "build")
  }

  def generateBuildScript(GradleScriptingLanguage gradleScriptingLanguage, SimulationLanguage simulationLanguage) {
    switch (gradleScriptingLanguage) {
      case GradleScriptingLanguage.GROOVY:
        buildFile = projectDir.newFile("build.gradle")

        String languagePlugin
        switch (simulationLanguage) {
          case SimulationLanguage.JAVA:
            languagePlugin = "java"
            break
          case SimulationLanguage.SCALA:
            languagePlugin = "scala"
            break
          case SimulationLanguage.KOTLIN:
            languagePlugin = "org.jetbrains.kotlin.jvmkotlin"
        }

        buildFile.text = """
plugins {
  id '$languagePlugin'
  id 'io.gatling.gradle'
}

repositories {
    mavenCentral()
}

dependencies {
  gatling group: 'commons-lang', name: 'commons-lang', version: '2.6'
  gatling group: 'org.json4s', name: 'json4s-jackson_2.13', version: '3.6.10'
}
"""
        break

      case GradleScriptingLanguage.KOTLIN:
        buildFile = projectDir.newFile("build.gradle.kts")

        String languagePlugin
        switch (simulationLanguage) {
          case SimulationLanguage.JAVA:
            languagePlugin = "id(\"java\")"
            break
          case SimulationLanguage.SCALA:
            languagePlugin = "id(\"scala\")"
            break
          case SimulationLanguage.KOTLIN:
            languagePlugin = """
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.allopen") version "1.9.22"
"""
        }

        buildFile.text = """
plugins {
  $languagePlugin
  id("io.gatling.gradle")
}

repositories {
  mavenCentral()
}

dependencies {
  gatling("commons-lang:commons-lang:2.6")
  gatling("org.json4s:json4s-jackson_2.13:3.6.10")
}
"""
    }
  }
}
