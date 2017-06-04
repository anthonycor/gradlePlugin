package org.labkey.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.CopySpec
import org.gradle.api.tasks.TaskAction
import org.labkey.gradle.util.DatabaseProperties
import org.labkey.gradle.util.PropertiesUtils

class DoThenSetup extends DefaultTask
{
    protected DatabaseProperties databaseProperties

    Closure<Void> fn = {
        setDatabaseProperties();
    }

    DoThenSetup()
    {
        this.dependsOn project.tasks.stageTomcatJars
    }

    @TaskAction
    void setup() {
        getFn().run()

        //ant setup copy portions. Setting jdbc props is now handled by pick_db and bootstrap.
        Properties configProperties = databaseProperties.getConfigProperties()
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
                return PropertiesUtils.replaceProps(line, configProperties, true);
            })
        })

        project.copy({ CopySpec copy ->
            copy.from "${project.rootProject.buildDir}"
            copy.into "${project.ext.tomcatConfDir}"
            copy.include "labkey.xml"
        })

        project.ant.copy(
                todir: "${project.tomcatDir}/lib",
                preserveLastModified: true
        )
                {
                    fileset(dir: project.staging.tomcatLibDir)
                }

    }

    protected void setDatabaseProperties()
    {
        databaseProperties = new DatabaseProperties(project, false)
    }

    void setDatabaseProperties(DatabaseProperties dbProperties)
    {
        this.databaseProperties = dbProperties
    }

    DatabaseProperties getDatabaseProperties()
    {
        return databaseProperties
    }
}
