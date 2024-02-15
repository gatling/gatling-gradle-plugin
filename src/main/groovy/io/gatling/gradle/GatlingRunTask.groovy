package io.gatling.gradle

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException
import org.gradle.process.ExecResult
import org.gradle.process.JavaExecSpec
import org.gradle.util.GradleVersion

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

    @Internal
    String simulationClass = null

    @OutputDirectory
    File gatlingReportDir = project.file("${project.reportsDir}/gatling")

    GatlingRunTask() {
        outputs.upToDateWhen { false }
    }

    List<String> createGatlingArgs(String gatlingVersion) {
        def gatlingVersionComponents = gatlingVersion.split("\\.")
        int gatlingMajorVersion = Integer.valueOf(gatlingVersionComponents[0])
        int gatlingMinorVersion = Integer.valueOf(gatlingVersionComponents[1])

        def baseArgs = ["-rf", gatlingReportDir.absolutePath]

        return (gatlingMajorVersion == 3 && gatlingMinorVersion >= 8) || gatlingMajorVersion >= 4 ?
            baseArgs + ["-l", "gradle", "-btv", GradleVersion.current().version] :
            baseArgs
    }

    @TaskAction
    void gatlingRun() {
        def gatlingExt = project.extensions.getByType(GatlingPluginExtension)

        Map<String, ExecResult> results = simulationClasses().collectEntries { String simulationClass ->
            [(simulationClass): project.javaexec({ JavaExecSpec exec ->
                exec.mainClass.set(GatlingPluginExtension.GATLING_MAIN_CLASS)
                exec.classpath = project.configurations.gatlingRuntimeClasspath

                exec.jvmArgs this.jvmArgs ?: gatlingExt.jvmArgs
                exec.systemProperties System.properties
                exec.systemProperties this.systemProperties ?: gatlingExt.systemProperties
                exec.environment += gatlingExt.environment
                exec.environment += this.environment

                def logbackFile = LogbackConfigTask.logbackFile(project.buildDir)
                if (logbackFile.exists()) {
                    exec.systemProperty("logback.configurationFile", logbackFile.absolutePath)
                }

                exec.args this.createGatlingArgs(gatlingExt.gatlingVersion)
                exec.args "-s", simulationClass

                exec.standardInput = System.in

                exec.ignoreExitValue = true
            } as Action<JavaExecSpec>)]
        }

        Map<String, ExecResult> failed = results.findAll { it.value.exitValue != 0 }

        if (!failed.isEmpty()) {
            throw new TaskExecutionException(this, new RuntimeException("There're failed simulations: ${failed.keySet().sort().join(", ")}"))
        }
    }

    List<String> simulationClasses() {
        def classpath = project.configurations.gatlingRuntimeClasspath.getFiles()
        def includes = this.simulationClass ? List.of(simulationClass) : project.gatling.includes
        def excludes = this.simulationClass ? List.of() : project.gatling.excludes

        return SimulationFilesUtils.resolveSimulations(classpath, includes, excludes)
    }
}
