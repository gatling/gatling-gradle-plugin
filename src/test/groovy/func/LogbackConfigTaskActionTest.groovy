package func

import groovy.util.slurpersupport.GPathResult
import helper.GatlingFuncSpec
import org.gradle.testkit.runner.BuildResult

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class LogbackConfigTaskActionTest extends GatlingFuncSpec {

    static def PROCESS_GATLING_RESOURCES_TASK_NAME = "processGatlingResources"

    File logbackConfig

    XmlSlurper xml = new XmlSlurper()

    def setup() {
        prepareTest()
        logbackConfig = new File(buildDir, "resources/gatling/logback.xml".toString())
    }

    private def rootLevel(GPathResult gpath) {
        gpath.root.@level
    }

    private def httpLevel(GPathResult gpath) {
        gpath.depthFirst().find { logger ->
            logger.@name == 'io.gatling.http.engine.response'
        }.@level
    }

    def "should create sample logback using extension logLevel"() {
        when:
        BuildResult result = executeGradle(PROCESS_GATLING_RESOURCES_TASK_NAME)
        then:
        result.task(":$PROCESS_GATLING_RESOURCES_TASK_NAME").outcome == SUCCESS
        and:
        logbackConfig.exists()
        and:
        rootLevel(xml.parse(logbackConfig)) == "WARN"
    }

    def "should set root logger level via logLevel extension"() {
        given:
        buildFile << 'gatling { logLevel = "QQQQ" }'
        when:
        BuildResult result = executeGradle(PROCESS_GATLING_RESOURCES_TASK_NAME)
        then:
        result.task(":$PROCESS_GATLING_RESOURCES_TASK_NAME").outcome == SUCCESS
        and:
        logbackConfig.exists()
        and:
        rootLevel(xml.parse(logbackConfig)) == "QQQQ"
    }

    def "should not set HTTP logger level via logHttp extension when value is NONE"() {
        given:
        buildFile << 'gatling { logHttp = "NONE" }'
        when:
        BuildResult result = executeGradle(PROCESS_GATLING_RESOURCES_TASK_NAME)
        then:
        result.task(":$PROCESS_GATLING_RESOURCES_TASK_NAME").outcome == SUCCESS
        and:
        logbackConfig.exists()
        and:
        xml.parse(logbackConfig).logger.findAll().isEmpty()
    }

    def "should set HTTP logger level to TRACE via logHttp extension when value is ALL"() {
        given:
        buildFile << 'gatling { logHttp = "ALL" }'
        when:
        BuildResult result = executeGradle(PROCESS_GATLING_RESOURCES_TASK_NAME)
        then:
        result.task(":$PROCESS_GATLING_RESOURCES_TASK_NAME").outcome == SUCCESS
        and:
        logbackConfig.exists()
        and:
        httpLevel(xml.parse(logbackConfig)) == "TRACE"
    }

    def "should set HTTP logger level to DEBUG via logHttp extension when value is FAILURES"() {
        given:
        buildFile << 'gatling { logHttp = "FAILURES" }'
        when:
        BuildResult result = executeGradle(PROCESS_GATLING_RESOURCES_TASK_NAME)
        then:
        result.task(":$PROCESS_GATLING_RESOURCES_TASK_NAME").outcome == SUCCESS
        and:
        logbackConfig.exists()
        and:
        httpLevel(xml.parse(logbackConfig)) == "DEBUG"
    }

    def "should not create sample logback.xml when it exists"() {
        given: ""
        new File(projectDir.root, "src/gatling/resources/logback.xml") << """<fakeLogback attr="value"/>"""
        when:
        BuildResult result = executeGradle(PROCESS_GATLING_RESOURCES_TASK_NAME)
        then:
        result.task(":$PROCESS_GATLING_RESOURCES_TASK_NAME").outcome == SUCCESS
        and:
        logbackConfig.exists()
        and:
        xml.parse(logbackConfig).@attr == "value"
    }
}
