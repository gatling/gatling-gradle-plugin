package io.gatling.gradle

import io.gatling.plugin.BatchEnterprisePlugin
import io.gatling.plugin.ConfigurationConstants
import io.gatling.plugin.EnterprisePlugin
import io.gatling.plugin.EnterprisePluginProvider
import io.gatling.plugin.GatlingConstants
import io.gatling.plugin.PluginConfiguration
import io.gatling.plugin.exceptions.UnsupportedClientException
import io.gatling.plugin.io.PluginIO
import io.gatling.plugin.io.PluginLogger
import io.gatling.plugin.io.PluginScanner
import io.gatling.plugin.model.BuildTool
import org.gradle.api.InvalidUserDataException
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

class GatlingPluginExtension {

    static final Map DEFAULT_SYSTEM_PROPS = [:]

    final static class Enterprise {
        private String apiToken
        private String simulationId
        private String simulationName
        private String runTitle
        private String runDescription
        private String packageId
        private String apiUrl
        private String webAppUrl
        private boolean batchMode
        private boolean waitForRunEnd
        private String controlPlaneUrl
        private String packageDescriptorFilename

        def setBatchMode(boolean batchMode) {
            this.batchMode = batchMode
        }

        def batchMode(boolean batchMode) {
            setBatchMode(batchMode)
        }

        def setApiUrl(String apiUrl) {
            this.apiUrl = apiUrl
        }

        def apiUrl(String apiUrl) {
            setApiUrl(apiUrl)
        }

        def setWebAppUrl(String webAppUrl) {
            this.webAppUrl = webAppUrl
        }

        def webAppUrl(String webAppUrl) {
            setWebAppUrl(webAppUrl)
        }

        def setSimulationId(String simulationId) {
            this.simulationId = simulationId
        }

        def simulationId(String simulationId) {
            setSimulationId(simulationId)
        }

        def setSimulationName(String simulationName) {
            this.simulationName = simulationName
        }

        def simulationName(String simulationName) {
            setSimulationName(simulationName)
        }

        def setRunTitle(String runTitle) {
            this.runTitle = runTitle
        }

        def runTitle(String runTitle) {
            setRunTitle(runTitle)
        }

        def setRunDescription(String runDescription) {
            this.runDescription = runDescription
        }

        def runDescription(String runDescription) {
            setRunDescription(runDescription)
        }

