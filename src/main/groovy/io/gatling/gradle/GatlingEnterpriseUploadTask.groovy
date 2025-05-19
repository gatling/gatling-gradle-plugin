package io.gatling.gradle


import kotlin.NotImplementedError
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.TaskAction

@CacheableTask
class GatlingEnterpriseUploadTask extends DefaultTask {

    @TaskAction
    void publish() {
        throw new NotImplementedError(
            "The enterprise upload command is no longer supported. It has been replaced by the enterprise deploy command." +
                " Refer to the documentation for more information: https://docs.gatling.io/reference/integrations/build-tools/gradle-plugin/#deploying-on-gatling-enterprise"
        )
    }
}
