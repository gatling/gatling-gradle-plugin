package io.gatling.gradle

import io.gatling.plugin.util.EnterpriseClient
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.TaskAction

@CacheableTask
class GatlingEnterpriseUploadTask extends DefaultTask {

    @TaskAction
    void publish() {
        def gatling = project.extensions.getByType(GatlingPluginExtension)
        EnterpriseClient enterpriseClient = gatling.enterprise.getEnterpriseClient(project.version.toString())
        enterpriseClient.uploadPackage(gatling.enterprise.packageId, inputs.files.singleFile)
    }
}
