package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Delete
import org.labkey.gradle.task.ConfigureLog4J
import org.labkey.gradle.task.DeployApp
import org.labkey.gradle.task.DoThenSetup

/**
 * Created by susanh on 8/8/16.
 */
class ServerDeploy implements Plugin<Project>
{
    public static final String GROUP_NAME = "Deploy"

    @Override
    void apply(Project project)
    {
        project.extensions.create("serverDeploy", ServerDeployExtension)

        project.serverDeploy {
            dir = "${project.rootProject.buildDir}/deploy"
            modulesDir = "${dir}/modules"
            webappDir = "${dir}/labkeyWebapp"
            binDir = "${dir}/bin"
            rootWebappsDir = "${project.rootDir}/webapps"
        }
        addTasks(project)
    }

    private static void addTasks(Project project)
    {
        def Task deployAppTask = project.task(
                "deployApp",
                group: GROUP_NAME,
                type: DeployApp,
                description: "Deploy the application locally into ${project.serverDeploy.dir}"
        )

        def Task setup = project.task(
                "setup",
                group: GROUP_NAME,
                type: DoThenSetup,
                description: "Installs labkey.xml and various jar files into the tomcat directory.  Sets default database properties."
        )
        deployAppTask.dependsOn(setup)

        def Task log4jTask = project.task(
                'configureLog4j',
                group: GROUP_NAME,
                type: ConfigureLog4J,
                description: "Edit and copy log4j.xml file",
        )
        deployAppTask.dependsOn(log4jTask)

        project.task(
                'clean',
                group: GROUP_NAME,
                type: Delete,
                description: "Remove the deploy directory (${project.serverDeploy.dir})",
                {
                    delete project.serverDeploy.dir
                }
        )

        def Task cleanDeploy = project.task(
                "cleanAndDeploy",
                group: GROUP_NAME,
                type: DeployApp,
                description: "Deploy the application locally into ${project.serverDeploy.dir}",
        )
        cleanDeploy.doFirst{
            project.delete(project.serverDeploy.dir)
        }


    }
}


class ServerDeployExtension
{
    def String dir
    def String modulesDir
    def String webappDir
    def String binDir
    def String rootWebappsDir
}

