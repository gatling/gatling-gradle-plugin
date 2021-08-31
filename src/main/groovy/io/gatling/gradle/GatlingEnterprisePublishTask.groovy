package io.gatling.gradle

import io.gatling.plugin.util.EnterpriseClient
import io.gatling.plugin.util.OkHttpEnterpriseClient
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.TaskAction

@CacheableTask
class GatlingEnterprisePublishTask extends DefaultTask {

    @TaskAction
    void publish() {
        def gatling = project.extensions.getByType(GatlingPluginExtension)
        EnterpriseClient enterpriseClient = new OkHttpEnterpriseClient(gatling.enterprise.url, gatling.enterprise.apiToken)
        enterpriseClient.uploadPackage(gatling.enterprise.packageId, inputs.files.singleFile)
    }
}
