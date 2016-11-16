package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.labkey.gradle.task.PackageDistribution
import org.labkey.gradle.util.GroupNames

class Distribution implements Plugin<Project>
{
    public static final String DIRECTORY = "distributions"

    public static boolean isApplicable(Project project)
    {
        return project.file(DIRECTORY).exists()
    }

    @Override
    void apply(Project project)
    {

        project.extensions.create("dist", DistributionExtension)

        project.dist {
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
        def Task dist = project.task(
                "distribution",
                group: GroupNames.DISTRIBUTION,
                description: "Make LabKey distribution for a single module",
                type: PackageDistribution
        )
        dist.dependsOn(project.configurations.distribution)
        if (project.rootProject.hasProperty("distAll"))
            project.rootProject.tasks.distAll.dependsOn(dist)
    }

    /**
     * This method is used within the distribution build.gradle files to allow distributions
     * to easily build upon one another.
     * @param project the project that is to inherit dependencies
     * @param inheritedProjectPath the project whose dependencies are inherited
     */
    public static void inheritDependencies(Project project, String inheritedProjectPath)
    {
        project.project(inheritedProjectPath).configurations.distribution.dependencies.each {
            project.dependencies.add("distribution", it)
        }
    }
}


class DistributionExtension
{
    def String dir = "dist"
    def String distModulesDir
    def String type = "modules"

    // properties used in the installer/build.xml file
    def String subDirName
    def String extraFileIdentifier
    def Boolean skipWindowsInstaller
    def Boolean skipZipDistribution
    def Boolean skipTarGZDistribution
    def Boolean versionPrefix

}