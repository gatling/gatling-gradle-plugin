package io.gatling.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction

import static io.gatling.gradle.GatlingPluginExtension.DEFAULT_LOG_HTTP
import static io.gatling.gradle.GatlingPluginExtension.DEFAULT_LOG_LEVEL

class LogbackConfigTask extends DefaultTask {

    static File logbackFile(File buildDir) {
        new File(buildDir, "generated/gatlingLogback/logback.xml")
    }

    @OutputFile
    File logbackFile = logbackFile(this.project.buildDir)

    static def template(GatlingPluginExtension gatlingExt) {
        """<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%-5level] %logger{15} - %msg%n%rEx</pattern>
        </encoder>
        <immediateFlush>false</immediateFlush>
    </appender>
    ${gatlingExt.logHttp == LogHttp.NONE ? '' : """<logger name="io.gatling.http.engine.response" level="${gatlingExt.logHttp.logLevel}"/>"""}
    <root level="${gatlingExt.logLevel}">
       <appender-ref ref="CONSOLE" />
    </root>
</configuration>"""
    }

    @Internal
    private Logger logger

    /** Used for testing purposes. Was available on the DefaultTask up to Gradle 6.6. */
    void replaceLogger(Logger logger) {
        this.logger = logger
    }

    @Override
    Logger getLogger() {
        return this.logger ?: super.getLogger()
    }

    @InputFiles
    Iterable<File> getLogbackConfigs() {
        SourceSet gatlingSourceSet = project.sourceSets.gatling
        gatlingSourceSet.resources.matching {
            include 'logback-test.xml'
            include 'logback.xml'
            include 'logback.groovy'
        }.files
    }

    @TaskAction
    void generateLogbackConfig() {
        def files = getLogbackConfigs()
        def gatlingExt = project.extensions.getByType(GatlingPluginExtension)

        if (files.isEmpty()) {
            logbackFile.with {
                parentFile.mkdirs()
                if (!gatlingExt.logLevel) {
                    gatlingExt.logLevel = DEFAULT_LOG_LEVEL
                }
                if (!gatlingExt.logHttp) {
                    gatlingExt.logHttp = DEFAULT_LOG_HTTP
                }
                text = template(gatlingExt)
            }
        } else {
            if (logbackFile.exists()) {
                logbackFile.delete()
            }
            if (gatlingExt.logLevel || gatlingExt.logHttp) {
                getLogger().warn("Existing ${files.first().name} will override logLevel and logHttp from gatling configuration in build.gradle.")
            }
        }
    }
}