        def setPackageId(String packageId) {
            this.packageId = packageId
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

        def setWaitForRunEnd(boolean waitForRunEnd) {
            this.waitForRunEnd = waitForRunEnd
        }

        def waitForRunEnd(boolean waitForRunEnd) {
            setWaitForRunEnd(waitForRunEnd)
        }

        def setControlPlaneUrl(String controlPlaneUrl) {
            this.controlPlaneUrl = controlPlaneUrl
        }

        def controlPlaneUrl(String controlPlaneUrl) {
            setControlPlaneUrl(controlPlaneUrl)
        }

        def setPackageDescriptorFilename(String packageDescriptorFilename) {
            this.packageDescriptorFilename = packageDescriptorFilename
        }

        def packageDescriptorFilename(String packageDescriptorFilename) {
            setPackageDescriptorFilename(packageDescriptorFilename)
        }

        @Input
        @Optional
        UUID getSimulationId() {
            var str = ConfigurationConstants.UploadOptions.SimulationId.valueOf(simulationId)
            str ? UUID.fromString(str) : null
        }

        @Input
        @Optional
        String getSimulationName() {
            ConfigurationConstants.StartOptions.SimulationName.valueOf(simulationName)
        }

        @Input
        @Optional
        String getRunTitle() {
            ConfigurationConstants.StartOptions.RunTitle.valueOf(runTitle)
        }

        String getRunDescription() {
            ConfigurationConstants.StartOptions.RunDescription.valueOf(runDescription)
        }

        @Input
        @Optional
        String getApiToken() {
            ConfigurationConstants.ApiToken.valueOf(apiToken)
        }

        @Input
        @Optional
        UUID getPackageId() {
            var str = ConfigurationConstants.UploadOptions.PackageId.valueOf(packageId)
            str ? UUID.fromString(str) : null
        }

        @Input
        @Optional
        URL getApiUrl() {
            new URI(ConfigurationConstants.ApiUrl.valueOf(apiUrl)).toURL()
        }

        @Input
        @Optional
        URL getWebAppUrl() {
            new URI(ConfigurationConstants.WebAppUrl.valueOf(webAppUrl)).toURL()
        }

        @Input
        @Optional
        boolean getBatchMode() {
            ConfigurationConstants.BatchMode.valueOf(batchMode)
        }

        @Input
        @Optional
        boolean getWaitForRunEnd() {
            ConfigurationConstants.StartOptions.WaitForRunEnd.valueOf(waitForRunEnd)
        }

        @Input
        @Optional
        URL getControlPlaneUrl() {
            var str = ConfigurationConstants.ControlPlaneUrl.valueOf(controlPlaneUrl)
            str ? new URI(str).toURL() : null
        }

        @Input
        @Optional
        String getPackageDescriptorFilename() {
            ConfigurationConstants.DeployOptions.PackageDescriptorFilename.valueOf(packageDescriptorFilename)
        }

        BatchEnterprisePlugin initBatchEnterprisePlugin(Logger logger) {
            PluginConfiguration pluginConfiguration = getPluginConfiguration(logger, true)

            try {
                return EnterprisePluginProvider.getBatchInstance(pluginConfiguration)
            } catch (UnsupportedClientException e) {
                throw new UnsupportedClientPluginException(e)
            }
        }

        EnterprisePlugin initEnterprisePlugin(Logger logger) {
            PluginConfiguration pluginConfiguration = getPluginConfiguration(logger, getBatchMode())

            try {
                return EnterprisePluginProvider.getInstance(pluginConfiguration)
            } catch (UnsupportedClientException e) {
                throw new UnsupportedClientPluginException(e)
            }
        }

        private PluginConfiguration getPluginConfiguration(Logger logger, boolean forceBatchMode) {
            final apiToken = ConfigurationConstants.ApiToken.value()
            if (!apiToken) {
                throw new InvalidUserDataException("""
                    |An API token is required to call the Gatling Enterprise server.
                    |See https://docs.gatling.io/reference/execute/cloud/admin/api-tokens/ and create a token wil the role 'Configure'.
                    |You can then set your API token's value in the environment variable '${ConfigurationConstants.ApiToken.ENV_VAR}', pass it with '-D${ConfigurationConstants.ApiToken.SYS_PROP}=<API_TOKEN>' or add the configuration to your Gradle settings, e.g.:
                    |gatling.enterprise.apiToken \"MY_API_TOKEN_VALUE\"
                    """.stripMargin()
                )
            }

            return new PluginConfiguration(
                getApiUrl(),
                getWebAppUrl(),
                apiToken,
                getControlPlaneUrl(),
                BuildTool.GRADLE,
                getPluginVersion(),
                forceBatchMode,
                getPluginIOInstance(logger)
            )
        }

        private String getPluginVersion() {
            final String implementationVersion = getClass().getPackage().getImplementationVersion()

            return (implementationVersion == null) ? "undefined" : implementationVersion
        }

        private PluginIO getPluginIOInstance(logger) {
            return new PluginIO() {
                private final GradlePluginIO gradlePluginIO = new GradlePluginIO(logger)

                @Override
                PluginLogger getLogger() {
                    return gradlePluginIO.logger
                }

                @Override
                PluginScanner getScanner() {
                    return gradlePluginIO.scanner
                }
            }
        }
    }

    Enterprise enterprise = new Enterprise()

    def enterprise(Closure c) {
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c.delegate = enterprise
        c()
    }

    static final String GATLING_MAIN_CLASS = 'io.gatling.app.Gatling'

    static final String GATLING_RECORDER_CLASS = 'io.gatling.recorder.GatlingRecorder'

    static final String GATLING_JAVA_SOURCES_DIR = "src/gatling/java"

    static final String GATLING_SCALA_SOURCES_DIR = "src/gatling/scala"

    static final String GATLING_KOTLIN_SOURCES_DIR = "src/gatling/kotlin"

    static final String GATLING_RESOURCES_DIR = "src/gatling/resources"

    static final String GATLING_VERSION = '3.13.5'

    static final String SCALA_VERSION = '2.13.16'

    String gatlingVersion = GATLING_VERSION

    String scalaVersion = SCALA_VERSION

    List<String> jvmArgs
    Map systemProperties
    Map environment = [:]

    List<String> includes = List.of()
    List<String> excludes = List.of()

    Boolean includeMainOutput = true
    Boolean includeTestOutput = true

    GatlingPluginExtension() {
        this.jvmArgs = GatlingConstants.DEFAULT_JVM_OPTIONS_GATLING
        this.systemProperties = DEFAULT_SYSTEM_PROPS
    }
}
