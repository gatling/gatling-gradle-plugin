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

import io.gatling.plugin.BatchEnterprisePlugin;
import io.gatling.plugin.ConfigurationConstants;
import io.gatling.plugin.EnterprisePlugin;
import io.gatling.plugin.EnterprisePluginProvider;
import io.gatling.plugin.GatlingConstants;
import io.gatling.plugin.PluginConfiguration;
import io.gatling.plugin.exceptions.EnterprisePluginException;
import io.gatling.plugin.exceptions.UnsupportedClientException;
import io.gatling.plugin.model.BuildTool;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.gradle.api.Action;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;

public class GatlingPluginExtension {

  public static final Map<String, Object> DEFAULT_SYSTEM_PROPS = Collections.emptyMap();

  public static final String GATLING_MAIN_CLASS = "io.gatling.app.Gatling";

  public static final String GATLING_RECORDER_CLASS = "io.gatling.recorder.GatlingRecorder";

  public static final String GATLING_JAVA_SOURCES_DIR = "src/gatling/java";

  public static final String GATLING_SCALA_SOURCES_DIR = "src/gatling/scala";

  public static final String GATLING_KOTLIN_SOURCES_DIR = "src/gatling/kotlin";

  public static final String GATLING_RESOURCES_DIR = "src/gatling/resources";

  public static final String GATLING_VERSION = "3.16.0";

  public static final String SCALA_VERSION = "2.13.18";

  private static boolean isNullOrEmpty(String s) {
    return s == null || s.isEmpty();
  }

  private static URL toUrl(String s) {
    try {
      return new URI(s).toURL();
    } catch (URISyntaxException | MalformedURLException e) {
      throw new InvalidUserDataException("Invalid URL: " + s, e);
    }
  }

  public static final class Enterprise {
    private String apiToken;
    private String simulationId;
    private String simulationName;
    private String runTitle;
    private String runDescription;
    private String packageId;
    private String apiUrl;
    private String webAppUrl;
    private boolean batchMode;
    private boolean waitForRunEnd;
    private String controlPlaneUrl;
    private String packageDescriptorFilename;
    private String validateSimulationId;

    public void setBatchMode(boolean batchMode) {
      this.batchMode = batchMode;
    }

    public void batchMode(boolean batchMode) {
      setBatchMode(batchMode);
    }

    public void setApiUrl(String apiUrl) {
      this.apiUrl = apiUrl;
    }

    public void apiUrl(String apiUrl) {
      setApiUrl(apiUrl);
    }

    public void setWebAppUrl(String webAppUrl) {
      this.webAppUrl = webAppUrl;
    }

    public void webAppUrl(String webAppUrl) {
      setWebAppUrl(webAppUrl);
    }

    public void setSimulationId(String simulationId) {
      this.simulationId = simulationId;
    }

    public void simulationId(String simulationId) {
      setSimulationId(simulationId);
    }

    public void setSimulationName(String simulationName) {
      this.simulationName = simulationName;
    }

    public void simulationName(String simulationName) {
      setSimulationName(simulationName);
    }

    public void setRunTitle(String runTitle) {
      this.runTitle = runTitle;
    }

    public void runTitle(String runTitle) {
      setRunTitle(runTitle);
    }

    public void setRunDescription(String runDescription) {
      this.runDescription = runDescription;
    }

    public void runDescription(String runDescription) {
      setRunDescription(runDescription);
    }

    public void setPackageId(String packageId) {
      this.packageId = packageId;
    }

    public void packageId(String packageId) {
      setPackageId(packageId);
    }

    public void setApiToken(String apiToken) {
      this.apiToken = apiToken;
    }

    public void apiToken(String apiToken) {
      setApiToken(apiToken);
    }

    public void setWaitForRunEnd(boolean waitForRunEnd) {
      this.waitForRunEnd = waitForRunEnd;
    }

    public void waitForRunEnd(boolean waitForRunEnd) {
      setWaitForRunEnd(waitForRunEnd);
    }

