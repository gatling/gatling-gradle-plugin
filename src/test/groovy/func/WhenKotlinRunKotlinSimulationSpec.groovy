package func

import helper.GatlingFuncSpec
import org.gradle.testkit.runner.BuildResult

import static io.gatling.gradle.GatlingPlugin.GATLING_RUN_TASK_NAME
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class WhenKotlinRunKotlinSimulationSpec extends GatlingFuncSpec {

    def "should execute only #simulation when forced by --simulation option"() {
        setup:
        prepareKotlinTestWitKotlin("/gradle-layout")
        when:
        BuildResult result = executeGradle(GATLING_RUN_TASK_NAME, "--non-interactive", "--simulation=example.BasicSimulation")
        then: "custom task was run successfully"
        result.task(":$GATLING_RUN_TASK_NAME").outcome == SUCCESS
        and: "only one simulation was executed"
        new File(buildDir, "reports/gatling").listFiles().size() == 1
        and: "logs doesn't contain INFO"
        !result.output.split().any { it.contains("INFO") }
    }
}
