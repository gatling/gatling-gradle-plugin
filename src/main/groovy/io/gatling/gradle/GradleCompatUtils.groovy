package io.gatling.gradle

import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.process.JavaExecSpec
import org.gradle.util.GradleVersion

/** Provides compatibility adapters for different supported versions of Gradle. */
class GradleCompatUtils {
    private static boolean isFiveTwoOrNewer() {
        return GradleVersion.current().baseVersion >= GradleVersion.version("5.2")
    }

    private static boolean isSevenZeroOrNewer() {
        return GradleVersion.current().baseVersion >= GradleVersion.version("7.0")
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

    static File getArchiveFile(AbstractArchiveTask archiveTask) {
        // 'AbstractArchiveTask#archivePath' deprecated in Gradle 5.2, scheduled for removal in 9.0
        // See https://docs.gradle.org/8.0/userguide/upgrading_version_7.html#abstractarchivetask_api_cleanup
        if (isFiveTwoOrNewer()) {
            return archiveTask.archiveFile.get().asFile
        } else {
            return archiveTask.getArchivePath()
        }
    }

    static setMainClass(JavaExecSpec exec, String mainClass) {
        // 'JavaExecSpec.main' deprecated in Gradle 7.1 (but replacement already stable in 7.0), scheduled for removal in 9.0
        // https://docs.gradle.org/8.0/userguide/upgrading_version_7.html#java_exec_properties
        if (isSevenZeroOrNewer()) {
            exec.mainClass.set(mainClass)
        } else {
            exec.main = mainClass
        }
    }
}
