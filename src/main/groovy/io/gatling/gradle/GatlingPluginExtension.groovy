package io.gatling.gradle

import io.gatling.plugin.BatchEnterprisePlugin
import io.gatling.plugin.BatchEnterprisePluginClient
import io.gatling.plugin.GatlingConstants
import io.gatling.plugin.InteractiveEnterprisePlugin
import io.gatling.plugin.InteractiveEnterprisePluginClient
import io.gatling.plugin.client.EnterpriseClient
import io.gatling.plugin.client.http.HttpEnterpriseClient
import io.gatling.plugin.exceptions.UnsupportedClientException
import io.gatling.plugin.io.PluginIO
import org.gradle.api.InvalidUserDataException
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

class GatlingPluginExtension {

    static final Map DEFAULT_SYSTEM_PROPS = [:]
    private static final String API_TOKEN_PROPERTY = "gatling.enterprise.apiToken"
    private static final String API_TOKEN_ENV = "GATLING_ENTERPRISE_API_TOKEN"
    private static final String SIMULATION_ID_PROPERTY = "gatling.enterprise.simulationId"
    private static final String TEAM_ID_PROPERTY = "gatling.enterprise.teamId"
    private static final String PACKAGE_ID_PROPERTY = "gatling.enterprise.packageId"
    private static final String SIMULATION_CLASS_PROPERTY = "gatling.enterprise.simulationClass"
    private static final String BATCH_MODE_PROPERTY = "gatling.enterprise.batchMode"
    private static final String SYSTEM_PROPS_PROPERTY = "gatling.enterprise.systemProps"
    private static final String ENVIRONMENT_VARIABLES_PROPERTY = "gatling.enterprise.environmentVariables"
    private static final String WAIT_FOR_RUN_END_PROPERTY = "gatling.enterprise.waitForRunEnd"
    private static final String CONTROL_PLANE_URL = "gatling.enterprise.controlPlaneUrl"
    private static final String PLUGIN_NAME = "gatling-gradle-plugin"

    final static class Enterprise {
        private String apiToken
        private UUID simulationId
        private UUID teamId
        private UUID packageId
        private Map<String, String> systemProps
        private String systemPropsString
        private Map<String, String> environmentVariables
        private String environmentVariablesString
        private URL url = new URL("https://cloud.gatling.io")
        private String simulationClass
        private boolean batchMode
        private boolean waitForRunEnd
        private URL controlPlaneUrl

        def setBatchMode(boolean batchMode) {
            this.batchMode = batchMode
        }

        def batchMode(boolean batchMode) {
            setBatchMode(batchMode)
        }

        def setUrl(String url) {
            this.url = new URL(url)
        }

        def url(String url) {
            setUrl(url)
        }

        def setSimulationId(String simulationId) {
            this.simulationId = UUID.fromString(simulationId)
        }

        def simulationId(String simulationId) {
            setSimulationId(simulationId)
        }

        def setTeamId(String teamId) {
            this.teamId = UUID.fromString(teamId)
        }

        def teamId(String teamId) {
            setTeamId(teamId)
        }

        def setSystemProps(Map<String, String> systemProps) {
            this.systemProps = systemProps
        }

        def systemProps(Map<String, String> systemProps) {
            setSystemProps(systemProps)
        }

        def setSystemPropsString(String systemPropsString) {
            this.systemPropsString = systemPropsString
        }

        def systemPropsString(String systemPropsString) {
            setSystemPropsString(systemPropsString)
        }

        def setEnvironmentVariables(Map<String, String> environmentVariables) {
            this.environmentVariables = environmentVariables
        }

        def environmentVariables(Map<String, String> environmentVariables) {
            setEnvironmentVariables(environmentVariables)
        }

        def setEnvironmentVariablesString(String environmentVariablesString) {
            this.environmentVariablesString = environmentVariablesString
        }

        def environmentVariablesString(String environmentVariablesString) {
            setEnvironmentVariablesString(environmentVariablesString)
        }

        def setPackageId(String packageId) {
            this.packageId = UUID.fromString(packageId)
        }

        def packageId(String packageId) {
            setPackageId(packageId)
        }

        def setApiToken(String apiToken) {
            this.apiToken = apiToken
        }

        def apiToken(String apiToken) {
            setApiToken(apiToken)
        }

        def setSimulationClass(String simulationClass) {
            this.simulationClass = simulationClass
        }

        def simulationClass(String simulationClass) {
            setSimulationClass(simulationClass)
        }

        def setWaitForRunEnd(boolean waitForRunEnd) {
            this.waitForRunEnd = waitForRunEnd
        }

        def waitForRunEnd(boolean waitForRunEnd) {
            setWaitForRunEnd(waitForRunEnd)
        }

        def setControlPlaneUrl(String controlPlaneUrl) {
            this.controlPlaneUrl = new URL(controlPlaneUrl)
        }

        def controlPlaneUrl(String controlPlaneUrl) {
            setControlPlaneUrl(controlPlaneUrl)
        }

        @Input
        @Optional
        UUID getSimulationId() {
            if (simulationId == null) {
                def systemSimulationId = System.getProperty(SIMULATION_ID_PROPERTY)
                return systemSimulationId ? UUID.fromString(systemSimulationId) : null
            } else {
                return simulationId
            }
        }

        @Input
        @Optional
        Map<String, String> getSystemProps() {
            return systemProps
        }

        @Input
        @Optional
        String getSystemPropsString() {
            return systemPropsString ?: System.getProperty(SYSTEM_PROPS_PROPERTY)
        }

