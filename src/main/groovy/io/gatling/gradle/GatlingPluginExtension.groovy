package io.gatling.gradle

class GatlingPluginExtension implements JvmConfigurable {

    private static final String API_TOKEN_PROPERTY = "gatling.enterprise.apiToken"
    private static final String API_TOKEN_ENV = "GATLING_ENTERPRISE_API_TOKEN"

    final static class Enterprise {
        private String apiToken
        private UUID packageId
        private URL url = new URL("https://cloud.gatling.io/api/public")

        def url(String url) {
            this.url = new URL(url)
        }

        def packageId(String packageId) {
            this.packageId = UUID.fromString(packageId)
        }

        def apiToken(String apiToken) {
            this.apiToken = apiToken
        }

        String getApiToken() {
            if (apiToken == null) {
                return System.getProperty(API_TOKEN_PROPERTY, System.getenv(API_TOKEN_ENV))
            } else {
                return apiToken
            }
        }

        UUID getPackageId() {
            return packageId
        }

        URL getUrl() {
            return url
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

    static final String GATLING_TOOL_VERSION = '3.7.0-M4'

    static final String SCALA_VERSION = '2.13.7'

    static final Closure DEFAULT_SIMULATIONS = { include("**/*Simulation*.java", "**/*Simulation*.kt", "**/*Simulation*.scala") }

    static final String DEFAULT_LOG_LEVEL = "WARN"
    static final LogHttp DEFAULT_LOG_HTTP = LogHttp.NONE

    def toolVersion = GATLING_TOOL_VERSION

    def scalaVersion = SCALA_VERSION

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
