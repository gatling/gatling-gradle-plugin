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
package io.gatling.gradle;

import io.gatling.plugin.EnterprisePlugin;
import io.gatling.plugin.model.RunComment;
import io.gatling.plugin.model.RunSummary;
import io.gatling.plugin.model.SimulationEndResult;
import io.gatling.plugin.util.WebAppUrlRenderer;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

@DisableCachingByDefault(
    because = "Deploying a package on Gatling Enterprise is not a cacheable task")
public class GatlingEnterpriseStartTask extends GatlingEnterpriseDeployTask {

  // overrides the parent action instead of declaring a new one, otherwise Gradle would run both
  // and deploy twice
  @Override
  @TaskAction
  public void deploy() throws Exception {
    super.deploy();

    GatlingPluginExtension.Enterprise enterprise = gatlingExt.getEnterprise();
    boolean waitForRunEnd = enterprise.getWaitForRunEnd();
    EnterprisePlugin enterprisePlugin = enterprise.initEnterprisePlugin(getLogger());

    RecoverEnterprisePluginException.handle(
        this,
        () -> {
          RunComment runComment =
              new RunComment(enterprise.getRunTitle(), enterprise.getRunDescription());
          RunSummary runSummary =
              enterprisePlugin.startSimulation(
                  enterprise.getSimulationName(), deploymentInfo, runComment);
          getLogger()
              .lifecycle(
                  "\nSimulation successfully started.\nReports are available at: "
                      + WebAppUrlRenderer.toWebAppUrl(
                          enterprise.getWebAppUrl(), runSummary.reportsUrl)
                      + "\n");
          if (waitForRunEnd) {
            SimulationEndResult finishedRun = enterprisePlugin.waitForRunEnd(runSummary);
            if (!Boolean.TRUE.equals(finishedRun.successful)) {
              throw new GradleException("Simulation failed.");
            }
          }
          return null;
        });
  }
}
