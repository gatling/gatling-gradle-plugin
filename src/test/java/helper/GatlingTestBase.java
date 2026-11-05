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

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.io.TempDir;

public abstract class GatlingTestBase {
  protected static final String KOTLIN_PLUGIN_VERSION = "2.4.21";

  @TempDir protected File projectDir;

  protected File srcDir;

  protected File buildDir;

  protected File buildFile;

  protected void createBuildFolder(String fixtureDir, SimulationLanguage simulationLanguage) {
    String suffix = simulationLanguage.name().toLowerCase(Locale.ROOT);
    if (fixtureDir != null) {
      URL fixture = getClass().getResource(fixtureDir + "-" + suffix);
      try {
        FileUtils.copyDirectory(new File(fixture.toURI()), projectDir);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      } catch (URISyntaxException e) {
        throw new IllegalStateException(e);
      }
    }
    srcDir = new File(projectDir, "src/gatling/" + suffix);
    buildDir = new File(projectDir, "build");
  }

  protected void generateBuildScript(
      GradleScriptingLanguage gradleScriptingLanguage, SimulationLanguage simulationLanguage) {
    switch (gradleScriptingLanguage) {
      case GROOVY:
        buildFile = new File(projectDir, "build.gradle");
        writeFile(
            buildFile,
            """
            plugins {
              id '%s'
              id 'io.gatling.gradle'
            }

            repositories {
              mavenCentral()
            }

            dependencies {
              gatling 'commons-lang:commons-lang:2.5'
              gatling 'org.json4s:json4s-jackson_2.13:3.6.10'
            }
            """
                .formatted(groovyLanguagePlugin(simulationLanguage)));
        break;

      case KOTLIN:
        buildFile = new File(projectDir, "build.gradle.kts");
        writeFile(
            buildFile,
            """
            plugins {
              %s
              id("io.gatling.gradle")
            }

            repositories {
              mavenCentral()
            }

            dependencies {
              gatling("commons-lang:commons-lang:2.5")
              gatling("org.json4s:json4s-jackson_2.13:3.6.10")
            }
            """
                .formatted(kotlinLanguagePlugin(simulationLanguage)));
        break;

      default:
        throw new IllegalArgumentException(
            "Unsupported scripting language: " + gradleScriptingLanguage);
    }
  }

  private static String groovyLanguagePlugin(SimulationLanguage simulationLanguage) {
    switch (simulationLanguage) {
      case JAVA:
        return "java";
      case SCALA:
        return "scala";
      default:
        throw new IllegalArgumentException(
            "Unsupported simulation language for Groovy DSL: " + simulationLanguage);
    }
  }

  private static String kotlinLanguagePlugin(SimulationLanguage simulationLanguage) {
    switch (simulationLanguage) {
      case JAVA:
        return "id(\"java\")";
      case SCALA:
        return "id(\"scala\")";
      case KOTLIN:
        return """
        kotlin("jvm") version "%1$s"
          kotlin("plugin.allopen") version "%1$s"
        """
            .formatted(KOTLIN_PLUGIN_VERSION);
      default:
        throw new IllegalArgumentException(
            "Unsupported simulation language: " + simulationLanguage);
    }
  }

  protected static void writeFile(File file, String content) {
    try {
      file.getParentFile().mkdirs();
      Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  protected static void appendToFile(File file, String content) {
    try {
      file.getParentFile().mkdirs();
      Files.writeString(
          file.toPath(),
          content,
          StandardCharsets.UTF_8,
          StandardOpenOption.CREATE,
          StandardOpenOption.APPEND);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  protected static String readFile(File file) {
    try {
      return Files.readString(file.toPath(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  protected void appendToBuildFile(String content) {
    appendToFile(buildFile, content);
  }
}
