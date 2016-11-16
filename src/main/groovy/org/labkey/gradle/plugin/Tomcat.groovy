package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
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
        project.extensions.create("tomcat",TomcatExtension)
        addTasks(project)
    }


    private void addTasks(Project project)
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
    }
}

public class TomcatExtension
{
    def boolean devMode = true
    def String assertionFlag = "-ea" // set to -da to disable assertions
    def String maxMemory = "1G"
    def boolean recompileJsp = true
    def boolean sequencePipelineEnabled = false
    def String trustStore = ""
    def String trustStorePassword = ""
    def String catalinaOpts = ""

}
