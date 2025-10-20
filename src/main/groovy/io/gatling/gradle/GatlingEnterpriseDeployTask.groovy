/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.gradle

import io.gatling.plugin.BatchEnterprisePlugin
import io.gatling.plugin.deployment.DeploymentConfiguration
import io.gatling.plugin.model.DeploymentInfo
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class GatlingEnterpriseDeployTask extends DefaultTask {

  protected DeploymentInfo deploymentInfo
  protected final GatlingPluginExtension gatlingExt = project.extensions.getByType(GatlingPluginExtension)
  protected final String artifactId = project.name
  protected final File projectDir = project.projectDir

  @TaskAction
  void deploy() {
    final BatchEnterprisePlugin enterprisePlugin = gatlingExt.enterprise.initBatchEnterprisePlugin(logger)
    final File descriptorFile = DeploymentConfiguration.fromBaseDirectory(projectDir, gatlingExt.enterprise.packageDescriptorFilename)
    final File packageFile = inputs.files.singleFile
    final Boolean isPrivateRepositoryEnabled = gatlingExt.enterprise.controlPlaneUrl != null
    final String validateSimulationId = gatlingExt.enterprise.validateSimulationId

    deploymentInfo = validateSimulationId != null
            ? enterprisePlugin.deployFromDescriptor(descriptorFile, packageFile, artifactId, isPrivateRepositoryEnabled, validateSimulationId)
            : enterprisePlugin.deployFromDescriptor(descriptorFile, packageFile, artifactId, isPrivateRepositoryEnabled)
  }
}
