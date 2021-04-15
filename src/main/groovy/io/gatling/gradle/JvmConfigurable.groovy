package io.gatling.gradle

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

trait JvmConfigurable {

    static final List<String> DEFAULT_JVM_ARGS = [
        '-server',
        '-Xmx1G',
        '-XX:+HeapDumpOnOutOfMemoryError',
        '-XX:+UseG1GC',
        '-XX:+ParallelRefProcEnabled',
        '-XX:MaxInlineLevel=20',
        '-XX:MaxTrivialSize=12',
        '-XX:-UseBiasedLocking'
    ]

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

    private Map systemProperties

    @Input
    @Optional
    Map getSystemProperties() {
        return systemProperties
    }

    void setSystemProperties(Map systemProperties) {
        this.systemProperties = systemProperties
    }

    private Map environment = [:]

    @Input
    Map getEnvironment() {
        return environment
    }

    void setEnvironment(Map environment) {
        this.environment = environment
    }
}
