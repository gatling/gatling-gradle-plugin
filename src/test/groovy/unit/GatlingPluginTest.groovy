package unit

import helper.GatlingUnitSpec
import io.gatling.gradle.GatlingPluginExtension
import io.gatling.gradle.GatlingRunTask
import io.gatling.gradle.LogbackConfigTask
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.language.jvm.tasks.ProcessResources

import static io.gatling.gradle.GatlingPlugin.GATLING_LOGBACK_TASK_NAME
import static io.gatling.gradle.GatlingPlugin.GATLING_RUN_TASK_NAME

class GatlingPluginTest extends GatlingUnitSpec {

    def "should create gatling configurations"() {
        expect:
        ['gatling', 'gatlingCompile', 'gatlingRuntime', 'gatlingImplementation', 'gatlingRuntimeOnly'].every {
            project.configurations.getByName(it) != null
        }
    }

    def "should create gatling extension for project "() {
        expect:
        with(gatlingExt) {
            it instanceof GatlingPluginExtension
            it.simulations == DEFAULT_SIMULATIONS
            it.jvmArgs == DEFAULT_JVM_ARGS
            it.systemProperties == DEFAULT_SYSTEM_PROPS
        }
    }

    def "should add gatling dependencies with default version"() {
        when:
        project.evaluate()
        then:
        project.configurations.getByName("gatling").allDependencies.find {
            it.name == "gatling-charts-highcharts" && it.version == GatlingPluginExtension.GATLING_TOOL_VERSION
        }
        project.configurations.getByName("gatlingImplementation").allDependencies.find {
            it.name == "scala-library" && it.version == GatlingPluginExtension.SCALA_VERSION
        }
        project.configurations.getByName("implementation").allDependencies.find {
            it.name == "scala-library" && it.version == GatlingPluginExtension.SCALA_VERSION
        }
    }

    def "should allow overriding gatling version via extension"() {
        when:
        project.gatling { toolVersion = '3.0.0-RC1' }
        and:
        project.evaluate()
        then:
        project.configurations.getByName("gatling").allDependencies.find {
            it.name == "gatling-charts-highcharts" && it.version == "3.0.0-RC1"
        }
    }

    def "should allow overriding scala version via extension"() {
        when:
        project.gatling { scalaVersion = '2.11.3' }
        and:
        project.evaluate()
        then:
        project.configurations.getByName("gatlingImplementation").allDependencies.find {
            it.name == "scala-library" && it.version == "2.11.3"
        }
        project.configurations.getByName("implementation").allDependencies.find {
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
            it.simulations == null
            it.jvmArgs == null
            it.systemProperties == null
            it.dependsOn.size() == 1 && it.dependsOn.first() instanceof Collection
            it.dependsOn.first()*.name.sort() == ["gatlingClasses", "gatlingLogback"]
        }
    }

    def "should create processGatlingResources task"() {
        expect:
        with(project.tasks.getByName("processGatlingResources")) {
            it instanceof ProcessResources
        }
    }

    def "should add mavenCentral if no repositories declared"() {
        when:
        project.evaluate()
        then:
        project.repositories.size() == 1
        with(project.repositories.first()) {
            it instanceof MavenArtifactRepository
            it.name == "gatlingMavenCentral"
            it.url.toString() == "https://repo.maven.apache.org/maven2/"
        }
    }

    def "should add mavenCentral if repo with gatling not declared"() {
        given: "maven repo without gatling"
        project.repositories {
            maven {
                url "https://packages.confluent.io/maven/"
            }
        }
        when:
        project.evaluate()
        then:
        project.repositories.size() == 2
        project.repositories.any {
            it instanceof MavenArtifactRepository && it.url.toString() == "https://repo.maven.apache.org/maven2/" && it.name == "gatlingMavenCentral"
        }
    }

    def "should not add mavenCentral if repo with gatling declared"() {
        given: "maven repo with gatling"
        project.repositories {
            jcenter()
        }
        when:
        project.evaluate()
        then:
        project.repositories.size() == 1
        with(project.repositories.first()) {
            it instanceof MavenArtifactRepository
            it.url.toString() == "https://jcenter.bintray.com/"
        }
    }

    def "should not add mavenCentral if already declared but gatling not resolved because of user error"() {
        given: "maven central declared"
        project.repositories {
            mavenCentral(name: "myMaven")
        }
        and: "incorrect gatling config"
        project.gatling {
            toolVersion = "000.000.000"
        }
        when:
        project.evaluate()
        then:
        project.repositories.size() == 1
        project.repositories.first().name == "myMaven"
    }

    def "should add 2nd mavenCentral if 1st one has content filtering preventing gatling resolution"() {
        given: "maven central with gatling exclusion"
        project.repositories {
            mavenCentral {
                name = "myMaven"
                content { excludeGroup "io.gatling" }
            }
        }
        when:
        project.evaluate()
        then:
        project.repositories.size() == 2
        project.repositories.any {
            it instanceof MavenArtifactRepository && it.url.toString() == "https://repo.maven.apache.org/maven2/" && it.name == "gatlingMavenCentral"
        }
    }
}
