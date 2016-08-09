package org.labkey.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.labkey.gradle.plugin.LabKey

/**
 * Created by susanh on 8/2/16.
 */
class ConfigureLog4J extends DefaultTask
{
    @InputFile
    File log4jXML = new File("${project.serverDeploy.rootWebappsDir}/log4j.xml")

    @OutputDirectory
    File outputDir = new File(project.labkey.webappClassesDir)
    @TaskAction
    def compress()
    {
        def String consoleAppender = "" // this is the setting for production mode
        if (project.labkey.deployMode == LabKey.DeployMode.dev)
        {
            consoleAppender = '<appender-ref ref="CONSOLE">"'
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
