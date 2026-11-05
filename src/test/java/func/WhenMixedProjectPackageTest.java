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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class WhenMixedProjectPackageTest extends GatlingFuncTestBase {

  private static final String[] PACKAGE_ARGS = {
    ENTERPRISE_PACKAGE_TASK_NAME, "--build-cache", "--configuration-cache"
  };

  @BeforeEach
  void setUp() {
    prepareGroovyTestWithJava("/gradle-layout");
    writeFile(
        new File(projectDir, "settings.gradle"),
        """
        rootProject.name = 'mixed-project'
        include 'plain', 'child'
        buildCache {
          local {
            directory = file('build-cache')
          }
        }
        """);
    appendToBuildFile(
        """
        // Realize the task before either child build script is evaluated.
        tasks.named('gatlingEnterprisePackage').get()
        """);
    writeFile(
        new File(projectDir, "plain/build.gradle"),
        """
        plugins {
          id 'base'
        }
        """);
    writeFile(
        new File(projectDir, "child/build.gradle"),
        """
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
          sourceSets.create('late')
        }
        """);
    writeChildFile("src/main/java/child/Main.java", "package child; public class Main {}");
    writeChildFile("src/test/java/child/Test.java", "package child; public class Test {}");
    writeChildFile(
        "src/additional/java/child/Additional.java", "package child; public class Additional {}");
    writeChildFile("src/late/java/child/Late.java", "package child; public class Late {}");
    writeChildFile("src/additional/resources/child-resource.txt", "child resource");
    writeChildFile("generated-input/generated-resource.txt", "generated resource");
  }

  private void writeChildFile(String path, String content) {
    writeFile(new File(projectDir, "child/" + path), content + "\n");
  }

  private File packageFile() {
    return new File(buildDir, "libs/mixed-project-tests.jar");
  }

  private void assertPackageContains(String... paths) throws IOException {
    try (ZipFile archive = new ZipFile(packageFile())) {
      for (String path : paths) {
        assertThat(archive.getEntry(path)).as(path).isNotNull();
      }
    }
  }

  private String packageResource(String path) throws IOException {
    try (ZipFile archive = new ZipFile(packageFile())) {
      ZipEntry entry = archive.getEntry(path);
      assertThat(entry).as(path).isNotNull();
      try (InputStream is = archive.getInputStream(entry)) {
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
      }
    }
  }

  private BuildResult build(String gradleVersion, String... extraArgs) {
    List<String> args = new ArrayList<>(List.of(extraArgs));
    args.addAll(List.of(PACKAGE_ARGS));
    return createRunner(args.toArray(new String[0])).withGradleVersion(gradleVersion).build();
  }

  @ParameterizedTest(name = "Gradle {0}")
  @MethodSource("supportedGradleVersions")
  void earlyPackageTaskRealizationAllowsNonJavaChildrenAndHelp(String gradleVersion) {
    BuildResult result = createRunner("help").withGradleVersion(gradleVersion).build();

    assertThat(result.task(":help").getOutcome()).isEqualTo(SUCCESS);
    assertThat(result.task(":" + ENTERPRISE_PACKAGE_TASK_NAME)).isNull();
  }

  @ParameterizedTest(name = "Gradle {0}")
  @MethodSource("supportedGradleVersions")
  void lateChildOutputsRetainProducersAndCacheTracking(String gradleVersion) throws IOException {
    // packaging without explicitly building child tasks
    BuildResult result = build(gradleVersion);
    assertThat(result.task(":" + ENTERPRISE_PACKAGE_TASK_NAME).getOutcome()).isEqualTo(SUCCESS);
    for (String task :
        List.of(
            ":child:compileJava",
            ":child:compileTestJava",
            ":child:compileAdditionalJava",
            ":child:compileLateJava",
            ":child:processAdditionalResources",
            ":child:generateResources")) {
      assertThat(result.task(task).getOutcome()).as(task).isEqualTo(SUCCESS);
    }
    assertPackageContains(
        "child/Main.class",
        "child/Test.class",
        "child/Additional.class",
        "child/Late.class",
        "child-resource.txt",
        "generated-resource.txt",
        "example/BasicSimulation.class");

    // packaging again
    BuildResult repeated = build(gradleVersion);
    assertThat(repeated.task(":" + ENTERPRISE_PACKAGE_TASK_NAME).getOutcome())
        .isEqualTo(UP_TO_DATE);
    assertThat(repeated.getOutput()).contains("Reusing configuration cache");

    // removing all outputs and restoring from the build cache
    BuildResult restored = build(gradleVersion, "clean", ":child:clean");
    assertThat(restored.task(":" + ENTERPRISE_PACKAGE_TASK_NAME).getOutcome())
        .isEqualTo(FROM_CACHE);
    assertPackageContains("child/Additional.class", "generated-resource.txt");

    // changing a child class without changing build configuration
    writeChildFile(
        "src/additional/java/child/Additional.java",
        "package child; public class Additional { public int value() { return 42; } }");
    BuildResult changedClass = build(gradleVersion);
    assertThat(changedClass.task(":" + ENTERPRISE_PACKAGE_TASK_NAME).getOutcome())
        .isEqualTo(SUCCESS);
    assertThat(changedClass.getOutput()).contains("Reusing configuration cache");

    // changing a generated child resource independently
    writeChildFile("generated-input/generated-resource.txt", "changed resource");
    BuildResult changedResource = build(gradleVersion);
    assertThat(changedResource.task(":" + ENTERPRISE_PACKAGE_TASK_NAME).getOutcome())
        .isEqualTo(SUCCESS);
    assertThat(changedResource.getOutput()).contains("Reusing configuration cache");
    assertThat(packageResource("generated-resource.txt")).isEqualTo("changed resource\n");
  }

  @ParameterizedTest(name = "Gradle {0}")
  @MethodSource("supportedGradleVersions")
  void lateKotlinChildOutputsRetainTheirCompilerDependency(String gradleVersion)
      throws IOException {
    // the Kotlin plugin is declared on the root project, so its build services can be restored
    // from the configuration cache
    writeFile(
        buildFile,
        readFile(buildFile)
            .replace(
                "id 'io.gatling.gradle'",
                "id 'io.gatling.gradle'\n  id 'org.jetbrains.kotlin.jvm' version '"
                    + KOTLIN_PLUGIN_VERSION
                    + "' apply false"));
    writeFile(
        new File(projectDir, "child/build.gradle"),
        """
        plugins {
          id 'org.jetbrains.kotlin.jvm'
        }
        repositories {
          mavenCentral()
        }
        sourceSets {
          additional
        }
        """);
    writeChildFile(
        "src/additional/kotlin/child/KotlinOutput.kt", "package child; class KotlinOutput");

    // packaging twice
    String[] args = {"clean", ENTERPRISE_PACKAGE_TASK_NAME, "--configuration-cache"};
    createRunner(args).withGradleVersion(gradleVersion).build();
    BuildResult result = createRunner(args).withGradleVersion(gradleVersion).build();

    assertThat(result.getOutput()).contains("Reusing configuration cache");
    assertThat(result.task(":child:compileAdditionalKotlin").getOutcome()).isEqualTo(SUCCESS);
    assertPackageContains("child/KotlinOutput.class", "child/Additional.class");
  }
}
