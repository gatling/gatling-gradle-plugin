package io.gatling.gradle

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.util.GradleVersion

final class GatlingPlugin implements Plugin<Project> {

    public static def GATLING_EXTENSION_NAME = 'gatling'

    public static def GATLING_RUN_TASK_NAME = 'gatlingRun'

    public static def GATLING_RECORDER_TASK_NAME = 'gatlingRecorder'

    public static def ENTERPRISE_PACKAGE_TASK_NAME = "gatlingEnterprisePackage"

    public static def ENTERPRISE_UPLOAD_TASK_NAME = "gatlingEnterpriseUpload"

    public static def ENTERPRISE_START_TASK_NAME = "gatlingEnterpriseStart"

    public static def ENTERPRISE_DEPLOY_TASK_NAME = "gatlingEnterpriseDeploy"

    void apply(Project project) {
        validateGradleVersion()

        createConfiguration(project)

        project.tasks.register(GATLING_RECORDER_TASK_NAME, GatlingRecorderTask.class) {
            description = "Launch recorder"
            group = "Gatling"
        }

        project.tasks.register(GATLING_RUN_TASK_NAME, GatlingRunTask.class) {
            dependsOn(project.tasks.named("gatlingClasses"))
            description = "Execute Gatling simulations locally"
            group = "Gatling"
        }

        def gatlingEnterprisePackageTask = registerEnterprisePackageTask(project)
        registerEnterpriseUploadTask(project, gatlingEnterprisePackageTask)
        registerEnterpriseDeployTask(project, gatlingEnterprisePackageTask)
        registerEnterpriseStartTask(project, gatlingEnterprisePackageTask)
    }

    private void validateGradleVersion() {
        if (GradleVersion.current() < GradleVersion.version("7.1")) {
            throw new GradleException("Current Gradle version (${GradleVersion.current().version}) is unsupported. Minimal supported version is 7.1")
        }
    }

    private void registerEnterpriseUploadTask(Project project, TaskProvider<GatlingEnterprisePackageTask> gatlingEnterprisePackageTask) {
        project.tasks.register(ENTERPRISE_UPLOAD_TASK_NAME, GatlingEnterpriseUploadTask.class) {
            inputs.files gatlingEnterprisePackageTask
            dependsOn(gatlingEnterprisePackageTask)
        }
    }

    private TaskProvider<GatlingEnterpriseDeployTask> registerEnterpriseDeployTask(Project project, TaskProvider<GatlingEnterprisePackageTask> gatlingEnterprisePackageTask) {
        project.tasks.register(ENTERPRISE_DEPLOY_TASK_NAME, GatlingEnterpriseDeployTask.class) {
            inputs.files gatlingEnterprisePackageTask
            dependsOn(gatlingEnterprisePackageTask)
        }
    }

    private void registerEnterpriseStartTask(Project project, TaskProvider<GatlingEnterprisePackageTask> gatlingEnterprisePackageTask) {
        project.tasks.register(ENTERPRISE_START_TASK_NAME, GatlingEnterpriseStartTask.class) {
            inputs.files gatlingEnterprisePackageTask
            dependsOn(gatlingEnterprisePackageTask)
        }
    }

    private TaskProvider<GatlingEnterprisePackageTask> registerEnterprisePackageTask(Project project) {
        project.tasks.register(ENTERPRISE_PACKAGE_TASK_NAME, GatlingEnterprisePackageTask.class) {packageTask ->
            packageTask.archiveClassifier.set("tests")
            packageTask.configurations = [
                project.configurations.gatlingRuntimeClasspath
            ]
        }
    }

    private void createConfiguration(Project project) {
        def hasJava = project.pluginManager.hasPlugin("java")
        def hasScala = project.pluginManager.hasPlugin("scala")
        def hasKotlin = project.pluginManager.hasPlugin("kotlin")

        if (!hasJava && !hasScala && !hasKotlin) {
            throw new UnsupportedOperationException("You must configure the plugin for your language of choice: java, scala or kotlin.")
        }

        def gatlingExt = project.extensions.create(GATLING_EXTENSION_NAME, GatlingPluginExtension)

        project.sourceSets {
            gatling {
                resources.srcDirs = [gatlingExt.GATLING_RESOURCES_DIR]
                if (hasJava) {
                    java.srcDirs = [gatlingExt.GATLING_JAVA_SOURCES_DIR]
                }
                if (hasScala) {
                    scala.srcDirs = [gatlingExt.GATLING_SCALA_SOURCES_DIR]
                }
                if (hasKotlin) {
                    kotlin.srcDirs = [gatlingExt.GATLING_KOTLIN_SOURCES_DIR]
                }
            }
        }

        project.configurations {
            gatling { visible = false }
            gatlingImplementation.extendsFrom(gatling)
        }

        project.dependencies {
            gatlingRuntimeOnly project.sourceSets.gatling.output
            if (hasScala) {
                constraints {
                    zinc("org.apache.logging.log4j:log4j-core") {
                        version {
                            require "2.17.1"
                        }
                        because 'log4shell'
                    }
                }
            }
        }

        if (hasScala) {
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
        }

        project.afterEvaluate { Project evaluatedProject ->
            evaluatedProject.dependencies {
                def evaluatedExt = evaluatedProject.extensions.getByType(GatlingPluginExtension)

                if (hasScala) {
                    gatlingImplementation "org.scala-lang:scala-library:${evaluatedExt.scalaVersion}"
                }
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
