package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.DeleteSpec
import org.gradle.api.tasks.Delete
import org.labkey.gradle.task.PackageDistribution
import org.labkey.gradle.util.BuildUtils
import org.labkey.gradle.util.GroupNames

class Distribution implements Plugin<Project>
{
    public static final String DIRECTORY = "distributions"

    static boolean isApplicable(Project project)
    {
        return project.file(DIRECTORY).exists()
    }

    @Override
    void apply(Project project)
    {
        project.buildDir = "${project.rootDir}/build/dist/${project.name}"
        DistributionExtension extension = project.extensions.create("dist", DistributionExtension)
        extension.distModulesDir = "${project.rootProject.buildDir}/distModules"

        addConfigurations(project)
        addTasks(project)

        project.dist.dir = "${project.rootProject.projectDir}/dist"
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
        Task dist = project.task(
                "distribution",
                group: GroupNames.DISTRIBUTION,
                description: "Make a LabKey distribution",
                type: PackageDistribution
        )
        dist.dependsOn(project.configurations.distribution)
        BuildUtils.addLabKeyDependency(
                project: project, config: 'tomcatJars', depProjectPath: ":server:bootstrap"
        )
        project.task(
                "cleanDist",
                group: GroupNames.DISTRIBUTION,
                description: "Remove the build directory for a distribution",
                type: Delete,
                { DeleteSpec delete ->
                    delete.delete project.buildDir
                }
        )
        if (project.rootProject.hasProperty("distAll"))
            project.rootProject.tasks.distAll.dependsOn(dist)

    }

    /**
     * This method is used within the distribution build.gradle files to allow distributions
     * to easily build upon one another.
     * @param project the project that is to inherit dependencies
     * @param inheritedProjectPath the project whose dependencies are inherited
     */
    static void inheritDependencies(Project project, String inheritedProjectPath)
    {
        project.project(inheritedProjectPath).configurations.distribution.dependencies.each {
            project.dependencies.add("distribution", it)
        }
    }
}


class DistributionExtension
{
    public static final String DIST_FILE_DIR = "distExtra/labkeywebapp/WEB-INF/classes"
    public static final String DIST_FILE_NAME = "distribution"

    String dir = "dist"
    String distModulesDir
    String type = "modules"

    // properties used in the installer/build.xml file
    String subDirName
    String extraFileIdentifier = ""
    Boolean skipWindowsInstaller
    Boolean skipZipDistribution
    Boolean skipTarGZDistribution
    Boolean includeMassSpecBinaries = false
    String versionPrefix
    String labkeyInstallerVersion
}