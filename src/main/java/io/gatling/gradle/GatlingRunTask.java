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

import io.gatling.plugin.SimulationSelector;
import io.gatling.plugin.util.NoFork;
import io.gatling.plugin.util.SystemProperties;
import io.gatling.shared.cli.GatlingCliOptions;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.specs.Specs;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;
import org.gradle.api.tasks.VerificationException;
import org.gradle.api.tasks.options.Option;
import org.gradle.process.ExecOperations;
import org.gradle.process.ExecResult;
import org.gradle.util.GradleVersion;
import org.gradle.work.DisableCachingByDefault;

@DisableCachingByDefault(because = "Running a Gatling test locally is not a cacheable task")
public class GatlingRunTask extends DefaultTask {

  private List<String> jvmArgs;

  private Map<String, Object> systemProperties;

  private Map<String, Object> environment = new LinkedHashMap<>();

  private boolean nonInteractive = Boolean.parseBoolean(System.getenv("CI"));

  private String simulationClassName;

  private boolean runAllSimulations;

  private String runDescription;

  private boolean runInSameProcess;

  private FileCollection gatlingRuntimeClasspath =
      getProject().getConfigurations().getByName("gatlingRuntimeClasspath");

  private File gatlingReportDir =
      getProject().getLayout().getBuildDirectory().dir("reports/gatling").get().getAsFile();

  protected final ExecOperations execOperations;
  protected final GatlingPluginExtension gatlingExt =
      getProject().getExtensions().getByType(GatlingPluginExtension.class);
  // copied into mutable lists as Gradle < 9 can't restore JDK immutable collections (e.g.
  // List.of()) from the configuration cache
  protected final List<String> includes = new ArrayList<>(gatlingExt.getIncludes());
  protected final List<String> excludes = new ArrayList<>(gatlingExt.getExcludes());

  @Inject
  public GatlingRunTask(ExecOperations execOperations) {
    this.execOperations = execOperations;
    getOutputs().upToDateWhen(Specs.satisfyNone());
  }

  @Input
  @Optional
  public List<String> getJvmArgs() {
    return jvmArgs;
  }

  public void setJvmArgs(List<String> jvmArgs) {
    this.jvmArgs = jvmArgs;
  }

  @Input
  @Optional
  public Map<String, Object> getSystemProperties() {
    return systemProperties;
  }

  public void setSystemProperties(Map<String, Object> systemProperties) {
    this.systemProperties = systemProperties;
  }

  @Input
  @Optional
  public Map<String, Object> getEnvironment() {
    return environment;
  }

  public void setEnvironment(Map<String, Object> environment) {
    this.environment = environment;
  }

  @Input
  public boolean getNonInteractive() {
    return nonInteractive;
  }

  @Option(option = "non-interactive", description = "Force this plugin in non interactive mode")
  public void setNonInteractive(boolean nonInteractive) {
    this.nonInteractive = nonInteractive;
  }

  @Input
  @Optional
  public String getSimulationClassName() {
    return simulationClassName;
  }

  @Option(option = "simulation", description = "Force the simulation to be executed")
  public void setSimulationClassName(String simulationClassName) {
    this.simulationClassName = simulationClassName;
  }

  @Input
  public boolean getRunAllSimulations() {
    return runAllSimulations;
  }

  @Option(option = "all", description = "Run all simulations sequentially in alphabetic order")
  public void setRunAllSimulations(boolean runAllSimulations) {
    this.runAllSimulations = runAllSimulations;
  }

  @Input
  @Optional
  public String getRunDescription() {
    return runDescription;
  }

  @Option(
      option = "run-description",
      description = "Add a description to be inserted in the HTML reports")
  public void setRunDescription(String runDescription) {
    this.runDescription = runDescription;
  }

  /**
   * Use only for attaching a debugger, not for running load tests. Run Gatling in gradle's JVM
   * instead of forking a new Java process. Requires at least Gatling 3.13.4. When enabled, the user
   * is responsible for passing the proper JVM options, eg with the <a
   * href="https://docs.gradle.org/current/userguide/build_environment.html#environment_variables_reference">JAVA_OPTS
   * env var</a>.
   */
  @Input
  public boolean getRunInSameProcess() {
    return runInSameProcess;
  }

  @Option(option = "same-process", description = "Run Gatling in the Gradle process for debugging")
  public void setRunInSameProcess(boolean runInSameProcess) {
    this.runInSameProcess = runInSameProcess;
  }

  @InputFiles
  @Classpath
  public FileCollection getGatlingRuntimeClasspath() {
    return gatlingRuntimeClasspath;
  }

  public void setGatlingRuntimeClasspath(FileCollection gatlingRuntimeClasspath) {
    this.gatlingRuntimeClasspath = gatlingRuntimeClasspath;
  }

  @OutputDirectory
  public File getGatlingReportDir() {
    return gatlingReportDir;
  }

  public void setGatlingReportDir(File gatlingReportDir) {
    this.gatlingReportDir = gatlingReportDir;
  }

