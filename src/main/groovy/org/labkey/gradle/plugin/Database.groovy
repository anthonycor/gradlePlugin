package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

import org.labkey.gradle.util.SqlUtils
import org.labkey.gradle.util.ParsingUtils

import org.labkey.gradle.task.DoThenSetup

/**
 * Created by Joe on 7/12/2016.
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

class PickDb extends DoThenSetup
{
    def String dbType;

    def Closure<Void> fn = {

        //ant pick_[pg|mssql|db]
        //copies the correct config file.
        project.copy({
            from "${project.projectDir}${File.separator}configs"
            into "${project.projectDir}"
            include "${dbType}.properties"
            rename { String fileName ->
                fileName.replace("${dbType}", "config")
            }
        })

        super.getFn().run();
    }
}

class Bootstrap extends DoThenSetup
{
    def Closure<Void> fn = {
        initDatabaseProperties(project);

        Properties configProperties = ParsingUtils.readConfigProperties(project);

        project.ext.jdbcDatabase = project.ext.databaseBootstrap;
        project.ext.jdbcHost = project.ext.databaseDefaultHost;
        project.ext.jdbcPort = project.ext.databaseDefaultPort;
        project.ext.jdbcURLParameters = "";

        project.ext.jdbcURL = ParsingUtils.parseCompositeProp(project.ext.properties, configProperties.getProperty("jdbcURL"));

        SqlUtils.dropDatabase(project, true);
    }
}
