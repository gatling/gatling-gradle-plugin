package helper

import io.gatling.gradle.GatlingPluginExtension
import io.gatling.gradle.GatlingRunTask
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

import static io.gatling.gradle.GatlingPlugin.GATLING_RUN_TASK_NAME

abstract class GatlingUnitSpec extends GatlingSpec {
    Project project

    GatlingPluginExtension gatlingExt

    def setup() {
        createBuildFolder("/gradle-layout")

        project = ProjectBuilder.builder().withProjectDir(projectDir.root).build()
        project.pluginManager.apply 'io.gatling.gradle'
        project.repositories { mavenCentral(name: "gatlingMavenCentral") }

        gatlingExt = project.extensions.getByType(GatlingPluginExtension)
    }

}
