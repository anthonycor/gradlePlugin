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

    Properties configProperties
    Project project

    DatabaseProperties(String dbTypeAndVersion, String shortType, version)
    {
        this.dbTypeAndVersion = dbTypeAndVersion
        this.shortType = shortType
        this.version = version
        this.configProperties = new Properties()
    }

    DatabaseProperties(Project project, Boolean useBootstrap)
    {
        this.project = project
        this.configProperties = readDatabaseProperties(project)
        if (!this.configProperties.isEmpty())
            setJdbcProperties(useBootstrap)
    }

    static Boolean hasConfigFile(Project project)
    {
        return project.project(":server").file(DATABASE_CONFIG_FILE).exists()
    }

    void setProject(Project project)
    {
        this.project = project
    }

    void setJdbcURL(String jdbcURL)
    {
        this.configProperties.setProperty(JDBC_URL_PROP, jdbcURL)
    }

    String getJdbcURL()
    {
        return this.configProperties.get(JDBC_URL_PROP)
    }

    void setJdbcDatabase(String database)
    {
        this.configProperties.setProperty(JDBC_DATABASE_PROP, database)
    }

    String getJdbcDatabase()
    {
        return this.configProperties.get(JDBC_DATABASE_PROP)
    }


    void setJdbcPort(String port)
    {
        this.configProperties.setProperty(JDBC_PORT_PROP, port)
    }

    String getJdbcPort()
    {
        return this.configProperties.get(JDBC_PORT_PROP)
    }

    private void setJdbcProperties(Boolean bootstrap)
    {
        if (bootstrap)
            this.configProperties.setProperty(JDBC_DATABASE_PROP, (String) this.configProperties.get(BOOTSTRAP_DB_PROP))
        else
            this.configProperties.setProperty(JDBC_DATABASE_PROP, (String) this.configProperties.get(DEFAULT_DB_PROP))
        this.configProperties.setProperty(JDBC_HOST_PROP, (String) this.configProperties.get(DEFAULT_HOST_PROP))
        this.configProperties.setProperty(JDBC_PORT_PROP, (String) this.configProperties.get(DEFAULT_PORT_PROP))
        this.configProperties.setProperty(JDBC_URL_PARAMS_PROP, "")
        this.configProperties.setProperty(JDBC_URL_PROP, PropertiesUtils.parseCompositeProp(this.configProperties, this.configProperties.getProperty(JDBC_URL_PROP)))
        this.project.ext.dbProperties = this.configProperties
    }

    void mergePropertiesFromFile()
    {
        Properties fileProperties = readDatabaseProperties(project)
        for (String name : fileProperties.propertyNames())
        {
            if (!this.configProperties.hasProperty(name))
            {
                this.configProperties.setProperty(name, fileProperties.getProperty(name))
            }
        }
        this.configProperties.setProperty(JDBC_URL_PROP, PropertiesUtils.parseCompositeProp(this.configProperties, this.configProperties.getProperty(JDBC_URL_PROP)))
        this.ext.dbProperties = this.configProperties
    }


    static Properties readDatabaseProperties(Project project)
    {
        if (hasConfigFile(project))
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
