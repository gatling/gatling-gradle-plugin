package io.gatling.gradle

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.scala.ScalaPlugin
import org.gradle.util.GradleVersion
import org.gradle.util.VersionNumber

class GatlingPlugin implements Plugin<Project> {

    public static def GATLING_EXTENSION_NAME = 'gatling'

    public static def GATLING_LOGBACK_TASK_NAME = 'gatlingLogback'

    public static def GATLING_RUN_TASK_NAME = 'gatlingRun'

    static String GATLING_TASK_NAME_PREFIX = "$GATLING_RUN_TASK_NAME-"

    void apply(Project project) {

        if (VersionNumber.parse(GradleVersion.current().version).major < 4) {
            throw new GradleException("Current Gradle version (${GradleVersion.current().version}) is unsupported. Minimal supported version is 4.0")
        }

        project.pluginManager.apply ScalaPlugin

        GatlingPluginExtension gatlingExt = project.extensions.create(GATLING_EXTENSION_NAME, GatlingPluginExtension)

        createConfiguration(project, gatlingExt)

        project.tasks.create(name: GATLING_LOGBACK_TASK_NAME,
            dependsOn: project.tasks.gatlingClasses, type: LogbackConfigTask,
            description: "Prepare logback config", group: "Gatling")

        createGatlingTask(project, GATLING_RUN_TASK_NAME, null)

        project.tasks.addRule("Pattern: $GATLING_RUN_TASK_NAME-<SimulationClass>: Executes single Gatling simulation.") { String taskName ->
            if (taskName.startsWith(GATLING_TASK_NAME_PREFIX)) {
                createGatlingTask(project, taskName, (taskName - GATLING_TASK_NAME_PREFIX))
            }
        }
    }

    void createGatlingTask(Project project, String taskName, String simulationFQN = null) {
        def task = project.tasks.create(name: taskName,
            dependsOn: [project.tasks.gatlingClasses, project.tasks.gatlingLogback],
            type: GatlingRunTask, description: "Execute Gatling simulation", group: "Gatling")

        if (simulationFQN) {
            task.configure {
                simulations = {
                    include "${simulationFQN.replace('.', '/')}.scala"
                }
            }
        }
    }

    void createConfiguration(Project project, GatlingPluginExtension gatlingExt) {
        project.sourceSets {
            gatling {
                scala.srcDirs = [gatlingExt.SIMULATIONS_DIR]
                resources.srcDirs = [gatlingExt.RESOURCES_DIR]
            }
        }

        project.configurations {
            gatling { visible = false }
            gatlingImplementation.extendsFrom(gatling)
        }

        project.dependencies {
            gatlingRuntimeOnly project.sourceSets.gatling.output
        }

        project.afterEvaluate { Project p ->
            p.dependencies {
                implementation "org.scala-lang:scala-library:${p.extensions.getByType(GatlingPluginExtension).scalaVersion}"
                gatlingImplementation "org.scala-lang:scala-library:${p.extensions.getByType(GatlingPluginExtension).scalaVersion}"
                gatling "io.gatling.highcharts:gatling-charts-highcharts:${p.extensions.getByType(GatlingPluginExtension).toolVersion}"

                if (gatlingExt.includeMainOutput) {
                    gatlingImplementation project.sourceSets.main.output
                }
                if (gatlingExt.includeTestOutput) {
                    gatlingImplementation project.sourceSets.test.output
                }
            }
        }
    }
}

