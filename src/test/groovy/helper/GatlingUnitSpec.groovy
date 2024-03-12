package helper

import io.gatling.gradle.GatlingPluginExtension
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

abstract class GatlingUnitSpec extends GatlingSpec {
    Project project

    GatlingPluginExtension gatlingExt

    def setup() {
        createBuildFolder("/gradle-layout", SimulationLanguage.SCALA)

        project = ProjectBuilder.builder().withProjectDir(projectDir.root).build()
        project.pluginManager.apply 'scala'
        project.pluginManager.apply 'io.gatling.gradle'
        project.repositories { mavenCentral(name: "gatlingMavenCentral") }

        gatlingExt = project.extensions.getByType(GatlingPluginExtension)
    }
}
