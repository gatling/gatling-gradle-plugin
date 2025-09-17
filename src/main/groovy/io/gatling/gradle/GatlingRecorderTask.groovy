package io.gatling.gradle

import io.gatling.shared.cli.RecorderCliOptions
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import org.gradle.process.JavaExecSpec
import org.gradle.process.internal.ExecException

import javax.inject.Inject

class GatlingRecorderTask extends DefaultTask {

    @Input
    @Optional
    String simulationClass

    @Input
    @Optional
    String simulationPackage

    @OutputDirectory
    Provider<Directory> gatlingReportDir = project.layout.buildDirectory.dir("reports/gatling")

    protected final ExecOperations execOperations
    protected final Configuration gatlingRuntimeClasspathConfiguration = project.configurations.gatlingRuntimeClasspath
    protected final SourceSet gatlingSourceSet = project.sourceSets.gatling
    protected final String groupId = project.group.toString()

    @Inject
    GatlingRecorderTask(ExecOperations execOperations) {
        this.execOperations = execOperations
        outputs.upToDateWhen { false }
    }

    List<String> createRecorderArgs() {
        File javaSrcDir = gatlingSourceSet.hasProperty("java") ? gatlingSourceSet.java.srcDirs?.find() : null
        File scalaSrcDir = gatlingSourceSet.hasProperty("scala") ? gatlingSourceSet.scala.srcDirs?.find() : null
        File kotlinSrcDir = gatlingSourceSet.hasProperty("kotlin") ? gatlingSourceSet.kotlin.srcDirs?.find() : null
        File resourcesDir = gatlingSourceSet.resources.srcDirs?.find()

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

        def nonEmptyGroup = groupId.isEmpty() ? null : groupId
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
        def result = execOperations.javaexec({ JavaExecSpec exec ->
            exec.mainClass.set(GatlingPluginExtension.GATLING_RECORDER_CLASS)
            exec.classpath = gatlingRuntimeClasspathConfiguration
            exec.args createRecorderArgs()

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
