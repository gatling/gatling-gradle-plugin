package unit

import helper.GatlingUnitSpec
import io.gatling.gradle.GatlingPluginExtension
import io.gatling.gradle.GatlingRunTask
import io.gatling.gradle.LogbackConfigTask
import io.gatling.plugin.GatlingConstants
import org.gradle.api.tasks.TaskProvider
import org.gradle.language.jvm.tasks.ProcessResources

import static io.gatling.gradle.GatlingPlugin.GATLING_LOGBACK_TASK_NAME
import static io.gatling.gradle.GatlingPlugin.GATLING_RUN_TASK_NAME

class GatlingPluginTest extends GatlingUnitSpec {

    def "should create gatling configurations"() {
        expect:
        ['gatling', 'gatlingCompileOnly', 'gatlingImplementation', 'gatlingRuntimeOnly'].every {
            project.configurations.getByName(it) != null
        }
    }

    def "should create gatling extension for project "() {
        expect:
        with(gatlingExt) {
            it instanceof GatlingPluginExtension
            it.jvmArgs == GatlingConstants.DEFAULT_JVM_OPTIONS_GATLING
            it.systemProperties == DEFAULT_SYSTEM_PROPS
        }
    }

    def "should add gatling dependencies with default version"() {
        when:
        project.evaluate()
        then:
        project.configurations.gatling.allDependencies.find {
            it.name == "gatling-charts-highcharts" && it.version == GatlingPluginExtension.GATLING_VERSION
        }
        project.configurations.gatlingImplementation.allDependencies.find {
            it.name == "scala-library" && it.version == GatlingPluginExtension.SCALA_VERSION
        }
    }

    def "should allow overriding gatling version via extension"() {
        when:
        project.gatling { gatlingVersion = '3.5.1' }
        and:
        project.evaluate()
        then:
        project.configurations.gatling.allDependencies.find {
            it.name == "gatling-charts-highcharts" && it.version == "3.5.1"
        }
    }

    def "should allow overriding scala version via extension"() {
        when:
        project.gatling { scalaVersion = '2.11.3' }
        and:
        project.evaluate()
        then:
        project.configurations.gatlingImplementation.allDependencies.find {
            it.name == "scala-library" && it.version == "2.11.3"
        }
    }

    def "should create gatlingLogback task"() {
        expect:
        with(project.tasks.getByName(GATLING_LOGBACK_TASK_NAME)) {
            it instanceof LogbackConfigTask
            it.dependsOn.size() == 1
            it.dependsOn.first().name == "gatlingClasses"
        }
    }

    def "should create gatlingRun task"() {
        expect:
        with(project.tasks.getByName(GATLING_RUN_TASK_NAME)) {
            it instanceof GatlingRunTask
            it.jvmArgs == null
            it.systemProperties == null
            it.dependsOn.size() == 2
            def taskNames = it.dependsOn.collect {TaskProvider element -> return element.name}
            taskNames.contains("gatlingClasses")
            taskNames.contains("gatlingLogback")
        }
    }

    def "should create processGatlingResources task"() {
        expect:
        with(project.tasks.processGatlingResources) {
            it instanceof ProcessResources
        }
    }
}
