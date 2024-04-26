package io.gatling.gradle

import io.gatling.plugin.SimulationSelector
import io.gatling.shared.cli.GatlingCliOptions
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException
import org.gradle.api.tasks.options.Option
import org.gradle.process.ExecResult
import org.gradle.process.JavaExecSpec
import org.gradle.util.GradleVersion

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

    @OutputDirectory
    File gatlingReportDir = project.file("${project.reportsDir}/gatling")

    GatlingRunTask() {
        outputs.upToDateWhen { false }
    }

    @TaskAction
    void gatlingRun() {
        def gatlingExt = project.extensions.getByType(GatlingPluginExtension)

        Map<String, ExecResult> results = simulationClasses().collectEntries { String simulationClass ->
            getLogger().info("Running simulation " + simulationClass + ".")

            [(simulationClass): project.javaexec({ JavaExecSpec exec ->
                exec.mainClass.set(GatlingPluginExtension.GATLING_MAIN_CLASS)
                exec.classpath = project.configurations.gatlingRuntimeClasspath

                exec.jvmArgs this.jvmArgs ?: gatlingExt.jvmArgs
                exec.systemProperties System.properties
                exec.systemProperties this.systemProperties ?: gatlingExt.systemProperties
                exec.environment += gatlingExt.environment
                exec.environment += this.environment

                exec.args createGatlingArgs(simulationClass)

                exec.standardInput = System.in

                exec.ignoreExitValue = true
            } as Action<JavaExecSpec>)]
        }

        Map<String, ExecResult> failed = results.findAll { it.value.exitValue != 0 }

        if (!failed.isEmpty()) {
            throw new TaskExecutionException(this, new RuntimeException("There are failed simulations: ${failed.keySet().sort().join(", ")}"))
        }
    }

    List<String> simulationClasses() {
        List<String> gatlingClasspath = project.configurations.gatlingRuntimeClasspath.getFiles().collect {it.getAbsolutePath()}.toList()

        SimulationSelector.Result result =
            SimulationSelector.simulations(
                simulationClassName,
                gatlingClasspath,
                project.gatling.includes,
                project.gatling.excludes,
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
            GatlingCliOptions.ResultsFolder.shortOption(), gatlingReportDir.absolutePath,
            GatlingCliOptions.Launcher.shortOption(), "gradle",
            GatlingCliOptions.BuildToolVersion.shortOption(), GradleVersion.current().version
        ]

        if (runDescription) {
            baseArgs += [GatlingCliOptions.RunDescription.shortOption(), Base64.encoder.encodeToString(runDescription.getBytes(StandardCharsets.UTF_8))]
        }

        return baseArgs
    }
}
