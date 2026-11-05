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

import io.gatling.shared.cli.RecorderCliOptions;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.specs.Specs;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;
import org.gradle.process.ExecOperations;
import org.gradle.process.ExecResult;
import org.gradle.process.internal.ExecException;
import org.gradle.work.DisableCachingByDefault;

@DisableCachingByDefault(because = "Launching the recorder is not a cacheable task")
public class GatlingRecorderTask extends DefaultTask {

  private String simulationClass;

  private String simulationPackage;

  private File gatlingReportDir =
      getProject().getLayout().getBuildDirectory().dir("reports/gatling").get().getAsFile();

  protected final ExecOperations execOperations;
  protected final FileCollection gatlingRuntimeClasspath =
      getProject().getConfigurations().getByName("gatlingRuntimeClasspath");
  protected final String groupId = getProject().getGroup().toString();
  // resolved eagerly as SourceSet can't be stored in the configuration cache
  protected final File javaSrcDir;
  protected final File scalaSrcDir;
  protected final File kotlinSrcDir;
  protected final File resourcesDir;

  @Inject
  public GatlingRecorderTask(ExecOperations execOperations) {
    this.execOperations = execOperations;
    getOutputs().upToDateWhen(Specs.satisfyNone());
    SourceSet gatlingSourceSet =
        getProject().getExtensions().getByType(SourceSetContainer.class).getByName("gatling");
    javaSrcDir = firstSrcDir(gatlingSourceSet.getJava());
    scalaSrcDir = firstSrcDir(gatlingSourceSet.getExtensions().findByName("scala"));
    kotlinSrcDir = firstSrcDir(gatlingSourceSet.getExtensions().findByName("kotlin"));
    resourcesDir = firstSrcDir(gatlingSourceSet.getResources());
  }

  private static File firstSrcDir(Object sourceDirectorySet) {
    if (sourceDirectorySet instanceof SourceDirectorySet) {
      Iterator<File> srcDirs = ((SourceDirectorySet) sourceDirectorySet).getSrcDirs().iterator();
      return srcDirs.hasNext() ? srcDirs.next() : null;
    }
    return null;
  }

  @Input
  @Optional
  public String getSimulationClass() {
    return simulationClass;
  }

  public void setSimulationClass(String simulationClass) {
    this.simulationClass = simulationClass;
  }

  @Input
  @Optional
  public String getSimulationPackage() {
    return simulationPackage;
  }

  public void setSimulationPackage(String simulationPackage) {
    this.simulationPackage = simulationPackage;
  }

  @OutputDirectory
  public File getGatlingReportDir() {
    return gatlingReportDir;
  }

  public void setGatlingReportDir(File gatlingReportDir) {
    this.gatlingReportDir = gatlingReportDir;
  }

  public List<String> createRecorderArgs() {
    List<String> args;
    if (scalaSrcDir != null && scalaSrcDir.exists()) {
      args =
          new ArrayList<>(
              Arrays.asList(
                  RecorderCliOptions.SimulationsFolder.shortOption(),
                  scalaSrcDir.getAbsolutePath(),
                  RecorderCliOptions.Format.shortOption(),
                  "scala"));
    } else if (kotlinSrcDir != null && kotlinSrcDir.exists()) {
      args =
          new ArrayList<>(
              Arrays.asList(
                  RecorderCliOptions.SimulationsFolder.shortOption(),
                  kotlinSrcDir.getAbsolutePath(),
                  RecorderCliOptions.Format.shortOption(),
                  "kotlin"));
    } else if (javaSrcDir != null && javaSrcDir.exists()) {
      // let the Recorder pick a default Java format based on Java version
      args =
          new ArrayList<>(
              Arrays.asList(
                  RecorderCliOptions.SimulationsFolder.shortOption(),
                  javaSrcDir.getAbsolutePath()));
    } else {
      throw new IllegalStateException("None of the scala/kotlin/java src dir exist");
    }

    args.add(RecorderCliOptions.ResourcesFolder.shortOption());
    args.add(resourcesDir.getAbsolutePath());

    String nonEmptyGroup = groupId.isEmpty() ? null : groupId;
    String resolvedPackage = simulationPackage != null ? simulationPackage : nonEmptyGroup;

    if (resolvedPackage != null) {
      args.add(RecorderCliOptions.Package.shortOption());
      args.add(resolvedPackage);
    }

    if (simulationClass != null) {
      args.add(RecorderCliOptions.ClassName.shortOption());
      args.add(simulationClass);
    }

    return args;
  }

  @TaskAction
  public void gatlingRecorder() {
    ExecResult result =
        execOperations.javaexec(exec -> {
          exec.getMainClass().set(GatlingPluginExtension.GATLING_RECORDER_CLASS);
          exec.setClasspath(gatlingRuntimeClasspath);
          exec.args(createRecorderArgs());
          exec.setStandardInput(System.in);
          exec.setIgnoreExitValue(true);
        });

    try {
      result.rethrowFailure();
    } catch (ExecException e) {
      throw new TaskExecutionException(this, new RuntimeException("Failed to launch recorder", e));
    }
  }
}
