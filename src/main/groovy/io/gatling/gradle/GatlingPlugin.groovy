package io.gatling.gradle

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.plugins.scala.ScalaPlugin
import org.gradle.api.tasks.TaskProvider
import org.gradle.util.GradleVersion

final class GatlingPlugin implements Plugin<Project> {

    public static def GATLING_EXTENSION_NAME = 'gatling'

    public static def GATLING_LOGBACK_TASK_NAME = 'gatlingLogback'

    public static def GATLING_RUN_TASK_NAME = 'gatlingRun'

    static String GATLING_TASK_NAME_PREFIX = "$GATLING_RUN_TASK_NAME-"

    public static def ENTERPRISE_PACKAGE_TASK_NAME = "gatlingEnterprisePackage"

    public static def ENTERPRISE_UPLOAD_TASK_NAME = "gatlingEnterpriseUpload"

    public static def ENTERPRISE_START_TASK_NAME = "gatlingEnterpriseStart"

    /**
     * @deprecated Please use {@link io.gatling.gradle.GatlingPlugin#ENTERPRISE_PACKAGE_TASK_NAME} instead
     */
    public static def FRONTLINE_JAR_TASK_NAME = "frontLineJar"

    void apply(Project project) {

        if (GradleVersion.current() < GradleVersion.version("7.0")) {
            throw new GradleException("Current Gradle version (${GradleVersion.current().version}) is unsupported. Minimal supported version is 7.0")
        }

        project.pluginManager.apply ScalaPlugin

        GatlingPluginExtension gatlingExt = project.extensions.create(GATLING_EXTENSION_NAME, GatlingPluginExtension)

        createConfiguration(project, gatlingExt)

        project.tasks.register(GATLING_LOGBACK_TASK_NAME, LogbackConfigTask.class) {
            dependsOn(project.tasks.named("gatlingClasses"))
            description = "Prepare logback config"
            group = "Gatling"
        }

        registerGatlingTask(project, GATLING_RUN_TASK_NAME, null)

        project.tasks.addRule("Pattern: $GATLING_RUN_TASK_NAME-<SimulationClass>: Executes single Gatling simulation.") { String taskName ->
            if (taskName.startsWith(GATLING_TASK_NAME_PREFIX)) {
                registerGatlingTask(project, taskName, (taskName - GATLING_TASK_NAME_PREFIX))
            }
        }

        def gatlingEnterprisePackage = registerEnterprisePackageTask(project)
        registerEnterpriseUploadTask(project, gatlingEnterprisePackage)
        registerEnterpriseStartTask(project, gatlingEnterprisePackage)

        project.dependencies {
            constraints {
                zinc("org.apache.logging.log4j:log4j-core") {
                    version {
                        require "2.17.1"
                    }
                    because 'log4shell'
                }
            }
        }

        project.afterEvaluate {
            if (project.plugins.findPlugin('io.gatling.frontline.gradle')) {
                def errorMessage = """\
                    Plugin io.gatling.frontline.gradle is no longer needed, its functionality is now included in the io.gatling.gradle plugin.
                    Please remove io.gatling.frontline.gradle from your build.gradle configuration file, and use the $ENTERPRISE_PACKAGE_TASK_NAME task instead of $FRONTLINE_JAR_TASK_NAME.
                    See https://gatling.io/docs/gatling/reference/current/extensions/gradle_plugin/ for more information.""".stripIndent()
                throw new ProjectConfigurationException(errorMessage, [])
            } else {
                project.tasks.register(FRONTLINE_JAR_TASK_NAME) {
                    doFirst {
                        logger.warn("""\
                            Task $FRONTLINE_JAR_TASK_NAME is deprecated and will be removed in a future version.
                            Please use $ENTERPRISE_PACKAGE_TASK_NAME instead.
                            See https://gatling.io/docs/gatling/reference/current/extensions/gradle_plugin/ for more information.""".stripIndent())
                    }
                    finalizedBy(gatlingEnterprisePackage)
                }
            }
        }
    }

    void registerGatlingTask(Project project, String taskName, String simulationFQN) {
        project.tasks.register(taskName, GatlingRunTask.class) {
            dependsOn(project.tasks.named("gatlingClasses"), project.tasks.named("gatlingLogback"))
            description = "Execute Gatling simulation"
            group = "Gatling"

            if (simulationFQN) {
                simulations = {
                    include(
                        "${simulationFQN.replace('.', '/')}.java",
                        "${simulationFQN.replace('.', '/')}.scala",
                        "${simulationFQN.replace('.', '/')}.kt"
                    )
                }
            }
        }
    }

    void registerEnterpriseUploadTask(Project project, TaskProvider<GatlingEnterprisePackageTask> gatlingEnterprisePackageTask) {
        project.tasks.register(ENTERPRISE_UPLOAD_TASK_NAME, GatlingEnterpriseUploadTask.class) {
            inputs.files gatlingEnterprisePackageTask
            dependsOn(gatlingEnterprisePackageTask)
        }
    }

    void registerEnterpriseStartTask(Project project, TaskProvider<GatlingEnterprisePackageTask> gatlingEnterprisePackageTask) {
        project.tasks.register(ENTERPRISE_START_TASK_NAME, GatlingEnterpriseStartTask.class) {
            inputs.files gatlingEnterprisePackageTask
            dependsOn(gatlingEnterprisePackageTask)
        }
    }

    TaskProvider<GatlingEnterprisePackageTask> registerEnterprisePackageTask(Project project) {
        TaskProvider<GatlingEnterprisePackageTask> gatlingEnterprisePackage = project.tasks.register(ENTERPRISE_PACKAGE_TASK_NAME, GatlingEnterprisePackageTask.class) {packageTask ->
            packageTask.configurations = [
                project.configurations.gatlingRuntimeClasspath
            ]
        }
        gatlingEnterprisePackage
    }

    void createConfiguration(Project project, GatlingPluginExtension gatlingExt) {
        project.sourceSets {
            gatling {
                java.srcDirs = [gatlingExt.JAVA_SIMULATIONS_DIR]
                scala.srcDirs = [gatlingExt.SCALA_SIMULATIONS_DIR]
                resources.srcDirs = [gatlingExt.RESOURCES_DIR]
            }
            if (gatling.hasProperty("kotlin")) {
                gatling {
                    kotlin.srcDirs = [gatlingExt.KOTLIN_SIMULATIONS_DIR]
                }
            }
        }

        project.configurations {
            gatling { visible = false }
            gatlingImplementation.extendsFrom(gatling)
        }

        project.dependencies {
            gatlingRuntimeOnly project.sourceSets.gatling.output
        }

        project.tasks.named("compileGatlingScala").configure {
            scalaCompileOptions.with {
                additionalParameters = [
                    "-deprecation",
                    "-feature",
                    "-unchecked",
                    "-language:implicitConversions",
                    "-language:postfixOps"
                ]
            }
        }

        project.afterEvaluate { Project evaluatedProject ->
            evaluatedProject.dependencies {
                def evaluatedExt = evaluatedProject.extensions.getByType(GatlingPluginExtension)

                gatlingImplementation "org.scala-lang:scala-library:${evaluatedExt.scalaVersion}"
                gatling "io.gatling.highcharts:gatling-charts-highcharts:${evaluatedExt.gatlingVersion}"

                if (evaluatedExt.includeMainOutput) {
                    gatlingImplementation evaluatedProject.sourceSets.main.output
                }
                if (evaluatedExt.includeTestOutput) {
                    gatlingImplementation evaluatedProject.sourceSets.test.output
                }
            }
        }
    }
}
