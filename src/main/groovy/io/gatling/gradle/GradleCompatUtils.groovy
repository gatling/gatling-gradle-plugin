package io.gatling.gradle

import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.util.GradleVersion

/** Provides compatibility adapters for different supported versions of Gradle. */
class GradleCompatUtils {
    private static boolean isFiveTwoOrNewer() {
        return GradleVersion.current().baseVersion >= GradleVersion.version("5.2")
    }

    static setClassifier(AbstractArchiveTask archiveTask, String classifier) {
        // 'AbstractArchiveTask#classifier' deprecated in Gradle 5.2, removed in 8.0
        // See https://docs.gradle.org/8.0/userguide/upgrading_version_7.html#abstractarchivetask_api_cleanup
        if (isFiveTwoOrNewer()) {
            archiveTask.archiveClassifier.set(classifier)
        } else {
            archiveTask.classifier = classifier
        }
    }
}
