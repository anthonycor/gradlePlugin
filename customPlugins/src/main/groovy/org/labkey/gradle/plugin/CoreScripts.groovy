package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.labkey.gradle.task.ServerSideJS

/**
 * Created by susanh on 8/11/16.
 */
class CoreScripts implements Plugin<Project>
{
    @Override
    void apply(Project project)
    {
        addTasks(project)
    }

    private void addTasks(Project project)
    {
        def Task task = project.task("serverSideJS",
                group: "Code generation",
                description: "Concatenate javascript files for use on the server side",
                type: ServerSideJS
        )
        if (project.hasProperty("module"))
            project.tasks.module.dependsOn(task)
    }
}