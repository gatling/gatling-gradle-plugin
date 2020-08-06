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

    static final Map DEFAULT_SYSTEM_PROPS = ["java.net.preferIPv6Addresses": true]

    List<String> jvmArgs

    Map systemProperties
}
