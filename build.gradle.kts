import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish") version "0.12.0"
    groovy //TODO remove it at the end of the migration
    id("com.adarshr.test-logger") version "2.1.1" //TODO latest version incompatible with existing groovy version
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("io.gatling:gatling-enterprise-plugin-commons:1.5.5") {
        exclude("com.fasterxml.jackson.core")
    }
    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.3")
    implementation("org.apache.ant:ant:1.10.11")

    testImplementation("org.spockframework:spock-core:1.3-groovy-2.5") {
        exclude("groovy-all")
    }
    testImplementation("com.github.stefanbirkner:system-rules:1.19.0") {
        exclude("junit-dep")
    }
    testImplementation(gradleTestKit())
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("commons-io:commons-io:2.11.0")
}

testlogger {
    setTheme("standard-parallel")
}

tasks{
    test {
        testLogging {
            events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
            exceptionFormat = TestExceptionFormat.FULL
            showStandardStreams = false
        }
        maxParallelForks = Runtime.getRuntime().availableProcessors().div(2).takeIf { it > 0 } ?: 1
    }
    validatePlugins{
        failOnWarning.set(false)
    }
}

gradlePlugin {
    plugins {
        create("gatlingPlugin") {
            id = "io.gatling.gradle"
            implementationClass = "io.gatling.gradle.GatlingPlugin"
            displayName = "Gatling plugin for Gradle"
            description = "Gatling plugin for Gradle"
        }
    }
}

pluginBundle {
    website = "https://github.com/gatling/gatling-gradle-plugin"
    vcsUrl = "https://github.com/gatling/gatling-gradle-plugin"
    tags = listOf("gatling", "load test", "stress test", "performance test", "java", "scala", "kotlin")
}