  @TaskAction
  public void gatlingRun() throws Exception {
    if (runInSameProcess) {
      for (String simulationClass : simulationClasses()) {
        getLogger().info("Running simulation " + simulationClass + ".");
        new NoFork(
                GatlingPluginExtension.GATLING_MAIN_CLASS,
                createGatlingArgs(simulationClass),
                new ArrayList<>(gatlingRuntimeClasspath.getFiles()))
            .run();
      }
    } else {
      // task values take precedence over the extension ones, unless empty
      List<String> effectiveJvmArgs =
          jvmArgs == null || jvmArgs.isEmpty() ? gatlingExt.getJvmArgs() : jvmArgs;
      Map<String, Object> effectiveSystemProperties =
          systemProperties == null || systemProperties.isEmpty()
              ? gatlingExt.getSystemProperties()
              : systemProperties;

      Map<String, ExecResult> results = new LinkedHashMap<>();
      for (String simulationClass : simulationClasses()) {
        getLogger().info("Running simulation " + simulationClass + ".");
        Map<String, Object> propagatedSystemProperties = new HashMap<>();
        for (Map.Entry<Object, Object> systemProp : System.getProperties().entrySet()) {
          if (SystemProperties.isSystemPropertyPropagated(systemProp.getKey().toString())) {
            propagatedSystemProperties.put(systemProp.getKey().toString(), systemProp.getValue());
          }
        }

        results.put(
            simulationClass,
            execOperations.javaexec(
                exec -> {
                  exec.getMainClass().set(GatlingPluginExtension.GATLING_MAIN_CLASS);
                  exec.setClasspath(gatlingRuntimeClasspath);
                  if (effectiveJvmArgs != null) {
                    exec.jvmArgs(effectiveJvmArgs);
                  }
                  exec.systemProperties(propagatedSystemProperties);
                  if (effectiveSystemProperties != null) {
                    exec.systemProperties(effectiveSystemProperties);
                  }
                  if (gatlingExt.getEnvironment() != null) {
                    exec.environment(gatlingExt.getEnvironment());
                  }
                  if (environment != null) {
                    exec.environment(environment);
                  }
                  exec.args(createGatlingArgs(simulationClass));
                  exec.setStandardInput(System.in);
                  exec.setIgnoreExitValue(true);
                }));
      }

      String crashed = sortedSimulationsWithExitValue(results, false);
      String assertionFailed = sortedSimulationsWithExitValue(results, true);

      if (!crashed.isEmpty()) {
        throw new TaskExecutionException(
            this, new RuntimeException("Some of the simulations crashed: " + crashed));
      } else if (!assertionFailed.isEmpty()) {
        throw new VerificationException(
            "Some of the simulations failed assertions: " + assertionFailed);
      }
    }
  }

  // exit code 2 means assertion failures, other non-zero ones mean crashes
  private static String sortedSimulationsWithExitValue(
      Map<String, ExecResult> results, boolean assertionFailures) {
    return results.entrySet().stream()
        .filter(
            entry -> {
              int exitValue = entry.getValue().getExitValue();
              return assertionFailures ? exitValue == 2 : exitValue != 0 && exitValue != 2;
            })
        .map(Map.Entry::getKey)
        .sorted()
        .collect(Collectors.joining(", "));
  }

  public List<String> simulationClasses() {
    List<String> gatlingClasspath =
        gatlingRuntimeClasspath.getFiles().stream()
            .map(File::getAbsolutePath)
            .collect(Collectors.toList());

    SimulationSelector.Result result =
        SimulationSelector.simulations(
            simulationClassName,
            gatlingClasspath,
            includes,
            excludes,
            runAllSimulations,
            !nonInteractive);

    SimulationSelector.Result.Error error = result.error;

    if (error != null) {
      String errorMessage;
      switch (error) {
        case NoSimulations:
          errorMessage = "No simulations to run";
          break;
        case MoreThanOneSimulationInNonInteractiveMode:
          errorMessage =
              "Running in non-interactive mode, yet more than 1 simulation is available. Either specify one with --simulation=<className> or run them all sequentially with --all.";
          break;
        case TooManyInteractiveAttempts:
          errorMessage = "Max attempts of reading simulation number reached. Aborting.";
          break;
        default:
          throw new IllegalStateException("Unknown error: " + error);
      }
      throw new UnsupportedOperationException(errorMessage);
    }

    return result.simulations;
  }

  public List<String> createGatlingArgs(String simulationClass) {
    List<String> args =
        new ArrayList<>(
            Arrays.asList(
                GatlingCliOptions.Simulation.shortOption(),
                simulationClass,
                GatlingCliOptions.ResultsFolder.shortOption(),
                gatlingReportDir.getAbsolutePath(),
                GatlingCliOptions.Launcher.shortOption(),
                "gradle",
                GatlingCliOptions.BuildToolVersion.shortOption(),
                GradleVersion.current().getVersion()));

    if (runDescription != null && !runDescription.isEmpty()) {
      args.add(GatlingCliOptions.RunDescription.shortOption());
      args.add(Base64.getEncoder().encodeToString(runDescription.getBytes(StandardCharsets.UTF_8)));
    }

    return args;
  }
}
