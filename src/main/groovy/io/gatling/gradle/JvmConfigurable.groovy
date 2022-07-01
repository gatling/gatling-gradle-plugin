package io.gatling.gradle

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

trait JvmConfigurable {

    static final Map DEFAULT_SYSTEM_PROPS = [:]

    /*
    * Note: We need the getter and setters, because Groovy traits generate field names with fully qualified names
    * like io_gatling_gradle_JvmConfigurable__jvmArgs which will confuse Gradle UP-TO-DATE annotations.
    */

    private List<String> jvmArgs

    @Input
    @Optional
    List<String> getJvmArgs() {
        return jvmArgs
    }

    void setJvmArgs(List<String> jvmArgs) {
        this.jvmArgs = jvmArgs
    }

    void jvmArgs(List<String> jvmArgs) {
        setJvmArgs(jvmArgs)
    }

    private Map systemProperties

    @Input
    @Optional
    Map getSystemProperties() {
        return systemProperties
    }

    void setSystemProperties(Map systemProperties) {
        this.systemProperties = systemProperties
    }

    void systemProperties(Map systemProperties) {
        setSystemProperties(systemProperties)
    }

    private Map environment = [:]

    @Input
    Map getEnvironment() {
        return environment
    }

    void setEnvironment(Map environment) {
        this.environment = environment
    }

    void environment(Map environment) {
        setEnvironment(environment)
    }
}
