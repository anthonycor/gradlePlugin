package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.labkey.gradle.task.ConfigureLog4J

class Distribution implements Plugin<Project>
{
    @Override
    void apply(Project project)
    {
        addTasks(project)
    }

    void addTasks(Project project)
    {
        def Task log4jTask = project.task('configureLog4j',
                group: "distribution",
                type: ConfigureLog4J,
                description: "Edit and copy log4j.xml file",
        )
//        project.tasks.build.dependsOn(log4jTask)
    }

}