    public void setControlPlaneUrl(String controlPlaneUrl) {
      this.controlPlaneUrl = controlPlaneUrl;
    }

    public void controlPlaneUrl(String controlPlaneUrl) {
      setControlPlaneUrl(controlPlaneUrl);
    }

    public void setPackageDescriptorFilename(String packageDescriptorFilename) {
      this.packageDescriptorFilename = packageDescriptorFilename;
    }

    public void packageDescriptorFilename(String packageDescriptorFilename) {
      setPackageDescriptorFilename(packageDescriptorFilename);
    }

    public void setValidateSimulationId(String validateSimulationId) {
      this.validateSimulationId = validateSimulationId;
    }

    public void validateSimulationId(String validateSimulationId) {
      setValidateSimulationId(validateSimulationId);
    }

    // simulationId and packageId were only used by the removed upload command:
    // they no longer have a system property or environment variable override
    @Input
    @Optional
    public UUID getSimulationId() {
      return isNullOrEmpty(simulationId) ? null : UUID.fromString(simulationId);
    }

    @Input
    @Optional
    public String getSimulationName() {
      return ConfigurationConstants.StartOptions.SimulationName.valueOf(simulationName);
    }

    @Input
    @Optional
    public String getRunTitle() {
      return ConfigurationConstants.StartOptions.RunTitle.valueOf(runTitle);
    }

    public String getRunDescription() {
      return ConfigurationConstants.StartOptions.RunDescription.valueOf(runDescription);
    }

    @Input
    @Optional
    public String getApiToken() {
      return ConfigurationConstants.ApiToken.valueOf(apiToken);
    }

    @Input
    @Optional
    public UUID getPackageId() {
      return isNullOrEmpty(packageId) ? null : UUID.fromString(packageId);
    }

    @Input
    @Optional
    public URL getApiUrl() {
      return toUrl(ConfigurationConstants.ApiUrl.valueOf(apiUrl));
    }

    @Input
    @Optional
    public URL getWebAppUrl() {
      return toUrl(ConfigurationConstants.WebAppUrl.valueOf(webAppUrl));
    }

    @Input
    @Optional
    public boolean getBatchMode() {
      return ConfigurationConstants.BatchMode.valueOf(batchMode);
    }

    @Input
    @Optional
    public boolean getWaitForRunEnd() {
      return ConfigurationConstants.StartOptions.WaitForRunEnd.valueOf(waitForRunEnd);
    }

    @Input
    @Optional
    public URL getControlPlaneUrl() {
      String str = ConfigurationConstants.ControlPlaneUrl.valueOf(controlPlaneUrl);
      return isNullOrEmpty(str) ? null : toUrl(str);
    }

    @Input
    @Optional
    public String getPackageDescriptorFilename() {
      return ConfigurationConstants.DeployOptions.PackageDescriptorFilename.valueOf(
          packageDescriptorFilename);
    }

    @Input
    @Optional
    public String getValidateSimulationId() {
      return ConfigurationConstants.DeployOptions.ValidateSimulationId.valueOf(
          validateSimulationId);
    }

    public BatchEnterprisePlugin initBatchEnterprisePlugin(Logger logger)
        throws EnterprisePluginException, UnsupportedClientPluginException {
      PluginConfiguration pluginConfiguration = getPluginConfiguration(logger, true);
      try {
        return EnterprisePluginProvider.getBatchInstance(pluginConfiguration);
      } catch (UnsupportedClientException e) {
        throw new UnsupportedClientPluginException(e);
      }
    }

    public EnterprisePlugin initEnterprisePlugin(Logger logger)
        throws EnterprisePluginException, UnsupportedClientPluginException {
      PluginConfiguration pluginConfiguration = getPluginConfiguration(logger, getBatchMode());
      try {
        return EnterprisePluginProvider.getInstance(pluginConfiguration);
      } catch (UnsupportedClientException e) {
        throw new UnsupportedClientPluginException(e);
      }
    }

