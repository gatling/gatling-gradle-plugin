package io.gatling.gradle;

public final class UnsupportedClientPluginException extends Exception {
    public UnsupportedClientPluginException(Exception cause) {
        super(
           "Please update the Gatling Gradle plugin to the latest version for compatibility with Gatling Enterprise. See https://gatling.io/docs/gatling/reference/current/extensions/gradle_plugin/ for more information about this plugin.",
           cause
        );
    }
}
