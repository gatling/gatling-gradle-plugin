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
package func;

import static io.gatling.gradle.GatlingPlugin.ENTERPRISE_PACKAGE_TASK_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.testkit.runner.TaskOutcome.FROM_CACHE;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE;

import helper.GatlingFuncTestBase;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.zip.ZipFile;
import org.apache.commons.io.FileUtils;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class WhenGroovyScalaPackageTest extends GatlingFuncTestBase {

  private static final String STRICT_COMMONS_LANG_2_6 =
      """
      dependencies {
        constraints {
          gatling("commons-lang:commons-lang") {
            version {
              strictly("2.6")
            }
          }
        }
      }
      """;

  @TempDir File secondProjectDir;

  @BeforeEach
  void setUp() {
    prepareGroovyTestWithScala("/gradle-layout");
  }

  private BuildResult build(String gradleVersion, List<String> gradleArgs) {
    return build(gradleVersion, gradleArgs, runner -> {});
  }

  private BuildResult build(
      String gradleVersion, List<String> gradleArgs, Consumer<GradleRunner> configuration) {
    GradleRunner runner =
        createRunner(gradleArgs.toArray(new String[0])).withGradleVersion(gradleVersion);
    configuration.accept(runner);
    return runner.build();
  }

  @ParameterizedTest(name = "Gradle {0}")
  @MethodSource("supportedGradleVersions")
  void shouldSuccessfullyCreateAPackage(String gradleVersion) throws IOException {
    BuildResult result = build(gradleVersion, List.of(ENTERPRISE_PACKAGE_TASK_NAME));

    assertThat(result.task(":" + ENTERPRISE_PACKAGE_TASK_NAME).getOutcome()).isEqualTo(SUCCESS);
    assertThat(result.task(":gatlingClasses").getOutcome()).isEqualTo(SUCCESS);
    File artifact = new File(buildDir, "libs/" + projectDir.getName() + "-tests.jar");
    assertThat(artifact).isFile();
    try (ZipFile archive = new ZipFile(artifact)) {
      for (String entry :
          List.of(
              "example/BasicSimulation.class",
              "example/MainUtils.class",
              "example/TestUtils.class",
              "search.csv")) {
        assertThat(archive.getEntry(entry)).as(entry).isNotNull();
      }
    }
  }

  @ParameterizedTest(name = "Gradle {0}")
  @MethodSource("supportedGradleVersions")
  void shouldSuccessfullyCreateAPackageWhenPackagePathEnvVarIsSet(String gradleVersion) {
    File packageFile = new File(projectDir, "package.jar");

    // not in-process: TestKit ignores withEnvironment in debug mode
    BuildResult result =
        build(
            gradleVersion,
            List.of(ENTERPRISE_PACKAGE_TASK_NAME),
            runner ->
                runner
                    .withDebug(false)
                    .withEnvironment(
                        Map.of(
                            "GATLING_ENTERPRISE_BUILDER_PACKAGE_PATH",
                            packageFile.getAbsolutePath())));

    assertThat(result.task(":" + ENTERPRISE_PACKAGE_TASK_NAME).getOutcome()).isEqualTo(SUCCESS);
    assertThat(packageFile).exists();
  }

  @ParameterizedTest(name = "Gradle {0}")
  @MethodSource("supportedGradleVersions")
  void builtPackageCanBeUpToDate(String gradleVersion) {
    build(gradleVersion, List.of(ENTERPRISE_PACKAGE_TASK_NAME));

    BuildResult result = build(gradleVersion, List.of(ENTERPRISE_PACKAGE_TASK_NAME));

    assertThat(result.task(":" + ENTERPRISE_PACKAGE_TASK_NAME).getOutcome()).isEqualTo(UP_TO_DATE);
  }

  @ParameterizedTest(name = "Gradle {0}")
  @MethodSource("supportedGradleVersions")
  void packageTaskIsCacheable(String gradleVersion) {
    build(gradleVersion, List.of("--build-cache", ENTERPRISE_PACKAGE_TASK_NAME));

    BuildResult result =
        build(gradleVersion, List.of("--build-cache", "clean", ENTERPRISE_PACKAGE_TASK_NAME));

    assertThat(result.task(":" + ENTERPRISE_PACKAGE_TASK_NAME).getOutcome()).isEqualTo(FROM_CACHE);
  }

  @ParameterizedTest(name = "Gradle {0}")
  @MethodSource("supportedGradleVersions")
  void packageTaskDoesNotTrackAbsolutePathsAsInput(String gradleVersion) throws IOException {
    // artifact id is configured, so it's stable across locations
    appendToFile(new File(projectDir, "settings.gradle"), "rootProject.name = 'my-artifact'\n");
    // the build is copied to a different location
    FileUtils.copyDirectory(projectDir, secondProjectDir);
    build(gradleVersion, List.of("--build-cache", ENTERPRISE_PACKAGE_TASK_NAME));

    BuildResult result =
        build(
            gradleVersion,
            List.of("--build-cache", ENTERPRISE_PACKAGE_TASK_NAME),
            runner -> runner.withProjectDir(secondProjectDir));

    assertThat(result.task(":" + ENTERPRISE_PACKAGE_TASK_NAME).getOutcome()).isEqualTo(FROM_CACHE);
  }

  @ParameterizedTest(name = "Gradle {0}")
  @MethodSource("supportedGradleVersions")
  void packageTaskShouldNotBeUpToDateWhenDependenciesChange(String gradleVersion) {
    build(gradleVersion, List.of(ENTERPRISE_PACKAGE_TASK_NAME));
    appendToBuildFile(STRICT_COMMONS_LANG_2_6);

    BuildResult result = build(gradleVersion, List.of(ENTERPRISE_PACKAGE_TASK_NAME));

    assertThat(result.task(":" + ENTERPRISE_PACKAGE_TASK_NAME).getOutcome()).isEqualTo(SUCCESS);
  }

  @ParameterizedTest(name = "Gradle {0}")
  @MethodSource("supportedGradleVersions")
  void packageTaskShouldNotBeFromCacheWhenDependenciesChange(String gradleVersion) {
    build(gradleVersion, List.of("--build-cache", ENTERPRISE_PACKAGE_TASK_NAME));
    appendToBuildFile(STRICT_COMMONS_LANG_2_6);

    BuildResult result =
        build(gradleVersion, List.of("--build-cache", "clean", ENTERPRISE_PACKAGE_TASK_NAME));

    assertThat(result.task(":" + ENTERPRISE_PACKAGE_TASK_NAME).getOutcome()).isEqualTo(SUCCESS);
  }

  @ParameterizedTest(name = "Gradle {0}")
  @MethodSource("supportedGradleVersions")
  void packageTaskShouldComputeDependenciesLazily(String gradleVersion) {
    // a dependency is added after the task is created
    appendToBuildFile(
        """
        tasks.gatlingEnterprisePackage {
          // force creation of task
        }
        afterEvaluate {
          dependencies {
            gatlingImplementation 'commons-io:commons-io:2.20.0'
          }
        }
        """);

    BuildResult result = build(gradleVersion, List.of("clean", ENTERPRISE_PACKAGE_TASK_NAME));

    assertThat(result.task(":" + ENTERPRISE_PACKAGE_TASK_NAME).getOutcome()).isEqualTo(SUCCESS);
  }

  @ParameterizedTest(name = "Gradle {0}")
  @MethodSource("supportedGradleVersionsForScalaConfigurationCache")
  void packageTaskIsCompatibleWithTheConfigurationCache(String gradleVersion) {
    List<String> args = List.of("clean", ENTERPRISE_PACKAGE_TASK_NAME, "--configuration-cache");
    build(gradleVersion, args);

    BuildResult result = build(gradleVersion, args);

    assertThat(result.task(":" + ENTERPRISE_PACKAGE_TASK_NAME).getOutcome()).isEqualTo(SUCCESS);
    assertThat(result.getOutput()).contains("Reusing configuration cache");
  }
}
