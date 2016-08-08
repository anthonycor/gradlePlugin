package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.labkey.gradle.task.ConfigureLog4J
import org.labkey.gradle.task.DeployApp
import org.labkey.gradle.task.PackageDistribution

class Distribution implements Plugin<Project>
{
    private static final String GROUP_NAME = "Distribution"

    @Override
    void apply(Project project)
    {

        project.extensions.create("dist", DistributionExtension)

        project.dist {
            deployDir = "${project.rootProject.buildDir}/deploy"
            deployModulesDir = "${deployDir}/modules"
            deployWebappDir = "${deployDir}/labkeyWebapp"
            deployBinDir = "${deployDir}/bin"
            distModulesDir = "${project.rootProject.buildDir}/distModules"
        }
        addConfigurations(project)
        addTasks(project)
    }

    private void addConfigurations(Project project)
    {
        project.configurations
                {
                    distribution
                }
    }

    private static void addTasks(Project project)
    {
        def Task deployAppTask = project.task(
                "deployApp",
                group: GROUP_NAME,
                type: DeployApp,
                description: "Deploy the application locally into ${project.dist.deployDir}"
        )

        def Task log4jTask = project.task(
                'configureLog4j',
                group: GROUP_NAME,
                type: ConfigureLog4J,
                description: "Edit and copy log4j.xml file",
        )
//        project.tasks.build.dependsOn(log4jTask)
        deployAppTask.dependsOn(log4jTask)

        def Task dist = project.task(
                "distribution",
                group: GROUP_NAME,
                description: "Make distributions",
                type: PackageDistribution
        )
    }

    public static void inheritDependencies(Project project, String inheritedProjectPath)
    {
        project.project(inheritedProjectPath).configurations.distribution.dependencies.each {
            project.dependencies.add("distribution", it)
        }
    }
}


class DistributionExtension
{
    def String deployDir
    def String deployModulesDir
    def String deployWebappDir
    def String deployBinDir

    def String distModulesDir

    def Map<String, Object> properties

    def String subDirName
    def String extraFileIdentifier
}