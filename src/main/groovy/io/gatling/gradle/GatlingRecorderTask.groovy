package io.gatling.gradle

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.gradle.process.JavaExecSpec
import org.gradle.process.internal.ExecException

class GatlingRecorderTask extends DefaultTask {

    @Input
    @Optional
    String simulationClass

    @Input
    @Optional
    String simulationPackage

    @OutputDirectory
    File gatlingReportDir = project.file("${project.reportsDir}/gatling")

    GatlingRecorderTask() {
        outputs.upToDateWhen { false }
    }

    List<String> createRecorderArgs() {
        def gatling = project.sourceSets.gatling

        File javaSrcDir = gatling.hasProperty("java") ? gatling.java.srcDirs?.find() : null
        File scalaSrcDir = gatling.hasProperty("scala") ? gatling.scala.srcDirs?.find() : null
        File kotlinSrcDir = gatling.hasProperty("kotlin") ? gatling.kotlin.srcDirs?.find() : null
        File resourcesDir = gatling.resources.srcDirs?.find()

        List<String> args
        if (scalaSrcDir != null && scalaSrcDir.exists()) {
            args = [
                "-sf", scalaSrcDir.getAbsolutePath(),
                "-fmt", "scala"
            ]
        } else if (kotlinSrcDir != null && kotlinSrcDir.exists()) {
            args = [
                "-sf", kotlinSrcDir.getAbsolutePath(),
                "-fmt", "kotlin"
            ]
        } else if (javaSrcDir != null && javaSrcDir.exists()) {
            args = [
                "-sf", javaSrcDir.getAbsolutePath()
                // let the Recorder pick a default Java format based on Java version
            ]
        } else {
            throw new IllegalStateException("None of the scala/kotlin/java src dir exist")
        }

        args += [ "-rf", resourcesDir.getAbsolutePath() ]

        if (simulationPackage != null) {
            args += [ "-pkg", simulationPackage ]
        }

        if (simulationClass != null) {
            args += [ "-cn", simulationClass ]
        }

        return args
    }

    @TaskAction
    void gatlingRecorder() {
        def gatlingExt = project.extensions.getByType(GatlingPluginExtension)

        def result = project.javaexec({ JavaExecSpec exec ->
            exec.mainClass.set(GatlingPluginExtension.GATLING_RECORDER_CLASS)
            exec.classpath = project.configurations.gatlingRuntimeClasspath

            def logbackFile = LogbackConfigTask.logbackFile(project.buildDir)
            if (logbackFile.exists()) {
                exec.systemProperty("logback.configurationFile", logbackFile.absolutePath)
            }

            exec.args this.createRecorderArgs()

            exec.standardInput = System.in

            exec.ignoreExitValue = true
        } as Action<JavaExecSpec>)

        try {
            result.rethrowFailure()
        } catch (ExecException e) {
            throw new TaskExecutionException(this, new RuntimeException("Failed to launch recorder", e))
        }
    }
}
