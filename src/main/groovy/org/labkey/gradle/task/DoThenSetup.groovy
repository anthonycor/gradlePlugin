package org.labkey.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileCollection
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
        if (project.findProject(":server") != null)
            this.dependsOn project.project(":server").configurations.tomcatJars
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

        if (project.findProject(":server") != null)
            copyTomcatJars()


    }

    private void copyTomcatJars()
    {
        Project serverProject = project.project(":server")
        // for consistency with a distribution deployment and the treatment of all other deployment artifacts,
        // first copy the tomcat jars into the staging directory
        project.ant.copy(

                todir: project.staging.tomcatLibDir,
                preserveLastModified: true
        )
            {
                serverProject.configurations.tomcatJars { Configuration collection ->
                    collection.addToAntBuilder(project.ant, "fileset", FileCollection.AntType.FileSet)
                }
                // Put unversioned files into the tomcatLibDir.  These files are meant to be copied into
                // the tomcat/lib directory when deploying a build or a distribution.  When version numbers change,
                // you will end up with multiple versions of these jar files on the classpath, which will often
                // result in problems of compatibility.  Additionally, we want to maintain the (incorrect) names
                // of the files that have been used with the Ant build process.
                //
                // We may employ CATALINA_BASE in order to separate our libraries from the ones that come with
                // the tomcat distribution. This will require updating our instructions for installation by clients
                // but would allow us to use artifacts with more self-documenting names.
                chainedmapper()
                        {
                            flattenmapper()
                            // get rid of the version numbers on the jar files
                            regexpmapper(from: "^(.*?)(-\\d+(\\.\\d+)*(-\\.*)?(-SNAPSHOT)?)?\\.jar", to: "\\1.jar")
                            filtermapper()
                                    {
                                        replacestring(from: "mysql-connector-java", to: "mysql") // the Ant build used mysql.jar
                                        replacestring(from: "javax.mail", to: "mail") // the Ant build used mail.jar
                                    }
                        }
            }

        // Then copy them into the tomcat/lib directory
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
