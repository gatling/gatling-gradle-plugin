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
import static org.gradle.testkit.runner.TaskOutcome.FROM_CACHE
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE

import helper.GatlingFuncSpec
import java.util.zip.ZipFile
import spock.lang.Unroll

class WhenMixedProjectPackageSpec extends GatlingFuncSpec {

  def setup() {
    prepareGroovyTestWithJava("/gradle-layout")
    projectDir.newFile("settings.gradle") << """
      rootProject.name = 'mixed-project'
      include 'plain', 'child'
      buildCache {
        local {
          directory = file('build-cache')
        }
      }
    """
    projectDir.newFolder("plain")
    projectDir.newFolder("child")
    buildFile << """
      // Realize the task before either child build script is evaluated.
      tasks.named('$ENTERPRISE_PACKAGE_TASK_NAME').get()
    """
    new File(projectDir.root, "plain/build.gradle").text = """
      plugins {
        id 'base'
      }
    """
    new File(projectDir.root, "child/build.gradle").text = """
      plugins {
        id 'java'
      }
      sourceSets {
        additional
      }
      def generateResources = tasks.register('generateResources', Copy) {
        from('generated-input')
        into(layout.buildDirectory.dir('generated-resources'))
      }
      sourceSets.additional.output.dir(generateResources)
      afterEvaluate {
        sourceSets.additional.java.destinationDirectory.set(
            layout.buildDirectory.dir('late-classes'))
      }
    """
    writeChildFile("src/main/java/child/Main.java", "package child; public class Main {}")
    writeChildFile("src/test/java/child/Test.java", "package child; public class Test {}")
    writeChildFile("src/additional/java/child/Additional.java", "package child; public class Additional {}")
    writeChildFile("src/additional/resources/child-resource.txt", "child resource")
    writeChildFile("generated-input/generated-resource.txt", "generated resource")
  }

  @Unroll
  def "early package task realization allows non-Java children and help #gradleVersion"() {
    when:
    def result = createRunner('help').withGradleVersion(gradleVersion).build()

    then:
    result.task(':help').outcome == SUCCESS
    result.task(":$ENTERPRISE_PACKAGE_TASK_NAME") == null

    where:
    gradleVersion << SUPPORTED_GRADLE_VERSIONS
  }

  @Unroll
  def "late child outputs retain producers and cache tracking #gradleVersion"() {
    given:
    def cacheArgs = gradleVersion == '8.4' ? [] : ['--configuration-cache']
    def args = [ENTERPRISE_PACKAGE_TASK_NAME, '--build-cache'] + cacheArgs

    when: "packaging without explicitly building child tasks"
    def result = createRunner(*args).withGradleVersion(gradleVersion).build()

    then:
    result.task(":$ENTERPRISE_PACKAGE_TASK_NAME").outcome == SUCCESS
    result.task(':child:compileJava').outcome == SUCCESS
    result.task(':child:compileTestJava').outcome == SUCCESS
    result.task(':child:compileAdditionalJava').outcome == SUCCESS
    result.task(':child:processAdditionalResources').outcome == SUCCESS
    result.task(':child:generateResources').outcome == SUCCESS
    packageContains('child/Main.class', 'child/Test.class', 'child/Additional.class',
            'child-resource.txt', 'generated-resource.txt', 'example/BasicSimulation.class')

    when: "packaging again"
    def repeated = createRunner(*args).withGradleVersion(gradleVersion).build()

    then:
    repeated.task(":$ENTERPRISE_PACKAGE_TASK_NAME").outcome == UP_TO_DATE
    cacheArgs.empty || repeated.output.contains('Reusing configuration cache')

    when: "removing all outputs and restoring from the build cache"
    def restored = createRunner(*(['clean', ':child:clean'] + args)).withGradleVersion(gradleVersion).build()

    then:
    restored.task(":$ENTERPRISE_PACKAGE_TASK_NAME").outcome == FROM_CACHE
    packageContains('child/Additional.class', 'generated-resource.txt')

    when: "changing a child class without changing build configuration"
    writeChildFile("src/additional/java/child/Additional.java",
            "package child; public class Additional { public int value() { return 42; } }")
    def changedClass = createRunner(*args).withGradleVersion(gradleVersion).build()

    then:
    changedClass.task(":$ENTERPRISE_PACKAGE_TASK_NAME").outcome == SUCCESS
    cacheArgs.empty || changedClass.output.contains('Reusing configuration cache')

    when: "changing a generated child resource independently"
    writeChildFile("generated-input/generated-resource.txt", "changed resource")
    def changedResource = createRunner(*args).withGradleVersion(gradleVersion).build()

    then:
    changedResource.task(":$ENTERPRISE_PACKAGE_TASK_NAME").outcome == SUCCESS
    cacheArgs.empty || changedResource.output.contains('Reusing configuration cache')
    packageResource('generated-resource.txt') == "changed resource\n"

    where:
    gradleVersion << SUPPORTED_GRADLE_VERSIONS
  }

  def "late Kotlin child outputs retain their compiler dependency"() {
    given:
    new File(projectDir.root, "child/build.gradle").text = """
      plugins {
        id 'org.jetbrains.kotlin.jvm' version '1.9.22'
      }
      repositories {
        mavenCentral()
      }
      sourceSets {
        additional
      }
    """
    writeChildFile("src/additional/kotlin/child/KotlinOutput.kt", "package child; class KotlinOutput")

    when:
    def result = createRunner(ENTERPRISE_PACKAGE_TASK_NAME, '--configuration-cache')
            .withGradleVersion('8.14.5').build()

    then:
    result.task(':child:compileAdditionalKotlin').outcome == SUCCESS
    packageContains('child/KotlinOutput.class', 'child/Additional.class')
  }

  private void writeChildFile(String path, String content) {
    def file = new File(projectDir.root, "child/$path")
    file.parentFile.mkdirs()
    file.text = content + '\n'
  }

  private boolean packageContains(String... paths) {
    new ZipFile(new File(buildDir, 'libs/mixed-project-tests.jar')).withCloseable { archive ->
      paths.every { archive.getEntry(it) != null }
    }
  }

  private String packageResource(String path) {
    new ZipFile(new File(buildDir, 'libs/mixed-project-tests.jar')).withCloseable { archive ->
      archive.getInputStream(archive.getEntry(path)).withCloseable { it.getText('UTF-8') }
    }
  }
}
