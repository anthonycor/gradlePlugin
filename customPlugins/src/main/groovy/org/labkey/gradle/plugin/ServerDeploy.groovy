package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Delete
import org.labkey.gradle.task.*

/**
 * First stages then deploys the application locally to the tomcat directory
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

        def Task stageAppTask = project.task(
                "stageApp",
                group: GROUP_NAME,
                type: StageApp,
                description: "Stage the modules for the application into ${project.staging.dir}"
        )
        deployAppTask.dependsOn(stageAppTask)

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
                'undeployModules',
                group: GROUP_NAME,
                description: "Moves the module files out of the deploy directory and back to staging",
                type: UndeployModules
        )

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

