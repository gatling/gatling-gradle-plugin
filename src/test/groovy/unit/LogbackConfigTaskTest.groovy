package unit

import groovy.util.slurpersupport.GPathResult
import helper.GatlingUnitSpec
import io.gatling.gradle.GatlingPluginExtension
import io.gatling.gradle.LogHttp
import io.gatling.gradle.LogbackConfigTask
import org.apache.commons.io.FileUtils
import spock.lang.Unroll

import static io.gatling.gradle.GatlingPlugin.GATLING_LOGBACK_TASK_NAME

class LogbackConfigTaskTest extends GatlingUnitSpec {

    LogbackConfigTask theTask

    File logbackConfig

    XmlSlurper xml = new XmlSlurper()

    private def rootLevel(GPathResult gpath) {
        gpath.root.@level
    }

    private def httpLevel(GPathResult gpath) {
        gpath.depthFirst().find { logger ->
            logger.@name == 'io.gatling.http.engine.response'
        }.@level
    }

    def setup() {
        theTask = project.tasks.getByName(GATLING_LOGBACK_TASK_NAME) as LogbackConfigTask
        logbackConfig = new File(buildDir, "resources/gatling/logback.xml".toString())
    }

    def "should create sample logback using default logLevel"() {
        when:
        theTask.generateLogbackConfig()
        then:
        logbackConfig.exists()
        and:
        rootLevel(xml.parse(logbackConfig)) == GatlingPluginExtension.DEFAULT_LOG_LEVEL
    }

    def "should set root logger level via logLevel extension"() {
        given:
        gatlingExt.logLevel = "QQQ"
        when:
        theTask.generateLogbackConfig()
        then:
        logbackConfig.exists()
        and:
        rootLevel(xml.parse(logbackConfig)) == gatlingExt.logLevel
    }

    def "should not set HTTP logger level via logHttp extension when value is NONE"() {
        given:
        gatlingExt.logHttp = LogHttp.NONE
        when:
        theTask.generateLogbackConfig()
        then:
        logbackConfig.exists()
        and:
        xml.parse(logbackConfig).logger.findAll().isEmpty()
    }

    def "should set HTTP logger level to TRACE via logHttp extension when value is ALL"() {
        given:
        gatlingExt.logHttp = LogHttp.ALL
        when:
        theTask.generateLogbackConfig()
        then:
        logbackConfig.exists()
        and:
        httpLevel(xml.parse(logbackConfig)) == "TRACE"
    }

    def "should set HTTP logger level to DEBUG via logHttp extension when value is FAILURES"() {
        given:
        gatlingExt.logHttp = LogHttp.FAILURES
        when:
        theTask.generateLogbackConfig()
        then:
        logbackConfig.exists()
        and:
        httpLevel(xml.parse(logbackConfig)) == "DEBUG"
    }

    @Unroll
    def "should not create logback.xml when logback config exists at #file"() {
        given:
        new File(projectDir.root, "src/gatling/resources/$logbackFile") << "arbitrary logback config file"
        when:
        theTask.generateLogbackConfig()
        then:
        !logbackConfig.exists()
        where:
        logbackFile << ["logback.xml", "logback-test.xml", "logback.groovy"]
    }

    @Unroll
    def "should not create logback.xml when logback config exists at custom resources, #file"() {
        given:
        def myFolder = projectDir.newFolder("src", "logback")
        and:
        new File(myFolder, logbackFile) << "arbitrary logback config file"
        and:
        project.sourceSets.gatling {
            resources.srcDirs "src/logback"
        }
        when:
        theTask.generateLogbackConfig()
        then:
        !logbackConfig.exists()
        where:
        logbackFile << ["logback.xml", "logback-test.xml", "logback.groovy"]
    }

    @Unroll
    def "should create logback.xml when logback config located not in the root of resources, #file"() {
        given:
        def myFolder = projectDir.newFolder("src", "gatling", "resources", "myfolder")
        new File(myFolder, logbackFile) << "arbitrary logback config file"
        when:
        theTask.generateLogbackConfig()
        then:
        logbackConfig.exists()
        where:
        logbackFile << ["logback.xml", "logback-test.xml", "logback.groovy"]
    }

    def "should create logback.xml when resources directory is empty"() {
        given:
        FileUtils.cleanDirectory(new File(projectDir.root, "src/gatling/resources"))
        when:
        theTask.generateLogbackConfig()
        then:
        logbackConfig.exists()
    }

    def "should create logback.xml when resources directory is missing"() {
        given:
        FileUtils.deleteDirectory(new File(projectDir.root, "src/gatling/resources"))
        when:
        theTask.generateLogbackConfig()
        then:
        logbackConfig.exists()
    }
}
