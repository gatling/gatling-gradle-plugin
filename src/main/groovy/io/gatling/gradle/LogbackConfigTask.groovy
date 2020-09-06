package io.gatling.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class LogbackConfigTask extends DefaultTask {

    static def template(GatlingPluginExtension gatlingExt) {
        def httpLogger = gatlingExt.logHttp == LogHttp.NONE ? "" : "<logger name=\"io.gatling.http.engine.response\" level=\"${gatlingExt.logHttp.logLevel}\"/>"

        """<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%-5level] %logger{15} - %msg%n%rEx</pattern>
        </encoder>
        <immediateFlush>false</immediateFlush>
    </appender>
    $httpLogger
    <root level="${gatlingExt.logLevel}">
       <appender-ref ref="CONSOLE" />
    </root>
</configuration>"""
    }

    @TaskAction
    void generateLogbackConfig() {
        def gatlingExt = this.project.extensions.getByType(GatlingPluginExtension)
        if (!this.project.file("${GatlingPluginExtension.RESOURCES_DIR}/logback.xml").exists()) {
            new File(this.project.buildDir, "resources/gatling/logback.xml").with {
                parentFile.mkdirs()
                text = template(gatlingExt)
            }
        }
    }
}