        @Input
        @Optional
        Map<String, String> getEnvironmentVariables() {
            return environmentVariables
        }

        @Input
        @Optional
        String getEnvironmentVariablesString() {
            return environmentVariablesString ?: System.getProperty(ENVIRONMENT_VARIABLES_PROPERTY)
        }

        @Input
        @Optional
        String getApiToken() {
            return apiToken ?: System.getProperty(API_TOKEN_PROPERTY, System.getenv(API_TOKEN_ENV))
        }

        @Input
        @Optional
        UUID getPackageId() {
            if (packageId == null) {
                def systemPackageId = System.getProperty(PACKAGE_ID_PROPERTY)
                return systemPackageId ? UUID.fromString(systemPackageId) : null
            } else {
                return packageId
            }
        }

        @Input
        @Optional
        UUID getTeamId() {
            if (teamId == null) {
                def systemTeamId = System.getProperty(TEAM_ID_PROPERTY)
                return systemTeamId ? UUID.fromString(systemTeamId) : null
            } else {
                return teamId
            }
        }

        @Input
        @Optional
        URL getUrl() {
            return url
        }

        @Input
        @Optional
        String getSimulationClass() {
            return simulationClass ?: System.getProperty(SIMULATION_CLASS_PROPERTY)
        }

        @Input
        @Optional
        boolean getBatchMode() {
            return batchMode || Boolean.getBoolean(BATCH_MODE_PROPERTY)
        }

        @Input
        @Optional
        boolean getWaitForRunEnd() {
            return waitForRunEnd || Boolean.getBoolean(WAIT_FOR_RUN_END_PROPERTY)
        }

        @Input
        @Optional
        URL getControlPlaneUrl() {
            def sysProp = System.getProperty(CONTROL_PLANE_URL)
            if (controlPlaneUrl != null) {
                return controlPlaneUrl
            } else if (sysProp != null && sysProp != ""){
                return new URL(sysProp)
            } else {
                return null
            }
        }

        EnterpriseClient initEnterpriseClient(String version) {
            def apiToken = getApiToken()
            if (!apiToken) {
                throw new InvalidUserDataException("""
                    |An API token is required to call the Gatling Enterprise server.
                    |See https://gatling.io/docs/enterprise/cloud/reference/admin/api_tokens/ and create a token wil the role 'Configure'.
                    |You can then set your API token's value in the environment variable 'GATLING_ENTERPRISE_API_TOKEN', pass it with '-Dgatling.enterprise.apiToken=<API_TOKEN>' or add the configuration to your Gradle settings, e.g.:
                    |gatling.enterprise.apiToken \"MY_API_TOKEN_VALUE\"
                    """.stripMargin()
                )
            }

            try {
                return new HttpEnterpriseClient(getUrl(), getApiToken(), PLUGIN_NAME, version, getControlPlaneUrl())
            } catch (UnsupportedClientException e) {
                throw new InvalidUserDataException(
                    "Please update the Gatling Gradle plugin to the latest version for compatibility with Gatling Enterprise. See https://gatling.io/docs/gatling/reference/current/extensions/gradle_plugin/ for more information about this plugin.",
                    e)
            }
        }

        BatchEnterprisePlugin initBatchEnterprisePlugin(String version, Logger logger) {
            return new BatchEnterprisePluginClient(initEnterpriseClient(version), new GradlePluginIO(logger).logger)
        }

        InteractiveEnterprisePlugin initInteractiveEnterprisePlugin(String version, Logger logger) {
            PluginIO pluginIO = new GradlePluginIO(logger)

            return new InteractiveEnterprisePluginClient(initEnterpriseClient(version), pluginIO)
        }
    }

    Enterprise enterprise = new Enterprise()

    def enterprise(Closure c) {
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c.delegate = enterprise
        c()
    }

    static final String GATLING_MAIN_CLASS = 'io.gatling.app.Gatling'

    static final String JAVA_SIMULATIONS_DIR = "src/gatling/java"

    static final String SCALA_SIMULATIONS_DIR = "src/gatling/scala"

    static final String KOTLIN_SIMULATIONS_DIR = "src/gatling/kotlin"

    static final String RESOURCES_DIR = "src/gatling/resources"

    static final String GATLING_VERSION = '3.10.5'

    static final String SCALA_VERSION = '2.13.13'

    static final Closure DEFAULT_SIMULATIONS = { include("**/*Simulation*.java", "**/*Simulation*.kt", "**/*Simulation*.scala") }

    static final String DEFAULT_LOG_LEVEL = "WARN"
    static final LogHttp DEFAULT_LOG_HTTP = LogHttp.NONE

    String gatlingVersion = GATLING_VERSION

    String scalaVersion = SCALA_VERSION

    List<String> jvmArgs
    Map systemProperties
    Map environment = [:]

    /**
     * Use gatlingVersion instead
     * @param toolVersion the Gatling version
     *
     * @deprecated  As of release 3.7.0, replaced by {@link #gatlingVersion}
     */
    @Deprecated
    void setToolVersion(String toolVersion) {
        gatlingVersion = toolVersion
    }

    Closure simulations = DEFAULT_SIMULATIONS

    Boolean includeMainOutput = true
    Boolean includeTestOutput = true

    String logLevel

    LogHttp logHttp

    GatlingPluginExtension() {
        this.jvmArgs = GatlingConstants.DEFAULT_JVM_OPTIONS_GATLING
        this.systemProperties = DEFAULT_SYSTEM_PROPS
    }
}
