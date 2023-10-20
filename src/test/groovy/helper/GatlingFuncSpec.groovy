package helper

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner

abstract class GatlingFuncSpec extends GatlingSpec {

    static def GATLING_HOST_NAME_SYS_PROP = "-Dgatling.hostName=HTTP://COMPUTER-DATABASE.GATLING.IO"

    void prepareTest(String fixtureDir = "/gradle-layout") {
        createBuildFolder(fixtureDir)
        generateBuildScripts()
    }

    void prepareKotlinTest(String fixtureDir = "/gradle-layout") {
        createBuildFolder(fixtureDir)
        generateKotlinBuildScripts()
    }

    protected GradleRunner createRunner(String... gradleArgs) {
        GradleRunner.create().forwardOutput()
            .withProjectDir(projectDir.getRoot())
            .withArguments(["--stacktrace", GATLING_HOST_NAME_SYS_PROP] + (gradleArgs as List))
            .withPluginClasspath()
            .withDebug(true)
    }

    BuildResult executeGradle(String... gradleArgs) {
        createRunner(gradleArgs).build()
    }

    protected static final List<String> SUPPORTED_GRADLE_VERSIONS = ["7.0", "7.6.2", "8.0", "8.2.1", "8.4"]
}
