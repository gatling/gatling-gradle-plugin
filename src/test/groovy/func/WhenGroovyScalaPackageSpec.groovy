package func

import helper.GatlingFuncSpec
import org.gradle.testkit.runner.BuildResult
import spock.lang.Unroll

import static io.gatling.gradle.GatlingPlugin.ENTERPRISE_PACKAGE_TASK_NAME
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class WhenGroovyScalaPackageSpec extends GatlingFuncSpec {

    def setup() {
        prepareGroovyTestWithScala("/gradle-layout")
    }

    @Unroll
    def "should successfully create a package for gradle version #gradleVersion"() {
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
        gradleVersion << SUPPORTED_GRADLE_VERSIONS
    }
}
