package org.labkey.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.CopySpec
import org.gradle.api.tasks.TaskAction
import org.labkey.gradle.util.DatabaseProperties
import org.labkey.gradle.util.PropertiesUtils

class DoThenSetup extends DefaultTask
{
    protected DatabaseProperties dbProperties

    Closure<Void> fn = {
        setDatabaseProperties();
    }

    @TaskAction
    setup() {
        getFn().run()

        //ant setup copy portions. Setting jdbc props is now handled by pick_db and bootstrap.
        Properties configProperties = dbProperties.getConfigProperties()
        project.logger.info("adding properties ${project.ext.properties} to ${configProperties}")
        configProperties.putAll(project.ext.properties)
        String appDocBase = project.serverDeploy.webappDir.toString().split("[/\\\\]").join("${File.separator}")
        configProperties.setProperty("appDocBase", appDocBase);
        boolean isNextLineComment = false;
        project.copy({ CopySpec copy ->
            copy.from "${project.rootProject.projectDir}/webapps"
            copy.into "${project.rootProject.buildDir}"
            copy.include "labkey.xml"
            copy.filter ({ String line ->
                String newLine = line;

                if (project.ext.has('enableJms') && project.ext.enableJms)
                {
                    newLine = newLine.replace("<!--@@jmsConfig@@", "");
                    newLine = newLine.replace("@@jmsConfig@@-->", "");
                    return newLine;
                }
                if (isNextLineComment || newLine.contains("<!--"))
                {
                    isNextLineComment = !newLine.contains("-->");
                    return newLine;
                }
                return PropertiesUtils.replaceProps(line, configProperties);
            })
        })

        project.copy({ CopySpec copy ->
            copy.from "${project.rootProject.buildDir}"
            copy.into "${project.ext.tomcatConfDir}"
            copy.include "labkey.xml"
        })
    }

    protected void setDatabaseProperties()
    {
        dbProperties = new DatabaseProperties(project, false)
        project.logger.info("Set dbProperties with configProperties ${dbProperties.configProperties}")
    }
}
