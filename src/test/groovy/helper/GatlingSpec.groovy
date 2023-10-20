package helper

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static org.apache.commons.io.FileUtils.copyDirectory

abstract class GatlingSpec extends Specification {
    @Rule
    public final TemporaryFolder projectDir = new TemporaryFolder()

    File srcDir

    File buildDir

    File buildFile

    def createBuildFolder(String fixtureDir) {
        if (fixtureDir) {
            copyDirectory(new File(this.class.getResource(fixtureDir).file), projectDir.root)
        }
        srcDir = new File(projectDir.root, "src/gatling/scala")
        buildDir = new File(projectDir.root, "build")
    }

    def generateBuildScripts() {
        buildFile = projectDir.newFile("build.gradle")
        buildFile.text = """
plugins {
  id 'io.gatling.gradle'
}

repositories {
    mavenCentral()
}

dependencies {
  gatling group: 'commons-lang', name: 'commons-lang', version: '2.6'
  gatling group: 'org.json4s', name: 'json4s-jackson_2.13', version: '3.6.10'
}
"""
    }

    def generateKotlinBuildScripts() {
        buildFile = projectDir.newFile("build.gradle.kts")
        buildFile.text = """
plugins {
  id("io.gatling.gradle")
}

repositories {
  mavenCentral()
}

dependencies {
  gatling("commons-lang:commons-lang:2.6")
  gatling("org.json4s:json4s-jackson_2.13:3.6.10")
}
"""
    }
}
