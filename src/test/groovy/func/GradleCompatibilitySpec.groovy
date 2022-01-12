package func

import helper.GatlingFuncSpec
import org.gradle.testkit.runner.BuildResult
import spock.lang.Unroll

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class GradleCompatibilitySpec extends GatlingFuncSpec {

    BuildResult executeGradleTaskWithVersion(String task, String gradleVersion, boolean shouldFail) {
        def runner = createRunner(task).withGradleVersion(gradleVersion)
        if (shouldFail) {
            return runner.buildAndFail()
        } else {
            return runner.build()
        }
    }

    @Unroll
    void 'should succeed for version #gradleVersion that is greater than 5.0'() {
        given:
        prepareTest()
        when:
        BuildResult result = executeGradleTaskWithVersion('tasks', gradleVersion, false)
        then:
        result.task(":tasks").outcome == SUCCESS
        where:
        gradleVersion << ["5.0", "5.6.4", "6.0", "6.3", "6.4.1", "6.9.1", "7.0", "7.3.3"]
    }

    @Unroll
    void 'should fail for version #gradleVersion that is less than 5.0'() {
        given:
        prepareTest()
        when:
        BuildResult result = executeGradleTaskWithVersion('tasks', gradleVersion, true)
        then:
        result.output.contains("Current Gradle version (${gradleVersion}) is unsupported. Minimal supported version is 5.0")
        where:
        gradleVersion << ["4.0.1", "4.10.2", "3.5", "3.0", "2.9"]
    }
}
