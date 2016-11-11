package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.JavaExec
import org.labkey.gradle.util.GroupNames

/**
 * Adds tasks for building the bootstrap jar file, copying it to the tomcat directory and creating the api file list
 * used during startup to remove unused jar files from the deployment.
 *
 * CONSIDER: Convert to a Sync type task from Gradle to do the removal of unused jar files
 */
class ServerBootstrap implements Plugin<Project>
{
    private static final String BOOTSTRAP_MAIN_CLASS = "org.labkey.bootstrap.ModuleExtractor"

    @Override
    void apply(Project project)
    {
        project.apply plugin: 'java-base'
        addSourceSets(project)
        addDependencies(project)
        addTasks(project)
    }

    private void addSourceSets(Project project)
    {
        project.sourceSets {
            main {
                java {
                    srcDirs = ['src']
                }
                output.classesDir = "${project.buildDir}/classes"
            }
        }

    }

    private void addDependencies(Project project)
    {
        project.dependencies
                {
                    compile 'org.apache.tomcat:tomcat-api',
                            'org.apache.tomcat:catalina',
                            'org.apache.tomcat:tomcat-juli',
                            'org.apache.tomcat:tomcat-util'
                }
    }

    private void addTasks(Project project)
    {
        project.jar {
            baseName "labkeyBootstrap"
        }
        project.processResources.enabled = false
        project.jar.manifest {
            attributes provider: 'LabKey'
            attributes 'Main-Class': BOOTSTRAP_MAIN_CLASS
        }

        def Task copyBootstrapJar = project.task(
                'copyBootstrapJar',
                group: GroupNames.DEPLOY,
                type: Copy,
                description: "Copy LabKey bootstrap jar to Tomcat",
                {
                    from project.jar
                    into "${project.tomcatDir}/lib"
                }
        )
        copyBootstrapJar.dependsOn(project.jar)
        project.project(":server").tasks.deployApp.dependsOn(copyBootstrapJar)

        def Task createApiFilesList = project.task(
                'createApiFilesList',
                group: GroupNames.DEPLOY,
                description: 'Create an index of the files in the application so extraneous files can be removed during bootstrapping',
                type: JavaExec,
                {
                    main = "org.labkey.bootstrap.DirectoryFileListWriter"
                    workingDir = project.staging.webappDir
                    classpath {
                        [
                                project.jar
                        ]
                    }
                }
        )
        createApiFilesList.dependsOn(project.jar)
        project.project(":server").tasks.deployApp.dependsOn(createApiFilesList)
    }
}