    private PluginConfiguration getPluginConfiguration(Logger logger, boolean forceBatchMode) {
      String apiToken = getApiToken();
      if (isNullOrEmpty(apiToken)) {
        throw new InvalidUserDataException(
            "\n"
                + "An API token is required to call the Gatling Enterprise server.\n"
                + "See https://docs.gatling.io/reference/execute/cloud/admin/api-tokens/ and create a token with the role 'Configure'.\n"
                + "You can then set your API token's value in the environment variable '"
                + ConfigurationConstants.ApiToken.ENV_VAR
                + "', pass it with '-D"
                + ConfigurationConstants.ApiToken.SYS_PROP
                + "=<API_TOKEN>' or add the configuration to your Gradle settings, e.g.:\n"
                + "gatling.enterprise.apiToken \"MY_API_TOKEN_VALUE\"\n");
      }

      return new PluginConfiguration(
          getApiUrl(),
          getWebAppUrl(),
          apiToken,
          getControlPlaneUrl(),
          BuildTool.GRADLE,
          getPluginVersion(),
          forceBatchMode,
          new GradlePluginIO(logger));
    }

    private String getPluginVersion() {
      String implementationVersion = getClass().getPackage().getImplementationVersion();
      return implementationVersion == null ? "undefined" : implementationVersion;
    }
  }

  private Enterprise enterprise = new Enterprise();

  private String gatlingVersion = GATLING_VERSION;

  private String scalaVersion = SCALA_VERSION;

  private List<String> jvmArgs = new ArrayList<>(GatlingConstants.DEFAULT_JVM_OPTIONS_GATLING);

  private Map<String, Object> systemProperties = new LinkedHashMap<>(DEFAULT_SYSTEM_PROPS);

  private Map<String, Object> environment = new LinkedHashMap<>();

  private List<String> includes = new ArrayList<>();

  private List<String> excludes = new ArrayList<>();

  private Boolean includeMainOutput = true;

  private Boolean includeTestOutput = true;

  public Enterprise getEnterprise() {
    return enterprise;
  }

  public void setEnterprise(Enterprise enterprise) {
    this.enterprise = enterprise;
  }

  public void enterprise(Action<? super Enterprise> action) {
    action.execute(enterprise);
  }

  public String getGatlingVersion() {
    return gatlingVersion;
  }

  public void setGatlingVersion(String gatlingVersion) {
    this.gatlingVersion = gatlingVersion;
  }

  public String getScalaVersion() {
    return scalaVersion;
  }

  public void setScalaVersion(String scalaVersion) {
    this.scalaVersion = scalaVersion;
  }

  public List<String> getJvmArgs() {
    return jvmArgs;
  }

  public void setJvmArgs(List<String> jvmArgs) {
    this.jvmArgs = jvmArgs;
  }

  public Map<String, Object> getSystemProperties() {
    return systemProperties;
  }

  public void setSystemProperties(Map<String, Object> systemProperties) {
    this.systemProperties = systemProperties;
  }

  public Map<String, Object> getEnvironment() {
    return environment;
  }

  public void setEnvironment(Map<String, Object> environment) {
    this.environment = environment;
  }

  public List<String> getIncludes() {
    return includes;
  }

  public void setIncludes(List<String> includes) {
    this.includes = includes;
  }

  public List<String> getExcludes() {
    return excludes;
  }

  public void setExcludes(List<String> excludes) {
    this.excludes = excludes;
  }

  public Boolean getIncludeMainOutput() {
    return includeMainOutput;
  }

  public void setIncludeMainOutput(Boolean includeMainOutput) {
    this.includeMainOutput = includeMainOutput;
  }

  public Boolean getIncludeTestOutput() {
    return includeTestOutput;
  }

  public void setIncludeTestOutput(Boolean includeTestOutput) {
    this.includeTestOutput = includeTestOutput;
  }
}
