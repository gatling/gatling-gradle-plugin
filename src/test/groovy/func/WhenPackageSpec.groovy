package func

import helper.GatlingFuncSpec
import org.gradle.testkit.runner.BuildResult
import spock.lang.Unroll

import static io.gatling.gradle.GatlingPlugin.ENTERPRISE_PACKAGE_TASK_NAME
import static io.gatling.gradle.GatlingPlugin.FRONTLINE_JAR_TASK_NAME
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class WhenPackageSpec extends GatlingFuncSpec {

    @Unroll
    def "should successfully create a package for gradle version #gradleVersion"() {
        setup:
        prepareTest()
        when:
        BuildResult result = createRunner(ENTERPRISE_PACKAGE_TASK_NAME)
            .withGradleVersion(gradleVersion)
            .build()
        then: "default tasks were executed successfully"
        result.task(":$ENTERPRISE_PACKAGE_TASK_NAME").outcome == SUCCESS
        result.task(":gatlingClasses").outcome == SUCCESS
        def artifactId = projectDir.root.getName()
        def artifact = new File(buildDir, "libs/${artifactId}-tests.jar")
        and: "artifact was created"
        artifact.isFile()
        where:
        gradleVersion << ["5.0", "5.6.4", "6.0", "6.3", "6.4.1", "6.9.1", "7.0", "7.3.3"]
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
        def artifact = new File(buildDir, "libs/${artifactId}-tests.jar")
        and: "artifact was created"
        artifact.isFile()
    }
}
