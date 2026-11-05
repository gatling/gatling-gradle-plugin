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
package io.gatling.gradle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.scala.ScalaCompile;

public final class GatlingPlugin implements Plugin<Project> {

  public static final String GATLING_EXTENSION_NAME = "gatling";

  public static final String GATLING_RUN_TASK_NAME = "gatlingRun";

  public static final String GATLING_RECORDER_TASK_NAME = "gatlingRecorder";

  public static final String ENTERPRISE_PACKAGE_TASK_NAME = "gatlingEnterprisePackage";

  public static final String ENTERPRISE_UPLOAD_TASK_NAME = "gatlingEnterpriseUpload";

  public static final String ENTERPRISE_START_TASK_NAME = "gatlingEnterpriseStart";

  public static final String ENTERPRISE_DEPLOY_TASK_NAME = "gatlingEnterpriseDeploy";

  @Override
  public void apply(Project project) {
    createConfiguration(project);

    project
        .getTasks()
        .register(
            GATLING_RECORDER_TASK_NAME,
            GatlingRecorderTask.class,
            task -> {
              task.setDescription("Launch recorder");
              task.setGroup("Gatling");
            });

    project
        .getTasks()
        .register(
            GATLING_RUN_TASK_NAME,
            GatlingRunTask.class,
            task -> {
              task.dependsOn(project.getTasks().named("gatlingClasses"));
              task.setDescription("Execute Gatling simulations locally");
              task.setGroup("Gatling");
            });

    TaskProvider<GatlingEnterprisePackageTask> packageTask = registerEnterprisePackageTask(project);
    project.getTasks().register(ENTERPRISE_UPLOAD_TASK_NAME, GatlingEnterpriseUploadTask.class);
    project
        .getTasks()
        .register(
            ENTERPRISE_DEPLOY_TASK_NAME,
            GatlingEnterpriseDeployTask.class,
            task -> {
              task.getInputs().files(packageTask);
              task.dependsOn(packageTask);
            });
    project
        .getTasks()
        .register(
            ENTERPRISE_START_TASK_NAME,
            GatlingEnterpriseStartTask.class,
            task -> {
              task.getInputs().files(packageTask);
              task.dependsOn(packageTask);
            });
  }

  private static TaskProvider<GatlingEnterprisePackageTask> registerEnterprisePackageTask(
      Project project) {
    TaskProvider<GatlingEnterprisePackageTask> packageTask =
        project
            .getTasks()
            .register(
                ENTERPRISE_PACKAGE_TASK_NAME,
                GatlingEnterprisePackageTask.class,
                task -> {
                  task.dependsOn(project.getTasks().named("gatlingClasses"));
                  task.getArchiveClassifier().set("tests");
                });
    project.allprojects(candidate ->
        candidate
            .getPluginManager()
            .withPlugin(
                "java-base",
                plugin ->
                    candidate
                        .getExtensions()
                        .getByType(JavaPluginExtension.class)
                        .getSourceSets()
                        .all(sourceSet ->
                            packageTask.configure(
                                task -> task.getClassDirectories().from(sourceSet.getOutput())))));
    return packageTask;
  }

  private static void setSrcDir(Object sourceDirectorySet, String srcDir) {
    ((SourceDirectorySet) sourceDirectorySet).setSrcDirs(Collections.singletonList(srcDir));
  }

  private static void createConfiguration(Project project) {
    boolean hasJava = project.getPluginManager().hasPlugin("java");
    boolean hasScala = project.getPluginManager().hasPlugin("scala");
    boolean hasKotlin = project.getPluginManager().hasPlugin("kotlin");

    if (!hasJava && !hasScala && !hasKotlin) {
      throw new UnsupportedOperationException(
          "You must configure the plugin for your language of choice: java, scala or kotlin.");
    }

    project.getExtensions().create(GATLING_EXTENSION_NAME, GatlingPluginExtension.class);

    SourceSet gatlingSourceSet =
        project.getExtensions().getByType(SourceSetContainer.class).maybeCreate("gatling");
    setSrcDir(gatlingSourceSet.getResources(), GatlingPluginExtension.GATLING_RESOURCES_DIR);
    if (hasJava) {
      setSrcDir(gatlingSourceSet.getJava(), GatlingPluginExtension.GATLING_JAVA_SOURCES_DIR);
    }
    if (hasScala) {
      setSrcDir(
          gatlingSourceSet.getExtensions().getByName("scala"),
          GatlingPluginExtension.GATLING_SCALA_SOURCES_DIR);
    }
    if (hasKotlin) {
      setSrcDir(
          gatlingSourceSet.getExtensions().getByName("kotlin"),
          GatlingPluginExtension.GATLING_KOTLIN_SOURCES_DIR);
    }

    Configuration gatlingConfiguration = project.getConfigurations().maybeCreate("gatling");
    project
        .getConfigurations()
        .getByName(gatlingSourceSet.getImplementationConfigurationName())
        .extendsFrom(gatlingConfiguration);

    DependencyHandler dependencies = project.getDependencies();
    dependencies.add(
        gatlingSourceSet.getRuntimeOnlyConfigurationName(), gatlingSourceSet.getOutput());
    if (hasScala) {
      dependencies
          .getConstraints()
          .add(
              "zinc",
              "org.apache.logging.log4j:log4j-core",
              constraint -> {
                constraint.version(version -> version.require("2.17.1"));
                constraint.because("log4shell");
              });

      project
          .getTasks()
          .named("compileGatlingScala", ScalaCompile.class)
          .configure(task ->
              task.getScalaCompileOptions()
                  .setAdditionalParameters(
                      // not List.of: Gradle < 9 can't restore it from the configuration cache
                      new ArrayList<>(
                          Arrays.asList(
                              "-deprecation",
                              "-feature",
                              "-unchecked",
                              "-language:implicitConversions",
                              "-language:postfixOps"))));
    }

    project.afterEvaluate(evaluatedProject -> {
      GatlingPluginExtension evaluatedExt =
          evaluatedProject.getExtensions().getByType(GatlingPluginExtension.class);
      DependencyHandler evaluatedDependencies = evaluatedProject.getDependencies();
      String gatlingImplementation = gatlingSourceSet.getImplementationConfigurationName();

      if (hasScala) {
        evaluatedDependencies.add(
            gatlingImplementation,
            "org.scala-lang:scala-library:" + evaluatedExt.getScalaVersion());
      }
      evaluatedDependencies.add(
          "gatling",
          "io.gatling.highcharts:gatling-charts-highcharts:" + evaluatedExt.getGatlingVersion());

      SourceSetContainer sourceSets =
          evaluatedProject.getExtensions().getByType(SourceSetContainer.class);
      if (Boolean.TRUE.equals(evaluatedExt.getIncludeMainOutput())) {
        evaluatedDependencies.add(
            gatlingImplementation,
            sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).getOutput());
      }
      if (Boolean.TRUE.equals(evaluatedExt.getIncludeTestOutput())) {
        evaluatedDependencies.add(
            gatlingImplementation,
            sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME).getOutput());
      }
    });
  }
}
