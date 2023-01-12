package io.gatling.gradle

import org.gradle.api.invocation.Gradle
import org.gradle.util.GradleVersion

class GradleUtils {
    static boolean isGradleFiveTwoOrNewer(Gradle gradle) {
        return GradleVersion.current().baseVersion >= GradleVersion.version("5.2")
    }
}
