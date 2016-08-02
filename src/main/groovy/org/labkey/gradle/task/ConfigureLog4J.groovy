package org.labkey.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.labkey.gradle.plugin.LabKey

/**
 * Created by susanh on 8/2/16.
 */
class ConfigureLog4J extends DefaultTask
{
    @TaskAction
    def compress()
    {
        def String consoleAppender = "" // this is the setting for production mode
        if (project.labkey.deployMode == LabKey.DeployMode.dev)
        {
            consoleAppender = '<appender-ref ref="CONSOLE">"'
        }
        ant.copy(
                todir: project.labkey.webappClassesDir,
                overwrite: true,
                preserveLastModified: true,

        )
        {
            fileset(file: "${project.labkey.rootWebappsDir}/log4j.xml")
            filterset(beginToken: "@@", endToken: "@@")
                    {
                        filter (token: "consoleAppender",
                                value: consoleAppender
                        )
                    }
        }
    }
}
