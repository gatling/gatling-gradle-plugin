package io.gatling.gradle

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.artifacts.result.UnresolvedDependencyResult
import org.gradle.api.plugins.scala.ScalaPlugin
import org.gradle.util.GradleVersion
import org.gradle.util.VersionNumber

class GatlingPlugin implements Plugin<Project> {

    public static def GATLING_EXTENSION_NAME = 'gatling'

    public static def GATLING_LOGBACK_TASK_NAME = 'gatlingLogback'

    public static def GATLING_RUN_TASK_NAME = 'gatlingRun'

    static String GATLING_TASK_NAME_PREFIX = "$GATLING_RUN_TASK_NAME-"

    public static def ENTERPRISE_PACKAGE_TASK_NAME = "gatlingEnterprisePackage"

    public static def ENTERPRISE_PUBLISH_TASK_NAME = "gatlingEnterprisePublish"

    /**
     * @deprecated Please use {@link io.gatling.gradle.GatlingPlugin#ENTERPRISE_PACKAGE_TASK_NAME} instead
     */
    public static def FRONTLINE_JAR_TASK_NAME = "frontLineJar"

    void apply(Project project) {

        if (VersionNumber.parse(GradleVersion.current().version).major < 5) {
            throw new GradleException("Current Gradle version (${GradleVersion.current().version}) is unsupported. Minimal supported version is 5.0")
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

        def gatlingEnterprisePackage = createEnterprisePackageTask(project)
        createEnterprisePublishTask(project, gatlingEnterprisePackage)

        project.afterEvaluate {
            if (project.plugins.findPlugin('io.gatling.frontline.gradle')) {
                def errorMessage = """\
                    Plugin io.gatling.frontline.gradle is no longer needed, its functionality is now included in the io.gatling.gradle plugin.
                    Please remove io.gatling.frontline.gradle from your build.gradle configuration file, and use the $ENTERPRISE_PACKAGE_TASK_NAME task instead of $FRONTLINE_JAR_TASK_NAME.
                    See https://gatling.io/docs/gatling/reference/current/extensions/gradle_plugin/ for more information.""".stripIndent()
                throw new ProjectConfigurationException(errorMessage, [])
            } else {
                def legacyFrontlineTask = project.tasks.create(name: FRONTLINE_JAR_TASK_NAME) {
                    doFirst {
                        getLogger().warn("""\
                            Task $FRONTLINE_JAR_TASK_NAME is deprecated and will be removed in a future version.
                            Please use $ENTERPRISE_PACKAGE_TASK_NAME instead.
                            See https://gatling.io/docs/gatling/reference/current/extensions/gradle_plugin/ for more information.""".stripIndent())
                    }
                }
                legacyFrontlineTask.finalizedBy(gatlingEnterprisePackage)
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

    void createEnterprisePublishTask(Project project, GatlingEnterprisePackageTask gatlingEnterprisePackageTask) {
        project.tasks.create(
            name: ENTERPRISE_PUBLISH_TASK_NAME,
            type: GatlingEnterprisePublishTask
        ) {
            inputs.files gatlingEnterprisePackageTask
            dependsOn(gatlingEnterprisePackageTask)
        }
    }

    GatlingEnterprisePackageTask createEnterprisePackageTask(Project project) {
        GatlingEnterprisePackageTask gatlingEnterprisePackage = project.tasks.create(name: ENTERPRISE_PACKAGE_TASK_NAME, type: GatlingEnterprisePackageTask)

        gatlingEnterprisePackage.conventionMapping.with {
            map("classifier") {
                "tests"
            }
        }

        gatlingEnterprisePackage.exclude(
            "META-INF/LICENSE",
            "META-INF/MANIFEST.MF",
            "META-INF/versions/**",
            "META-INF/maven/**",
            "**/*.SF",
            "**/*.DSA",
            "**/*.RSA"
        )

        gatlingEnterprisePackage.from(project.sourceSets.gatling.output)
        gatlingEnterprisePackage.configurations = [
            project.configurations.gatlingRuntimeClasspath
        ]
        gatlingEnterprisePackage.metaInf {
            def tempDir = new File(gatlingEnterprisePackage.getTemporaryDir(), "META-INF")
            def maven = new File(tempDir, "maven")
            maven.mkdirs()
            new File(maven,"pom.properties").text =
                """groupId=${project.group}
                |artifactId=${project.name}
                |version=${project.version}
                |""".stripMargin()
            new File(maven, "pom.xml").text =
                """<?xml version="1.0" encoding="UTF-8"?>
                |<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                |xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                |  <modelVersion>4.0.0</modelVersion>
                |  <groupId>${project.group}</groupId>
                |  <artifactId>${project.name}</artifactId>
                |  <version>${project.version}</version>
                |</project>
                |""".stripMargin()
            from (tempDir)
        }

        gatlingEnterprisePackage
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

        project.tasks.getByName("compileGatlingScala").configure {
            scalaCompileOptions.with {
                additionalParameters = [
                    "-target:jvm-1.8",
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

                implementation "org.scala-lang:scala-library:${evaluatedExt.scalaVersion}"
                gatlingImplementation "org.scala-lang:scala-library:${evaluatedExt.scalaVersion}"
                gatling "io.gatling.highcharts:gatling-charts-highcharts:${evaluatedExt.toolVersion}"

                if (evaluatedExt.includeMainOutput) {
                    gatlingImplementation evaluatedProject.sourceSets.main.output
                }
                if (evaluatedExt.includeTestOutput) {
                    gatlingImplementation evaluatedProject.sourceSets.test.output
                }
            }

            if (shouldAddMavenCentral(evaluatedProject)) {
                evaluatedProject.repositories {
                    mavenCentral(name: "gatlingMavenCentral")
                }
            }
        }
    }

    private static boolean shouldAddMavenCentral(Project project) {

        if (!project.repositories.isEmpty()) {
            // there are declared repositories, let's try to resolve gatling configuration
            def resolutionRes = project.configurations.gatling.copyRecursive().incoming.resolutionResult
            def deps = resolutionRes.allDependencies

            if (deps.any { it instanceof UnresolvedDependencyResult }) {
                // mavenCentral should be added only if dependencies from gatling configuration were not resolved
                if (deps.size() > 1) {
                    /**
                     some dependencies were resolved and some were not
                     it means that declared repositories either has content filter with gatling exclusion
                     or contains only few of gatling dependencies
                     adding mavenCentral that could resolve all of gatling and its transitive dependencies
                     */
                    return true
                } else if (deps.size() == 1 && project.repositories.every { it.url.toString() != "https://repo.maven.apache.org/maven2/" }) {
                    /**
                     none of dependencies were resolved
                     it could mean
                     - incorrect gatling version used
                     - maven repo is inaccessible (incorrect URL, temporary network issue or corporate policy)
                     adding mavenCentral only if there's no already declared mavenCentral()
                     */
                    return true
                }
            }
            return false
        } else {
            // there's no declared repositories, adding mavenCentral
            return true
        }
    }
}

