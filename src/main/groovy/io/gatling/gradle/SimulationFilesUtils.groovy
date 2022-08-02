package io.gatling.gradle

import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.InputFiles

import java.nio.file.Path
import java.nio.file.Paths

final class SimulationFilesUtils {

    static Iterable<String> resolveSimulations(Project project, Closure simulations) {
        def javaSrcDirs = project.sourceSets.gatling.java.srcDirs.collect { Paths.get(it.absolutePath) }
        def javaFiles = getJavaSimulationSources(project, simulations).collect { Paths.get(it.absolutePath) }

        def javaFQNs = javaFiles.collect { Path srcFile ->
            javaSrcDirs.find { srcFile.startsWith(it) }.relativize(srcFile).join(".") - ".java"
        }

        List<String> kotlinFQNs
        if (project.sourceSets.gatling.hasProperty("kotlin")) {
            def kotlinSrcDirs = project.sourceSets.gatling.kotlin.srcDirs.collect { Paths.get(it.absolutePath) }
            def kotlinFiles = getKotlinSimulationSources(project, simulations).collect { Paths.get(it.absolutePath) }

            kotlinFQNs = kotlinFiles.collect { Path srcFile ->
                kotlinSrcDirs.find { srcFile.startsWith(it) }.relativize(srcFile).join(".") - ".kt"
            }
        } else {
            kotlinFQNs = []
        }

        def scalaSrcDirs = project.sourceSets.gatling.scala.srcDirs.collect { Paths.get(it.absolutePath) }
        def scalaFiles = getScalaSimulationSources(project, simulations).collect { Paths.get(it.absolutePath) }

        def scalaFQNs = scalaFiles.collect { Path srcFile ->
            scalaSrcDirs.find { srcFile.startsWith(it) }.relativize(srcFile).join(".") - ".scala"
        }

        return javaFQNs + kotlinFQNs + scalaFQNs
    }

    @InputFiles
    static FileTree getJavaSimulationSources(Project project, Closure simulations) {
        def simulationFilter = simulations ?: project.gatling.simulations
        return project.sourceSets.gatling.java.matching(simulationFilter)
    }

    @InputFiles
    static FileTree getKotlinSimulationSources(Project project, Closure simulations) {
        if (project.sourceSets.gatling.hasProperty("kotlin")) {
            def simulationFilter = simulations ?: project.gatling.simulations
            return project.sourceSets.gatling.kotlin.matching(simulationFilter)
        } else {
            return project.files().asFileTree
        }
    }

    @InputFiles
    static FileTree getScalaSimulationSources(Project project, Closure simulations) {
        def simulationFilter = simulations ?: project.gatling.simulations
        return project.sourceSets.gatling.scala.matching(simulationFilter)
    }

}
