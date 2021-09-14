package io.gatling.gradle

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException
import org.gradle.process.ExecResult
import org.gradle.process.JavaExecSpec

import java.nio.file.Path
import java.nio.file.Paths

class GatlingRunTask extends DefaultTask implements JvmConfigurable {

    @Internal
    Closure simulations

    @OutputDirectory
    File gatlingReportDir = project.file("${project.reportsDir}/gatling")

    @InputFiles
    FileTree getJavaSimulationSources() {
        def simulationFilter = this.simulations ?: project.gatling.simulations
        return project.sourceSets.gatling.java.matching(simulationFilter)
    }

    @InputFiles
    FileTree getKotlinSimulationSources() {
        def simulationFilter = this.simulations ?: project.gatling.simulations
        return project.sourceSets.gatling.kotlin.matching(simulationFilter)
    }

    @InputFiles
    FileTree getScalaSimulationSources() {
        def simulationFilter = this.simulations ?: project.gatling.simulations
        return project.sourceSets.gatling.scala.matching(simulationFilter)
    }

    List<String> createGatlingArgs() {

        def classesDirs = project.sourceSets.gatling.output.classesDirs

        File javaClasses = classesDirs.filter { it.parentFile.name == 'java' }.singleFile
        File scalaClasses = classesDirs.filter { it.parentFile.name == 'scala' }.singleFile
        File kotlinClasses = classesDirs.filter { it.parentFile.name == 'kotlin' }.singleFile
        File binariesFolder = (scalaClasses.isDirectory() && scalaClasses.toPath().isEmpty()) ? scalaClasses :
            (kotlinClasses.isDirectory() && kotlinClasses.toPath().isEmpty()) ? kotlinClasses :
                javaClasses

        return ['-bf', binariesFolder.absolutePath,
                "-rsf", "${project.sourceSets.gatling.output.resourcesDir}",
                "-rf", gatlingReportDir.absolutePath]
    }

    Iterable<String> simulationFilesToFQN() {
        def javaSrcDirs = project.sourceSets.gatling.java.srcDirs.collect { Paths.get(it.absolutePath) }
        def javaFiles = getJavaSimulationSources().collect { Paths.get(it.absolutePath) }

        def javaFQNs = javaFiles.collect { Path srcFile ->
            javaSrcDirs.find { srcFile.startsWith(it) }.relativize(srcFile).join(".") - ".java"
        }

        def kotlinSrcDirs = project.sourceSets.gatling.kotlin.srcDirs.collect { Paths.get(it.absolutePath) }
        def kotlinFiles = getKotlinSimulationSources().collect { Paths.get(it.absolutePath) }

        def kotlinFQNs = kotlinFiles.collect { Path srcFile ->
            kotlinSrcDirs.find { srcFile.startsWith(it) }.relativize(srcFile).join(".") - ".kt"
        }

        def scalaSrcDirs = project.sourceSets.gatling.scala.srcDirs.collect { Paths.get(it.absolutePath) }
        def scalaFiles = getScalaSimulationSources().collect { Paths.get(it.absolutePath) }

        def scalaFQNs = scalaFiles.collect { Path srcFile ->
            scalaSrcDirs.find { srcFile.startsWith(it) }.relativize(srcFile).join(".") - ".scala"
        }

        return javaFQNs + kotlinFQNs + scalaFQNs
    }

    @TaskAction
    void gatlingRun() {
        def gatlingExt = project.extensions.getByType(GatlingPluginExtension)

        Map<String, ExecResult> results = simulationFilesToFQN().collectEntries { String simulationClzName ->
            [(simulationClzName): project.javaexec({ JavaExecSpec exec ->
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

                exec.args this.createGatlingArgs()
                exec.args "-s", simulationClzName

                exec.standardInput = System.in

                exec.ignoreExitValue = true
            } as Action<JavaExecSpec>)]
        }

        Map<String, ExecResult> failed = results.findAll { it.value.exitValue != 0 }

        if (!failed.isEmpty()) {
            throw new TaskExecutionException(this, new RuntimeException("There're failed simulations: ${failed.keySet().sort().join(", ")}"))
        }
    }
}
