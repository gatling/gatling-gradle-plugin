package io.gatling.gradle

import io.gatling.plugin.BatchEnterprisePlugin
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
    private static final String API_TOKEN_PROPERTY = "gatling.enterprise.apiToken"
    private static final String API_TOKEN_ENV = "GATLING_ENTERPRISE_API_TOKEN"
    private static final String SIMULATION_ID_PROPERTY = "gatling.enterprise.simulationId"
    private static final String SIMULATION_NAME_PROPERTY = "gatling.enterprise.simulationName"
    private static final String RUN_TITLE_PROPERTY = "gatling.enterprise.runTitle"
    private static final String RUN_DESCRIPTION_PROPERTY = "gatling.enterprise.runDescription"
    private static final String PACKAGE_ID_PROPERTY = "gatling.enterprise.packageId"
    private static final String BATCH_MODE_PROPERTY = "gatling.enterprise.batchMode"
    private static final String WAIT_FOR_RUN_END_PROPERTY = "gatling.enterprise.waitForRunEnd"
    private static final String CONTROL_PLANE_URL = "gatling.enterprise.controlPlaneUrl"
    private static final String PACKAGE_DESCRIPTOR_FILENAME_PROPERTY = "gatling.enterprise.packageDescriptorFilename"
    private static final String PLUGIN_NAME = "gatling-gradle-plugin"

    final static class Enterprise {
        private String apiToken
        private UUID simulationId
        private String simulationName
        private String runTitle
        private String runDescription
        private UUID packageId
        private URL url = URI.create("https://cloud.gatling.io").toURL()
        private boolean batchMode
        private boolean waitForRunEnd
        private URL controlPlaneUrl
        private String packageDescriptorFilename

        def setBatchMode(boolean batchMode) {
            this.batchMode = batchMode
        }

        def batchMode(boolean batchMode) {
            setBatchMode(batchMode)
        }

        def setUrl(String url) {
            this.url = URI.create(url).toURL()
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

        def setSimulationName(String simulationName) {
            this.simulationId = UUID.fromString(simulationName)
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
            setRunDescription(runTitle)
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

        def setPackageDescriptorFilename(String packageDescriptorFilename) {
            this.packageDescriptorFilename = packageDescriptorFilename
        }

        def packageDescriptorFilename(String packageDescriptorFilename) {
            setPackageDescriptorFilename(packageDescriptorFilename)
        }

        @Input
        @Optional
        UUID getSimulationId() {
            simulationId ?: System.getProperty(SIMULATION_ID_PROPERTY) ?: null
        }

        @Input
        @Optional
        String getSimulationName() {
            simulationName ?: System.getProperty(SIMULATION_NAME_PROPERTY) ?: null
        }

        @Input
        @Optional
        String getRunTitle() {
            runTitle ?: System.getProperty(RUN_TITLE_PROPERTY) ?: null
        }

        String getRunDescription() {
            runDescription ?: System.getProperty(RUN_DESCRIPTION_PROPERTY) ?: null
        }

        @Input
        @Optional
        String getApiToken() {
            apiToken ?: System.getProperty(API_TOKEN_PROPERTY, System.getenv(API_TOKEN_ENV))
        }

        @Input
        @Optional
        UUID getPackageId() {
            packageId ?: System.getProperty(PACKAGE_ID_PROPERTY) ?: null
        }

        @Input
        @Optional
        URL getUrl() {
            return url
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

        @Input
        @Optional
        String getPackageDescriptorFilename() {
            packageDescriptorFilename ?: System.getProperty(PACKAGE_DESCRIPTOR_FILENAME_PROPERTY) ?: null
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
            final apiToken = getApiToken()
            if (!apiToken) {
                throw new InvalidUserDataException("""
                    |An API token is required to call the Gatling Enterprise server.
                    |See https://gatling.io/docs/enterprise/cloud/reference/admin/api_tokens/ and create a token wil the role 'Configure'.
                    |You can then set your API token's value in the environment variable 'GATLING_ENTERPRISE_API_TOKEN', pass it with '-Dgatling.enterprise.apiToken=<API_TOKEN>' or add the configuration to your Gradle settings, e.g.:
                    |gatling.enterprise.apiToken \"MY_API_TOKEN_VALUE\"
                    """.stripMargin()
                )
            }

            return new PluginConfiguration(
                getUrl(),
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

    static final String GATLING_VERSION = '3.11.5'

    static final String SCALA_VERSION = '2.13.14'

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
