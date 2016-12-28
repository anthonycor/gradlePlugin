package org.labkey.gradle.util

import org.gradle.api.Project

/**
 * Created by susanh on 12/19/16.
 */
class DatabaseProperties
{
    protected static final String DATABASE_CONFIG_FILE = "config.properties"
    String dbTypeAndVersion // e.g., postgres9.2
    String shortType // pg or mssql
    String version
    String jdbcURL
    String jdbcDatabase
    String jdbcPort
    String jdbcHost = "localhost"

    DatabaseProperties(String dbTypeAndVersion, String shortType, version)
    {
        this.dbTypeAndVersion = dbTypeAndVersion
        this.shortType = shortType
        this.version = version
    }

    static Properties readDatabaseProperties(Project project)
    {
        if (project.project(":server").file(DATABASE_CONFIG_FILE).exists())
        {
            Properties props = PropertiesUtils.readFileProperties(project.project(":server"), DATABASE_CONFIG_FILE);
            return props;
        }
        else
        {
            return new Properties()
        }
    }
}
