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

    protected static final List<String> SUPPORTED_GRADLE_VERSIONS = ["5.0", "5.6.4", "6.0", "6.3", "6.4.1", "6.9.1", "7.0", "7.3.3"]
}
