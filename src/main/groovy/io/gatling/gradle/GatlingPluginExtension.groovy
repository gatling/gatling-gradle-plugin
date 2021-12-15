package io.gatling.gradle

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

class GatlingPluginExtension implements JvmConfigurable {

    private static final String API_TOKEN_PROPERTY = "gatling.enterprise.apiToken"
    private static final String API_TOKEN_ENV = "GATLING_ENTERPRISE_API_TOKEN"
    private static final String SIMULATION_ID_PROPERTY = "gatling.enterprise.simulationId"
    private static final String SIMULATION_ID_ENV = "GATLING_ENTERPRISE_SIMULATION_ID"
    private static final String PUBLIC_API_PATH = "/api/public"


    final static class Enterprise {
        private String apiToken
        private UUID simulationId
        private UUID packageId
        private Map<String, String> systemProps
        private URL url = new URL("https://cloud.gatling.io/api/public")
        private String simulationClass

        def setUrl(String url) {
            this.url = new URL(url + PUBLIC_API_PATH)
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

        def setSystemProps(Map<String, String> systemProps) {
            this.systemProps = systemProps
        }

        def systemProps(Map<String, String> systemProps) {
            setSystemProps(systemProps)
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

        @Input
        @Optional
        UUID getSimulationId() {
            if (simulationId == null) {
                def systemSimulationId = System.getProperty(SIMULATION_ID_PROPERTY, System.getenv(SIMULATION_ID_ENV))
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
        String getApiToken() {
            if (apiToken == null) {
                return System.getProperty(API_TOKEN_PROPERTY, System.getenv(API_TOKEN_ENV))
            } else {
                return apiToken
            }
        }

        @Input
        @Optional
        UUID getPackageId() {
            return packageId
        }

        @Input
        @Optional
        URL getUrl() {
            return url
        }

        @Input
        @Optional
        String getSimulationClass() {
            return simulationClass
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

    static final String GATLING_VERSION = '3.7.3'

    static final String SCALA_VERSION = '2.13.7'

    static final Closure DEFAULT_SIMULATIONS = { include("**/*Simulation*.java", "**/*Simulation*.kt", "**/*Simulation*.scala") }

    static final String DEFAULT_LOG_LEVEL = "WARN"
    static final LogHttp DEFAULT_LOG_HTTP = LogHttp.NONE

    String gatlingVersion = GATLING_VERSION

    String scalaVersion = SCALA_VERSION

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
        this.jvmArgs = DEFAULT_JVM_ARGS
        this.systemProperties = DEFAULT_SYSTEM_PROPS
    }
}
