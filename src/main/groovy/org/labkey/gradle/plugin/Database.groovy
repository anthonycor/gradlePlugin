package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.labkey.gradle.task.Bootstrap
import org.labkey.gradle.task.PickDb
import org.labkey.gradle.util.GroupNames

/**
 * Adds stand-alone tasks for setting database properties to be used by the server
 */
class Database implements Plugin<Project>
{
    @Override
    void apply(Project project)
    {
        addPickPgTask(project)
        addPickMSSQLTask(project)
        addBootstrapTask(project)
    }

    private void addPickPgTask(Project project)
    {
        project.task("pickPg",
            group: GroupNames.DATABASE,
            type: PickDb,
            description: "Switch to PostgreSQL configuration",
            {PickDb task ->
                task.dbType = "pg"
            }
        )
    }
    private void addPickMSSQLTask(Project project)
    {
        project.task("pickMSSQL",
            group: GroupNames.DATABASE,
            type: PickDb,
            description: "Switch to SQL Server configuration",
            {PickDb task ->
                task.dbType = "mssql"
            }
        )
    }

    private void addBootstrapTask(Project project)
    {
        project.task("bootstrap",
            group: GroupNames.DATABASE,
            type: Bootstrap,
            description: "Switch to bootstrap database properties as defined in current db.config file"
        )
    }
}


