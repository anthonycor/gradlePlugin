package org.labkey.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

import org.labkey.gradle.util.PropertiesUtils


class DoThenSetup extends DefaultTask
{

    //default value is init_database_properties, so dependsOn relationship with setup is preserved.
    Closure<Void> fn = {
        initDatabaseProperties(project);
        setJDBCDefaultProps(project);
    }

    @TaskAction
    def setup() {
        getFn().run()

        //ant setup copy portions. Setting jdbc props is now handled by pick_db and bootstrap.
        Properties configProperties = PropertiesUtils.readDatabaseProperties(project);
        configProperties.putAll(project.ext.properties);
        String appDocBase = project.serverDeploy.webappDir.toString().split("[/\\\\]").join("${File.separator}");
        configProperties.setProperty("appDocBase", appDocBase);
        boolean isNextLineComment = false;
        project.copy({
            from "${project.rootProject.projectDir}${File.separator}webapps"
            into "${project.rootProject.buildDir}"
            include "labkey.xml"
            filter ({ String line ->
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

        project.copy({
            from "${project.rootProject.buildDir}"
            into "${project.ext.tomcatConfDir}"
            include "labkey.xml"
        })

        ant.copy(
                todir: "${project.tomcatDir}/lib",
                overwrite: true,
                preservelastmodified: true
        )
        {
                fileset(dir:"${project.labkey.externalLibDir}/tomcat")
                        {
                            include(name:"*.jar")
                        }
        }

        ant.copy(
                todir: "${project.labkey.externalLibDir}/tomcat",
                overwrite: true,
                preservelastmodified: true
        )
        {
                fileset(dir: project.rootProject.buildDir)
                        {
                            include(name: "labkeyBootstrap.jar")
                        }
        }
    }

    static void initDatabaseProperties(Project project)
    {
        Properties configProperties = PropertiesUtils.readDatabaseProperties(project);
        for (String key : configProperties.keySet())
        {
            if (key.contains("database"))
            {
                project.ext[key] = configProperties.getProperty(key);
            }
        }
    }

    static void setJDBCDefaultProps(Project project)
    {
        Properties tempProperties = PropertiesUtils.readDatabaseProperties(project);

        project.ext.jdbcDatabase = project.ext.databaseDefault;
        project.ext.jdbcHost = project.ext.databaseDefaultHost;
        project.ext.jdbcPort = project.ext.databaseDefaultPort;
        project.ext.jdbcURLParameters = "";

        tempProperties.putAll(project.ext.properties);
        project.ext.jdbcURL = PropertiesUtils.parseCompositeProp(tempProperties, tempProperties.getProperty("jdbcURL"));
    }

}
