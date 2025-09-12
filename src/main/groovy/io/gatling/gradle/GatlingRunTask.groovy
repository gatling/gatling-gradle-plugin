package io.gatling.gradle

import io.gatling.plugin.SimulationSelector
import io.gatling.plugin.util.NoFork
import io.gatling.plugin.util.SystemProperties
import io.gatling.shared.cli.GatlingCliOptions
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException
import org.gradle.api.tasks.VerificationException
import org.gradle.api.tasks.options.Option
import org.gradle.process.ExecOperations
import org.gradle.process.ExecResult
import org.gradle.process.JavaExecSpec
import org.gradle.util.GradleVersion

import javax.inject.Inject
import java.nio.charset.StandardCharsets

class GatlingRunTask extends DefaultTask {

    @Input
    @Optional
    List<String> jvmArgs

    @Input
    @Optional
    Map systemProperties

    @Input
    @Optional
    Map environment = [:]

    @Input
    @Option(option = "non-interactive", description = "Force this plugin in non interactive mode")
    boolean nonInteractive = Boolean.parseBoolean(System.getenv("CI"))

    @Input
    @Optional
    @Option(option = "simulation", description = "Force the simulation to be executed")
    String simulationClassName

    @Input
    @Option(option = "all", description = "Run all simulations sequentially in alphabetic order")
    boolean runAllSimulations

    @Input
    @Optional
    @Option(option = "run-description", description = "Add a description to be inserted in the HTML reports")
    String runDescription

    /**
     * Use only for attaching a debugger, not for running load tests. Run Gatling in gradle's JVM
     * instead of forking a new Java process. Requires at least Gatling 3.13.4. When enabled, the user
     * is responsible for passing the proper JVM options, eg with the <a
     * href="https://docs.gradle.org/current/userguide/build_environment.html#environment_variables_reference">JAVA_OPTS env var</a>.
     */
    @Input
    @Option(option = "same-process", description = "Run gatling in the Gradle process for debugging")
    boolean runInSameProcess

    @OutputDirectory
    Provider<Directory> gatlingReportDir = project.layout.buildDirectory.dir("reports/gatling")

    protected final ExecOperations execOperations
    protected final GatlingPluginExtension gatlingExt = project.extensions.getByType(GatlingPluginExtension)
    protected final Configuration gatlingRuntimeClasspathConfiguration = project.configurations.gatlingRuntimeClasspath
    protected final def includes = project.gatling.includes
    protected final def excludes = project.gatling.excludes

    @Inject
    GatlingRunTask(ExecOperations execOperations) {
        this.execOperations = execOperations
        outputs.upToDateWhen { false }
    }

    @TaskAction
    void gatlingRun() {
        if (runInSameProcess) {
            simulationClasses().forEach { simulationClass ->
                getLogger().info("Running simulation " + simulationClass + ".")
                new NoFork(
                    GatlingPluginExtension.GATLING_MAIN_CLASS,
                    createGatlingArgs(simulationClass),
                    gatlingRuntimeClasspathConfiguration.files.toList()
                ).run()
            }
        } else {
            Map<String, ExecResult> results = simulationClasses().collectEntries { String simulationClass ->
                getLogger().info("Running simulation " + simulationClass + ".")
                Properties propagatedSystemProperties = new Properties()
                for (Map.Entry<Object, Object> systemProp : System.getProperties().entrySet()) {
                    if (SystemProperties.isSystemPropertyPropagated(systemProp.getKey().toString())) {
                        propagatedSystemProperties.put(systemProp.getKey(), systemProp.getValue())
                    }
                }

                [(simulationClass): execOperations.javaexec({ JavaExecSpec exec ->
                    exec.mainClass.set(GatlingPluginExtension.GATLING_MAIN_CLASS)
                    exec.classpath = gatlingRuntimeClasspathConfiguration
                    exec.jvmArgs this.jvmArgs ?: gatlingExt.jvmArgs
                    exec.systemProperties propagatedSystemProperties
                    exec.systemProperties this.systemProperties ?: gatlingExt.systemProperties
                    exec.environment += gatlingExt.environment
                    exec.environment += this.environment
                    exec.args createGatlingArgs(simulationClass)
                    exec.standardInput = System.in
                    exec.ignoreExitValue = true
                } as Action<JavaExecSpec>)]
            }

            Map<String, ExecResult> crashed = results.findAll { it.value.exitValue != 0 && it.value.exitValue != 2 }
            Map<String, ExecResult> assertionFailed = results.findAll { it.value.exitValue == 2 }

            if (!crashed.isEmpty()) {
                throw new TaskExecutionException(this, new RuntimeException("Some of the simulations crashed: ${crashed.keySet().sort().join(", ")}"))
            } else if (!assertionFailed.isEmpty()) {
                throw new VerificationException("Some of the simulations failed assertions: ${assertionFailed.keySet().sort().join(", ")}")
            }
        }
    }

    List<String> simulationClasses() {
        List<String> gatlingClasspath = gatlingRuntimeClasspathConfiguration.getFiles().collect {it.getAbsolutePath()}.toList()

        SimulationSelector.Result result =
            SimulationSelector.simulations(
                simulationClassName,
                gatlingClasspath,
                includes,
                excludes,
                runAllSimulations,
                !nonInteractive)

        SimulationSelector.Result.Error error = result.error

        if (error != null) {
            // switch on null is only introduced in Java 18
            String errorMessage

            switch (error) {
                case SimulationSelector.Result.Error.NoSimulations:
                    errorMessage = "No simulations to run"
                    break
                case SimulationSelector.Result.Error.MoreThanOneSimulationInNonInteractiveMode:
                    errorMessage =
                        "Running in non-interactive mode, yet more than 1 simulation is available. Either specify one with --simulation=<className> or run them all sequentially with --all."
                    break
                case SimulationSelector.Result.Error.TooManyInteractiveAttempts:
                    errorMessage = "Max attempts of reading simulation number reached. Aborting."
                    break
                default:
                    throw new IllegalStateException("Unknown error: " + error)
            }

            throw new UnsupportedOperationException(errorMessage)
        }

        return result.simulations
    }

    List<String> createGatlingArgs(String simulationClass) {
        def baseArgs = [
            GatlingCliOptions.Simulation.shortOption(), simulationClass,
            GatlingCliOptions.ResultsFolder.shortOption(), gatlingReportDir.get().asFile.absolutePath,
            GatlingCliOptions.Launcher.shortOption(), "gradle",
            GatlingCliOptions.BuildToolVersion.shortOption(), GradleVersion.current().version
        ]

        if (runDescription) {
            baseArgs += [GatlingCliOptions.RunDescription.shortOption(), Base64.encoder.encodeToString(runDescription.getBytes(StandardCharsets.UTF_8))]
        }

        return baseArgs
    }
}
