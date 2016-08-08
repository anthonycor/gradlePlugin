package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.labkey.gradle.task.ConfigureLog4J
import org.labkey.gradle.task.DeployApp

/**
 * Created by susanh on 8/8/16.
 */
class ServerDeploy implements Plugin<Project>
{
    private static final String GROUP_NAME = "Deploy"

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

        def Task log4jTask = project.task(
                'configureLog4j',
                group: GROUP_NAME,
                type: ConfigureLog4J,
                description: "Edit and copy log4j.xml file",
        )
        deployAppTask.dependsOn(log4jTask)
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

