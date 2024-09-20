package func

import helper.GatlingFuncSpec
import org.gradle.testkit.runner.BuildResult
import spock.lang.Unroll

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class GradleCompatibilitySpec extends GatlingFuncSpec {

    def setup() {
        prepareGroovyTestWithScala("/gradle-layout")
    }

    BuildResult executeGradleTaskWithVersion(String task, String gradleVersion, boolean shouldFail) {
        def runner = createRunner(task).withGradleVersion(gradleVersion)
        if (shouldFail) {
            return runner.buildAndFail()
        } else {
            return runner.build()
        }
    }

    @Unroll
    void 'should succeed for version #gradleVersion that is greater than 7.6'() {
        when:
        BuildResult result = executeGradleTaskWithVersion('tasks', gradleVersion, false)
        then:
        result.task(":tasks").outcome == SUCCESS
        where:
        gradleVersion << SUPPORTED_GRADLE_VERSIONS
    }

    @Unroll
    void 'should fail with friendly message for version #gradleVersion that is less than 7.6'() {
        when:
        BuildResult result = executeGradleTaskWithVersion('tasks', gradleVersion, true)
        then:
        result.output.contains("Current Gradle version (${gradleVersion}) is unsupported. Minimal supported version is 7.6")
        where:
        gradleVersion << ["6.9.4"]
    }
}
