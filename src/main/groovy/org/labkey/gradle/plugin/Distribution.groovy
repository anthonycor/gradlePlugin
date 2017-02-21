package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.artifacts.dependencies.DefaultProjectDependency

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
        project.extensions.create("dist", DistributionExtension, project)

        addConfigurations(project)
        addTasks(project)
        // This block sets up the task dependencies for each configuration dependency.
        project.afterEvaluate {
            if (project.hasProperty("distribution"))
            {
                Task distTask = project.tasks.distribution
                project.configurations.distribution.dependencies.each {
                    if (it instanceof DefaultProjectDependency)
                    {
                        distTask.dependsOn(((DefaultProjectDependency) it).dependencyProject.tasks.module)
                    }
                }
            }
        }
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
//        Task dist = project.task(
//                "distribution",
//                group: GroupNames.DISTRIBUTION,
//                description: "Make a LabKey modules distribution",
//                type: ModuleDistribution
//        )
//        dist.dependsOn(project.configurations.distribution)
//        dist.dependsOn(project.project(":server").tasks.stageApp)
//        BuildUtils.addLabKeyDependency(
//                project: project, config: 'tomcatJars', depProjectPath: ":server:bootstrap"
//        )
//        // TODO make this clean out the output files, not just the build directory
//        project.task(
//                "cleanDist",
//                group: GroupNames.DISTRIBUTION,
//                description: "Remove the build directory for a distribution",
//                type: Delete,
//                { DeleteSpec delete ->
//                    delete.delete installerBuildDir
//                }
//        )
//        if (project.rootProject.hasProperty("distAll"))
//            project.rootProject.tasks.distAll.dependsOn(dist)

    }

    /**
     * This method is used within the distribution build.gradle files to allow distributions
     * to easily build upon one another.
     * @param project the project that is to inherit dependencies
     * @param inheritedProjectPath the project whose dependencies are inherited
     */
    static void inheritDependencies(Project project, String inheritedProjectPath)
    {
        // Unless otherwise indicated, projects are evaluated in alphanumeric order, so
        // we explicitly indicate that the project to be inherited from must be evaluated first.
        // Otherwise, there will be no dependencies to inherit.
        project.evaluationDependsOn(inheritedProjectPath)
        project.project(inheritedProjectPath).configurations.distribution.dependencies.each {
            project.dependencies.add("distribution", it)
        }
    }
}


class DistributionExtension
{
    public static final String DIST_FILE_DIR = "distExtra/labkeywebapp/WEB-INF/classes"
    public static final String DIST_FILE_NAME = "distribution"
    public static final String VERSION_FILE_NAME = "VERSION"

    String dir
    String modulesDir
    String installerSrcDir
    String extraSrcDir
    String archiveDataDir
    String type = "modules"

    // properties used in the installer/build.xml file
    String subDirName
    String extraFileIdentifier = ""
    Boolean skipWindowsInstaller
    Boolean skipZipDistribution
    Boolean skipTarGZDistribution
    Boolean includeMassSpecBinaries = false
    String versionPrefix = null
    private Project project

    DistributionExtension(Project project)
    {
        this.project = project
        this.modulesDir = "${project.rootProject.buildDir}/distModules"
        this.dir = "${project.rootProject.projectDir}/dist"
        this.installerSrcDir = "${project.rootProject.projectDir}/server/installer"
        this.extraSrcDir = "${project.rootProject.buildDir}/distExtra"
        this.archiveDataDir = "${this.installerSrcDir}/archivedata"
    }

    Boolean buildInstallerExes()
    {
        return skipWindowsInstaller == null ? true: !skipWindowsInstaller
    }
}