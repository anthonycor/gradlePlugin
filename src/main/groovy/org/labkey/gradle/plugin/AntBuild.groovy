package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.PluginInstantiationException

/**
 * Plugin that will import the tasks from an ant build.xml file and add a dependency between the
 * project's build task and the default target in the build.xml file.
 */
class AntBuild implements Plugin<Project>
{
    private static final String ANT_BUILD_FILE = "build.xml"
    private static final String USE_GRADLE_PROPERTY = "useGradleBuild"

    public static boolean isApplicable(Project project)
    {
        return project.file(ANT_BUILD_FILE).exists() && !project.file("build.gradle").exists() && !project.hasProperty(USE_GRADLE_PROPERTY)
    }

    @Override
    void apply(Project project)
    {
        setAntProperties(project)

        project.ant.importBuild(ANT_BUILD_FILE)
                { antTargetName ->
                    'ant_' + antTargetName
                }


        if (project.ant.properties['ant.project.default-target'] != null && project.hasProperty("build"))
        {
            Task antBuildTask = project.tasks['ant_' + project.ant.properties['ant.project.default-target']]
            project.tasks.build.dependsOn(antBuildTask)
        }
        else
        {
            throw new PluginInstantiationException("No default target defined in ${project.file(ANT_BUILD_FILE)}")
        }
    }

    private static void setAntProperties(Project project)
    {
        project.ant.setProperty('basedir', project.project(":server").projectDir)
        project.ant.setProperty('modules.dir', project.projectDir.parent)
        project.ant.setProperty('build.modules.dir', project.buildDir.parent)
        project.ant.setProperty('build.dir', project.rootProject.buildDir)
        project.ant.setProperty('explodedModuleDir', project.labkey.explodedModuleDir)
        project.ant.setProperty('java.source.and.target', project.labkey.sourceCompatibility)
    }
}
