package io.gatling.gradle

class GatlingPluginExtension implements JvmConfigurable {

    static final String GATLING_MAIN_CLASS = 'io.gatling.app.Gatling'

    static final String SIMULATIONS_DIR = "src/gatling/scala"

    static final String RESOURCES_DIR = "src/gatling/resources"

    static final String GATLING_TOOL_VERSION = '3.3.1'

    static final String SCALA_VERSION = '2.12.8'

    static final Closure DEFAULT_SIMULATIONS = { include("**/*Simulation*.scala") }

    static final String DEFAULT_LOG_LEVEL = "WARN"

    def toolVersion = GATLING_TOOL_VERSION

    def scalaVersion = SCALA_VERSION

    Closure simulations = DEFAULT_SIMULATIONS

    Boolean includeMainOutput = true
    Boolean includeTestOutput = true

    String logLevel = DEFAULT_LOG_LEVEL

    LogHttp logHttp = LogHttp.NONE

    GatlingPluginExtension() {
        this.jvmArgs = DEFAULT_JVM_ARGS
        this.systemProperties = DEFAULT_SYSTEM_PROPS
    }
}
