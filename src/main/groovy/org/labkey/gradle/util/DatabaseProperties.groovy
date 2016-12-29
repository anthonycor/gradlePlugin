package org.labkey.gradle.util

import org.gradle.api.Project

/**
 * Created by susanh on 12/19/16.
 */
class DatabaseProperties
{
    private static final String DATABASE_CONFIG_FILE = "config.properties"

    private static final String JDBC_URL_PROP = "jdbcURL"
    private static final String JDBC_PORT_PROP = "jdbcPort"
    private static final String JDBC_DATABASE_PROP = "jdbcDatabase"
    private static final String JDBC_HOST_PROP = "jdbcHost"
    private static final String JDBC_URL_PARAMS_PROP = "jdbcURLParameters"
    private static final String BOOTSTRAP_DB_PROP = "databaseBootstrap"
    private static final String DEFAULT_DB_PROP = "databaseDefault"
    private static final String DEFAULT_HOST_PROP = "databaseDefaultHost"
    private static final String DEFAULT_PORT_PROP = "databaseDefaultPort"

    String dbTypeAndVersion // e.g., postgres9.2
    String shortType // pg or mssql
    String version // database version, e.g. 9.2

    Properties properties
    Project project

    DatabaseProperties(String dbTypeAndVersion, String shortType, version)
    {
        this.dbTypeAndVersion = dbTypeAndVersion
        this.shortType = shortType
        this.version = version
        this.properties = new Properties()
    }

    DatabaseProperties(Project project, Boolean useBootstrap)
    {
        this.project = project
        this.properties = readDatabaseProperties(project)
        setJdbcProperties(useBootstrap)
    }

    void setProject(Project project)
    {
        this.project = project
    }

    void setJdbcURL(String jdbcURL)
    {
        this.properties.setProperty(JDBC_URL_PROP, jdbcURL)
    }

    String getJdbcURL()
    {
        return this.properties.get(JDBC_URL_PROP)
    }

    void setJdbcDatabase(String database)
    {
        this.properties.setProperty(JDBC_DATABASE_PROP, database)
    }

    String getJdbcDatabase()
    {
        return this.properties.get(JDBC_DATABASE_PROP)
    }


    void setJdbcPort(String port)
    {
        this.properties.setProperty(JDBC_PORT_PROP, port)
    }

    String getJdbcPort()
    {
        return this.properties.get(JDBC_PORT_PROP)
    }

    private void setJdbcProperties(Boolean bootstrap)
    {
        if (bootstrap)
            this.properties.setProperty(JDBC_DATABASE_PROP, (String) this.properties.get(BOOTSTRAP_DB_PROP))
        else
            this.properties.setProperty(JDBC_DATABASE_PROP, (String) this.properties.get(DEFAULT_DB_PROP))
        this.properties.setProperty(JDBC_HOST_PROP, (String) this.properties.get(DEFAULT_HOST_PROP))
        this.properties.setProperty(JDBC_PORT_PROP, (String) this.properties.get(DEFAULT_PORT_PROP))
        this.properties.setProperty(JDBC_URL_PARAMS_PROP, "")
        this.properties.setProperty(JDBC_URL_PROP, PropertiesUtils.parseCompositeProp(this.properties, this.properties.getProperty(JDBC_URL_PROP)))
        this.project.ext.dbProperties = this.properties
    }

    void mergePropertiesFromFile()
    {
        Properties fileProperties = readDatabaseProperties(project)
        for (String name : fileProperties.propertyNames())
        {
            if (!this.properties.hasProperty(name))
            {
                this.properties.setProperty(name, fileProperties.getProperty(name))
            }
        }
        this.properties.setProperty(JDBC_URL_PROP, PropertiesUtils.parseCompositeProp(this.properties, this.properties.getProperty(JDBC_URL_PROP)))
        this.ext.dbProperties = this.properties
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
