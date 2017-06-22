/*
 * Copyright (c) 2016-2017 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
            setDefaultJdbcProperties(useBootstrap, true)
    }

    static File getConfigFile(Project project)
    {
        return project.project(":server").file(DATABASE_CONFIG_FILE)
    }

    static Boolean hasConfigFile(Project project)
    {
        return getConfigFile(project).exists()
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

    void setDefaultJdbcProperties(Boolean bootstrap, Boolean doInterpolation = true)
    {
        if (this.configProperties.getProperty(JDBC_DATABASE_PROP) == null)
        {
            if (bootstrap)
                this.configProperties.setProperty(JDBC_DATABASE_PROP, getConfigProperty(BOOTSTRAP_DB_PROP))
            else
                this.configProperties.setProperty(JDBC_DATABASE_PROP, getConfigProperty(DEFAULT_DB_PROP))
        }
        if (this.configProperties.getProperty(JDBC_HOST_PROP) == null)
            this.configProperties.setProperty(JDBC_HOST_PROP, getConfigProperty(DEFAULT_HOST_PROP))
        if (this.configProperties.getProperty(JDBC_PORT_PROP) == null)
            this.configProperties.setProperty(JDBC_PORT_PROP, getConfigProperty(DEFAULT_PORT_PROP))
        if (this.configProperties.getProperty(JDBC_URL_PARAMS_PROP) == null)
            this.configProperties.setProperty(JDBC_URL_PARAMS_PROP, "")
        if (doInterpolation)
            setJdbcUrl()
    }

    private String getConfigProperty(String property, defaultValue="")
    {
        if (this.configProperties.get(property) != null)
            return (String) this.configProperties.get(property)
        else
        {
            project.logger.info("Default database config property ${property} not defined; returning '${defaultValue}'.")
            return defaultValue
        }
    }

    void setJdbcUrl()
    {
        this.configProperties.setProperty(JDBC_URL_PROP, PropertiesUtils.parseCompositeProp(project, this.configProperties, this.configProperties.getProperty(JDBC_URL_PROP)))
    }

    void mergePropertiesFromFile(boolean doInterpolation)
    {
        Properties fileProperties = readDatabaseProperties(project)
        for (String name : fileProperties.propertyNames())
        {
            if (!this.configProperties.hasProperty(name))
            {
                this.configProperties.setProperty(name, fileProperties.getProperty(name))
            }
        }
        setDefaultJdbcProperties(false, doInterpolation)
    }

    void writeJdbcUrl()
    {
        writeDatabaseProperty(project, JDBC_URL_PROP, PropertiesUtils.parseCompositeProp(project, this.configProperties, this.configProperties.getProperty(JDBC_URL_PROP)))
    }


    static Properties readDatabaseProperties(Project project)
    {
        if (hasConfigFile(project))
        {
            Properties props = PropertiesUtils.readFileProperties(project.project(":server"), DATABASE_CONFIG_FILE)
            return props
        }
        else
        {
            project.logger.warn("No file ${DATABASE_CONFIG_FILE} found.  Returning empty properties.")
            return new Properties()
        }
    }

    private void writeDatabaseProperty(Project project, String name, String value)
    {
        project.ant.propertyfile(
                file: getConfigFile(project)
        )
                {
                    entry( key: name, value: value)
                }
    }
}
