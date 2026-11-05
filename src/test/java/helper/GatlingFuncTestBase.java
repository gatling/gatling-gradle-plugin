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
package helper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;

public abstract class GatlingFuncTestBase extends GatlingTestBase {

  protected static final String GATLING_HOST_NAME_SYS_PROP =
      "-Dtest.gatling.hostName=https://api-ecomm.gatling.io";

  protected static final List<String> SUPPORTED_GRADLE_VERSIONS = List.of("8.4", "8.14.5", "9.8.2");

  protected static Stream<String> supportedGradleVersions() {
    return SUPPORTED_GRADLE_VERSIONS.stream();
  }

  /** Gradle 8.4's own ScalaCompile task can't be restored from the configuration cache. */
  protected static Stream<String> supportedGradleVersionsForScalaConfigurationCache() {
    return SUPPORTED_GRADLE_VERSIONS.stream().filter(version -> !version.equals("8.4"));
  }

  protected void prepareGroovyTestWithScala(String fixtureDir) {
    createBuildFolder(fixtureDir, SimulationLanguage.SCALA);
    generateBuildScript(GradleScriptingLanguage.GROOVY, SimulationLanguage.SCALA);
  }

  protected void prepareGroovyTestWithJava(String fixtureDir) {
    createBuildFolder(fixtureDir, SimulationLanguage.JAVA);
    generateBuildScript(GradleScriptingLanguage.GROOVY, SimulationLanguage.JAVA);
  }

  protected void prepareKotlinTestWithKotlin(String fixtureDir) {
    createBuildFolder(fixtureDir, SimulationLanguage.KOTLIN);
    generateBuildScript(GradleScriptingLanguage.KOTLIN, SimulationLanguage.KOTLIN);
  }

  protected void prepareKotlinTestWithScala(String fixtureDir) {
    createBuildFolder(fixtureDir, SimulationLanguage.SCALA);
    generateBuildScript(GradleScriptingLanguage.KOTLIN, SimulationLanguage.SCALA);
  }

  protected GradleRunner createRunner(String... gradleArgs) {
    List<String> arguments = new ArrayList<>(List.of("--stacktrace", GATLING_HOST_NAME_SYS_PROP));
    arguments.addAll(Arrays.asList(gradleArgs));
    return GradleRunner.create()
        .forwardOutput()
        .withProjectDir(projectDir)
        .withArguments(arguments)
        .withPluginClasspath()
        .withDebug(true);
  }

  protected BuildResult executeGradle(String... gradleArgs) {
    return createRunner(gradleArgs).build();
  }

  protected BuildResult executeGradleAndFail(String... gradleArgs) {
    return createRunner(gradleArgs).buildAndFail();
  }
}
