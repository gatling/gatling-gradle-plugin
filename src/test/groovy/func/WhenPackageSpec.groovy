package func

import helper.GatlingFuncSpec
import org.gradle.testkit.runner.BuildResult

import static io.gatling.gradle.GatlingPlugin.ENTERPRISE_PACKAGE_TASK_NAME
import static io.gatling.gradle.GatlingPlugin.FRONTLINE_JAR_TASK_NAME
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class WhenPackageSpec extends GatlingFuncSpec {

    def "should create a package"() {
        setup:
        prepareTest()
        when:
        BuildResult result = executeGradle(ENTERPRISE_PACKAGE_TASK_NAME)
        then: "default tasks were executed successfully"
        result.task(":$ENTERPRISE_PACKAGE_TASK_NAME").outcome == SUCCESS
        result.task(":gatlingClasses").outcome == SUCCESS
        def artifactId = projectDir.root.getName()
        def artifact = new File(buildDir, "libs/${artifactId}.jar")
        and: "artifact was created"
        artifact.isFile()
    }

    def "should create a package using the legacy task name"() {
        setup:
        prepareTest()
        when:
        BuildResult result = executeGradle(FRONTLINE_JAR_TASK_NAME)
        then: "default tasks were executed successfully"
        result.task(":$FRONTLINE_JAR_TASK_NAME").outcome == SUCCESS
        result.task(":$ENTERPRISE_PACKAGE_TASK_NAME").outcome == SUCCESS
        result.task(":gatlingClasses").outcome == SUCCESS
        def artifactId = projectDir.root.getName()
        def artifact = new File(buildDir, "libs/${artifactId}.jar")
        and: "artifact was created"
        artifact.isFile()
    }
}
