package func

import helper.GatlingFuncSpec
import io.gatling.gradle.LogHttp
import io.gatling.gradle.LogbackConfigTask
import org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.BuildResult

import static io.gatling.gradle.GatlingPlugin.GATLING_RUN_TASK_NAME
import static java.lang.System.lineSeparator
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE

class WhenRunSimulationSpec extends GatlingFuncSpec {

    def "should execute all simulations by default"() {
        setup:
        prepareTest()
        when:
        BuildResult result = executeGradle(GATLING_RUN_TASK_NAME)
        then: "default tasks were executed successfully"
        result.task(":$GATLING_RUN_TASK_NAME").outcome == SUCCESS
        result.task(":gatlingClasses").outcome == SUCCESS
        and: "all simulations were run"
        def reports = new File(buildDir, "reports/gatling")
        reports.exists() && reports.listFiles().size() == 2
        and: "logs doesn't contain INFO"
        !result.output.split().any { it.contains("INFO") }
    }

    def "should execute only #simulation when initiated by rule"() {
        setup:
        prepareTest()
        when:
        BuildResult result = executeGradle("$GATLING_RUN_TASK_NAME-computerdatabase.BasicSimulation")
        then: "custom task was run successfully"
        result.task(":$GATLING_RUN_TASK_NAME-computerdatabase.BasicSimulation").outcome == SUCCESS
        and: "only one simulation was executed"
        new File(buildDir, "reports/gatling").listFiles().size() == 1
        and: "logs doesn't contain INFO"
        !result.output.split().any { it.contains("INFO") }
    }

    def "should allow Gatling config override"() {
        given:
        prepareTest()
        and: "override config by disabling reports"
        new File(new File(projectDir.root, "src/gatling/resources"), "gatling.conf") << """
gatling.charting.noReports = true
"""
        when:
        BuildResult result = executeGradle(GATLING_RUN_TASK_NAME)
        then:
        result.task(":$GATLING_RUN_TASK_NAME").outcome == SUCCESS
        and: "no reports generated"
        with(new File(buildDir, "reports/gatling").listFiles()) { reports ->
            reports.size() == 2
            reports.find { it.name.startsWith("basicsimulation") } != null
            reports.find { it.name.startsWith("basicsimulation") }.listFiles().collect { it.name } == ["simulation.log"]
            reports.find { it.name.startsWith("advancedsimulationstep03") } != null
            reports.find { it.name.startsWith("advancedsimulationstep03") }.listFiles().collect { it.name } == ["simulation.log"]

        }
    }

    def "should not fail when layout is incorrect"() {
        setup:
        prepareTest(null)
        when:
        BuildResult result = executeGradle(GATLING_RUN_TASK_NAME)
        then: "default tasks were executed successfully"
        result.task(":$GATLING_RUN_TASK_NAME").outcome == SUCCESS
        result.task(":gatlingClasses").outcome == UP_TO_DATE
        and: "no simulations compiled"
        !new File(buildDir, "classes/gatling").exists()
        and: "no simulations run"
        with(new File(buildDir, "reports/gatling")) {
            it.exists()
            it.list().size() == 0
        }
    }

    def "should not run gatling if no changes to source code"() {
        given:
        prepareTest()
        buildFile << """
gatling { simulations = { include 'computerdatabase/BasicSimulation.scala' } }
"""
        when: '1st time'
        BuildResult result = executeGradle("$GATLING_RUN_TASK_NAME")
        then:
        result.task(":compileGatlingScala").outcome == SUCCESS
        result.task(":$GATLING_RUN_TASK_NAME").outcome == SUCCESS

        when: '2nd time no changes'
        result = executeGradle("$GATLING_RUN_TASK_NAME")
        then:
        result.task(":compileGatlingScala").outcome == UP_TO_DATE
        result.task(":$GATLING_RUN_TASK_NAME").outcome == UP_TO_DATE

        when: '3r time with changes'
        new File(new File(srcDir, "computerdatabase"), "BasicSimulation.scala") << """
case class MyClz(str: String) // some fake code to change source file
"""
        result = executeGradle("$GATLING_RUN_TASK_NAME")
        then:
        result.task(":compileGatlingScala").outcome == SUCCESS
        result.task(":$GATLING_RUN_TASK_NAME").outcome == SUCCESS
    }

    def "should use extension for generated logback config when there is no resources"() {
        setup:
        prepareTest()
        and: "remove resources folder"
        FileUtils.deleteDirectory(new File(projectDir.root, "src/gatling/resources"))
        and: "configure custom log level"
        buildFile << """
gatling {
    logLevel = 'INFO'
    logHttp = 'ALL'
}"""
        when: 'run single simulation'
        BuildResult result = executeGradle("$GATLING_RUN_TASK_NAME-computerdatabase.BasicSimulation")
        then:
        result.task(":$GATLING_RUN_TASK_NAME-computerdatabase.BasicSimulation").outcome == SUCCESS
        and: "logback config created"
        LogbackConfigTask.logbackFile(buildDir).exists()
        and:
        with(result.output.split(lineSeparator())) { lines ->
            lines.findAll { it.contains("[TRACE]") }.every { it.contains("i.g.h.e.r.DefaultStatsProcessor") }
            lines.findAll { it.contains("[INFO ]") }.any { it.contains("i.g.") }
        }
    }

    def "should ignore logging settings from extension, if logback config exists in resources"() {
        setup:
        prepareTest()
        and: "put some config to gatling closure"
        buildFile << """
gatling {
    logLevel = 'INFO'
    logHttp = 'ALL'
}"""
        and: "create logback-test.xml"
        new File(projectDir.root, "src/gatling/resources/logback-test.xml") << """<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>@@@@ %level %logger]</pattern>
        </encoder>
        <immediateFlush>false</immediateFlush>
    </appender>
    <logger name="io.gatling" level="INFO"/>
    <root level="ERROR">
       <appender-ref ref="CONSOLE" />
    </root>
</configuration>"""

        when: 'run single simulation'
        BuildResult result = executeGradle("$GATLING_RUN_TASK_NAME-computerdatabase.BasicSimulation")
        then:
        result.task(":$GATLING_RUN_TASK_NAME-computerdatabase.BasicSimulation").outcome == SUCCESS
        and: "no logback config created"
        !LogbackConfigTask.logbackFile(buildDir).exists()
        and:
        with(result.output.split(lineSeparator()).findAll { it.startsWith("@@@@") }) { lines ->
            lines.every { it.contains("INFO") && it.contains("io.gatling") }
        }
    }
}
