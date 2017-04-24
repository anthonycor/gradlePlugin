package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.DeleteSpec
import org.gradle.api.tasks.Delete
import org.labkey.gradle.plugin.extension.TomcatExtension
import org.labkey.gradle.plugin.extension.UiTestExtension
import org.labkey.gradle.task.StartTomcat
import org.labkey.gradle.task.StopTomcat
import org.labkey.gradle.util.GroupNames
/**
 * Plugin for starting and stopping tomcat
 */
class Tomcat implements Plugin<Project>
{
    @Override
    void apply(Project project)
    {
        project.extensions.create("tomcat", TomcatExtension)
        if (project.plugins.hasPlugin(TestRunner.class))
        {
            UiTestExtension testEx = (UiTestExtension) project.getExtensions().getByType(UiTestExtension.class)
            project.tomcat.assertionFlag = Boolean.valueOf(testEx.getTestConfig("disableAssertions")) ? "-da" : "-ea"
        }
        project.tomcat.catalinaOpts = "-Dproject.root=${project.rootProject.projectDir.absolutePath}"

        addTasks(project)
    }


    private static void addTasks(Project project)
    {
        project.task(
                "startTomcat",
                group: GroupNames.WEB_APPLICATION,
                description: "Start the local Tomcat instance",
                type: StartTomcat
        )
        project.task(
                "stopTomcat",
                group: GroupNames.WEB_APPLICATION,
                description: "Stop the local Tomcat instance",
                type: StopTomcat
        )
        project.task(
                "cleanLogs",
                group: GroupNames.WEB_APPLICATION,
                description: "Delete logs from ${project.tomcatDir}",
                type: Delete,
                {
                    DeleteSpec spec -> spec.delete project.fileTree("${project.tomcatDir}/logs")
                }
        )
        Task cleanTempTask = project.task(
                "cleanTemp",
                group: GroupNames.WEB_APPLICATION,
                description: "Delete temp files from ${project.tomcatDir}"
        )
        // Note that we use the AntBuilder here because a fileTree in Gradle is a set of FILES only.
        // Deleting a file tree will delete all the leaves of the directory structure, but none of the
        // directories.
        cleanTempTask.doLast {
            project.ant.delete(includeEmptyDirs: true)
                    {
                        fileset(dir: "${project.tomcatDir}/temp")
                                {
                                    include(name: "**/*")
                                }
                    }
        }
    }
}

