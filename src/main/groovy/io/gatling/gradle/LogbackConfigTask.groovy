package io.gatling.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction

class LogbackConfigTask extends DefaultTask {

    public static File logbackFile(File buildDir) {
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
        if (files.isEmpty()) {
            logbackFile.with {
                parentFile.mkdirs()
                text = template(project.extensions.getByType(GatlingPluginExtension))
            }
        } else if (logbackFile.exists()) {
            logbackFile.delete()
        }
    }
}
