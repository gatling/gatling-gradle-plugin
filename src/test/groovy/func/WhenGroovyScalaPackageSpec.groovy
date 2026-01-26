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
import static org.apache.commons.io.FileUtils.copyDirectory
import static org.gradle.testkit.runner.TaskOutcome.FROM_CACHE
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE

import helper.GatlingFuncSpec
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import spock.lang.PendingFeature
import spock.lang.TempDir
import spock.lang.Unroll

class WhenGroovyScalaPackageSpec extends GatlingFuncSpec {

  @TempDir
  File secondProjectDir

  def setup() {
    prepareGroovyTestWithScala("/gradle-layout")
  }

  @Unroll
  def "should successfully create a package for gradle version #gradleVersion"() {
    when:
    def result = build(gradleVersion, [ENTERPRISE_PACKAGE_TASK_NAME])

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

  @Unroll
  def "should successfully create a package when GATLING_ENTERPRISE_BUILDER_PACKAGE_PATH env var is set for gradle version #gradleVersion"() {
    when: "building the package"
    def packageFile = new File(projectDir.root, 'package.jar')
    def result = build(gradleVersion, [ENTERPRISE_PACKAGE_TASK_NAME]) {
      withDebug(false)
      withEnvironment([GATLING_ENTERPRISE_BUILDER_PACKAGE_PATH: packageFile.absolutePath])
    }

    then: "artifact was created"
    result.task(":$ENTERPRISE_PACKAGE_TASK_NAME").outcome == SUCCESS
    packageFile.exists()

    where:
    gradleVersion << SUPPORTED_GRADLE_VERSIONS
  }

  @Unroll
  def "built package can be up-to-date for gradle version #gradleVersion"() {
    given: "the package has been built"
    build(gradleVersion, [ENTERPRISE_PACKAGE_TASK_NAME])

    when: "building the package again"
    def result = build(gradleVersion, [ENTERPRISE_PACKAGE_TASK_NAME])

    then: "the package task is up-to-date"
    result.task(":$ENTERPRISE_PACKAGE_TASK_NAME").outcome == UP_TO_DATE

    where:
    gradleVersion << SUPPORTED_GRADLE_VERSIONS
  }

  @Unroll
  def "package task is cacheable for gradle version #gradleVersion"() {
    given: "the package has been built"
    build(gradleVersion, ['--build-cache', ENTERPRISE_PACKAGE_TASK_NAME])

    when: "building the package again with clean"
    def result = build(gradleVersion, ['--build-cache', 'clean', ENTERPRISE_PACKAGE_TASK_NAME])

    then: "the package task is from-cache"
    result.task(":$ENTERPRISE_PACKAGE_TASK_NAME").outcome == FROM_CACHE

    where:
    gradleVersion << SUPPORTED_GRADLE_VERSIONS
  }

  @Unroll
  def "package task does not track absolute paths as input gradle version #gradleVersion"() {
    given: "artifact id is configured (so it's stable across locations)"
    new File(projectDir.root, 'settings.gradle') << '''
      rootProject.name = 'my-artifact'
    '''
    and: "the build is copied to a different location"
    copyDirectory(projectDir.root, secondProjectDir)
    and: "the package has been built"
    build(gradleVersion, ['--build-cache', ENTERPRISE_PACKAGE_TASK_NAME])

    when: "building the package again with project in different location"
    def result = build(gradleVersion, ['--build-cache', ENTERPRISE_PACKAGE_TASK_NAME]) {
      withProjectDir(secondProjectDir)
    }

    then: "the package task is from-cache"
    result.task(":$ENTERPRISE_PACKAGE_TASK_NAME").outcome == FROM_CACHE

    where:
    gradleVersion << SUPPORTED_GRADLE_VERSIONS
  }

  private BuildResult build(String gradleVersion, List<String> gradleArgs, @DelegatesTo(GradleRunner) Closure<?> configuration = {}) {
    def gradleRunner = createRunner(*gradleArgs)
            .withGradleVersion(gradleVersion)
    configuration.delegate = gradleRunner
    configuration()
    return gradleRunner.build()
  }
}
