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

import io.gatling.plugin.EnterprisePlugin
import io.gatling.plugin.model.RunComment
import io.gatling.plugin.model.RunSummary
import io.gatling.plugin.model.SimulationEndResult
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

class GatlingEnterpriseStartTask extends GatlingEnterpriseDeployTask {

  @TaskAction
  void publish() {
    super.deploy()

    final boolean waitForRunEnd = gatlingExt.enterprise.waitForRunEnd
    final EnterprisePlugin enterprisePlugin = gatlingExt.enterprise.initEnterprisePlugin(logger)

    RecoverEnterprisePluginException.handle(logger) {
      final RunComment runComment = new RunComment(gatlingExt.enterprise.runTitle, gatlingExt.enterprise.runDescription)
      final RunSummary runSummary = enterprisePlugin.startSimulation(gatlingExt.enterprise.simulationName, deploymentInfo, runComment)
      logger.lifecycle("""
                         |Simulation successfully started.
                         |Reports are available at: ${gatlingExt.enterprise.webAppUrl.toExternalForm() + runSummary.reportsPath}
                         |""".stripMargin())
      if (waitForRunEnd) {
        SimulationEndResult finishedRun = enterprisePlugin.waitForRunEnd(runSummary)
        if (!finishedRun.status.successful) {
          throw new GradleException("Simulation failed.")
        }
      }
    }
  }
}
