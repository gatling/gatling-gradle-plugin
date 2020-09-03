package io.gatling.gradle

enum LogHttp {
    NONE(null), ALL("TRACE"), FAILURES("DEBUG")

    final String logLevel

    private LogHttp(String logLevel) {
        this.logLevel = logLevel
    }
}
