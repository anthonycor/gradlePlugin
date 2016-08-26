package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.labkey.gradle.task.Bootstrap
import org.labkey.gradle.task.PickDb

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
            group: 'Database',
            type: PickDb,
            description: "Switch to PostgreSQL configuration",
            {
                dbType = "pg"
            }
        )
    }
    private void addPickMSSQLTask(Project project)
    {
        project.task("pickMSSQL",
            group: 'Database',
            type: PickDb,
            description: "Switch to SQL Server configuration",
            {
                dbType = "mssql"
            }
        )
    }

    private void addBootstrapTask(Project project)
    {
        project.task("bootstrap",
            group: 'Database',
            type: Bootstrap,
            description: "Switch to bootstrap database properties as defined in current db.config file"
        )
    }
}


