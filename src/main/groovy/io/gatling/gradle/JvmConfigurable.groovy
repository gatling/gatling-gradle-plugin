package io.gatling.gradle

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

    static final Map DEFAULT_ENVIRONMENT = [:]

    List<String> jvmArgs

    Map systemProperties

    Map environment
}
