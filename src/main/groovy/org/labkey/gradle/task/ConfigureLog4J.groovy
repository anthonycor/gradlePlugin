package org.labkey.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.labkey.gradle.plugin.extension.LabKeyExtension
/**
 * Used to copy over the log4j.xml template file and replace the consoleAppender value
 * as appropriate for the current deployMode.
 */
class ConfigureLog4J extends DefaultTask
{
    @InputFile
    File log4jXML = new File((String) project.serverDeploy.rootWebappsDir, "log4j.xml")

    File outputDir = new File((String) project.staging.webappClassesDir)

    @OutputFile
    File outputFile = new File(outputDir, "log4j.xml")

    @TaskAction
    void copyFile()
    {
        String consoleAppender = "" // this is the setting for production mode
        if (LabKeyExtension.isDevMode(project))
        {
            consoleAppender = '<appender-ref ref="CONSOLE"/>'
        }
        ant.copy(
                todir: outputDir,
                overwrite: true,
                preserveLastModified: true
        )
        {
            fileset(file: log4jXML)
            filterset(beginToken: "@@", endToken: "@@")
                    {
                        filter (token: "consoleAppender",
                                value: consoleAppender
                        )
                    }
        }
    }
}
