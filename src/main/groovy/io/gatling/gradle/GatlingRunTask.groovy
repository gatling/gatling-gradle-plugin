package io.gatling.gradle

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException
import org.gradle.process.ExecResult
import org.gradle.process.JavaExecSpec
import org.gradle.util.GradleVersion

final class GatlingRunTask extends DefaultTask implements JvmConfigurable {
    @Internal
    Closure simulations

    @OutputDirectory
    File gatlingReportDir = project.file("${project.reportsDir}/gatling")

    GatlingRunTask() {
        outputs.upToDateWhen { false }
    }

    private static File classesDirForLanguage(FileCollection classesDirs, String language) {
        def classesDirsOfType = classesDirs.filter { it.parentFile.name == language }
        if (classesDirsOfType.isEmpty()) {
            return null
        } else {
            File dir = classesDirsOfType.singleFile
            return dir.isDirectory() && !dir.toPath().isEmpty() ? dir : null
        }
    }

    List<String> createGatlingArgs(String gatlingVersion) {

        FileCollection classesDirs = project.sourceSets.gatling.output.classesDirs

        File javaClasses = classesDirForLanguage(classesDirs, 'java')
        File scalaClasses = classesDirForLanguage(classesDirs, 'scala')
        File kotlinClasses = classesDirForLanguage(classesDirs, 'kotlin')
        File binariesFolder = scalaClasses != null ? scalaClasses :
            kotlinClasses != null ? kotlinClasses : javaClasses

        def gatlingVersionComponents = gatlingVersion.split("\\.")
        int gatlingMajorVersion = Integer.valueOf(gatlingVersionComponents[0])
        int gatlingMinorVersion = Integer.valueOf(gatlingVersionComponents[1])

        def baseArgs = [
            '-bf', binariesFolder.absolutePath,
            "-rsf", "${project.sourceSets.gatling.output.resourcesDir}",
            "-rf", gatlingReportDir.absolutePath]

        return (gatlingMajorVersion == 3 && gatlingMinorVersion >= 8) || gatlingMajorVersion >= 4 ?
            baseArgs + ["-l", "gradle", "-btv", GradleVersion.current().version] :
            baseArgs
    }

    @TaskAction
    void gatlingRun() {
        def gatlingExt = project.extensions.getByType(GatlingPluginExtension)

        Map<String, ExecResult> results = simulationFilesToFQN().collectEntries { String simulationClass ->
            [(simulationClass): project.javaexec({ JavaExecSpec exec ->
                exec.main = GatlingPluginExtension.GATLING_MAIN_CLASS
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

    Iterable<String> simulationFilesToFQN() {
        return SimulationFilesUtils.resolveSimulations(project, this.simulations)
    }
}
