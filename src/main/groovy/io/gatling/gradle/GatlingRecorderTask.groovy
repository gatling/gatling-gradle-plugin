package io.gatling.gradle

import io.gatling.shared.cli.RecorderCliOptions
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
                RecorderCliOptions.SimulationsFolder.shortOption(), scalaSrcDir.getAbsolutePath(),
                RecorderCliOptions.Format.shortOption(), "scala"
            ]
        } else if (kotlinSrcDir != null && kotlinSrcDir.exists()) {
            args = [
                RecorderCliOptions.SimulationsFolder.shortOption(), kotlinSrcDir.getAbsolutePath(),
                RecorderCliOptions.Format.shortOption(), "kotlin"
            ]
        } else if (javaSrcDir != null && javaSrcDir.exists()) {
            args = [
                RecorderCliOptions.SimulationsFolder.shortOption(), javaSrcDir.getAbsolutePath()
                // let the Recorder pick a default Java format based on Java version
            ]
        } else {
            throw new IllegalStateException("None of the scala/kotlin/java src dir exist")
        }

        args += [RecorderCliOptions.ResourcesFolder.shortOption(), resourcesDir.getAbsolutePath()]

        def projectGroup = project.group.toString()
        def nonEmptyGroup = projectGroup.isEmpty() ? null : projectGroup
        def resolvedPackage = simulationPackage != null ? simulationPackage : nonEmptyGroup

        if (resolvedPackage != null) {
            args += [ RecorderCliOptions.Package.shortOption(), resolvedPackage ]
        }

        if (simulationClass != null) {
            args += [ RecorderCliOptions.ClassName.shortOption(), simulationClass ]
        }

        return args
    }

    @TaskAction
    void gatlingRecorder() {
        def gatlingExt = project.extensions.getByType(GatlingPluginExtension)

        def result = project.javaexec({ JavaExecSpec exec ->
            exec.mainClass.set(GatlingPluginExtension.GATLING_RECORDER_CLASS)
            exec.classpath = project.configurations.gatlingRuntimeClasspath
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
