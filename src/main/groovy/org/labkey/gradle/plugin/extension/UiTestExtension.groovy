package org.labkey.gradle.plugin.extension

import org.gradle.api.Project
import org.labkey.gradle.util.DatabaseProperties
import org.labkey.gradle.util.PropertiesUtils

/**
 * Created by susanh on 4/23/17.
 */
class UiTestExtension
{
    String propertiesFile = "test.properties"

    private Properties config = null
    private Project project

    UiTestExtension(Project project)
    {
        this.project = project
    }

    private void setConfig()
    {
        // read database configuration, but don't include jdbcUrl and other non-"database"
        // properties because they "cause problems" (quote from the test/build.xml file)
        DatabaseProperties dbProperties = new DatabaseProperties(project, false)
        this.config = new Properties()
        this.config.setProperty("debugSuspendSelenium", "n")
        for (String name : dbProperties.getConfigProperties().stringPropertyNames())
        {
            if (name.contains("database"))
                this.config.put(name, dbProperties.getConfigProperties().getProperty(name))
        }
        if (project.findProject(":server:test") != null)
        // read test.properties file
            PropertiesUtils.readProperties(project.project(":server:test").file(propertiesFile), this.config)
        // if the test.properties file is not available, all properties will need to be provided via project properties
        for (String name : config.propertyNames())
        {
            // two of the test.property names ('test' and 'clean') are the same as the
            // names of default tasks that come with the Java plugin.  All tasks are also
            // properties of a project, so we test for a String type (passed through the
            // command line) and override the property in the file only if we have a new
            // String.
            if (project.hasProperty(name) && project.property(name) instanceof String)
            {
                config.setProperty(name, project.property(name).toString())
            }
        }
    }

    Properties getConfig()
    {
        if (this.config == null)
            setConfig()
        return this.config;
    }

    Object getTestConfig(String name)
    {
        if (project.hasProperty(name))
            return project.property(name)
        else
        {
            return getConfig().get(name)
        }
    }

}
