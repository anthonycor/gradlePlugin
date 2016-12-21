package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.labkey.gradle.task.RunUiTest
import org.labkey.gradle.util.GroupNames
import org.labkey.gradle.util.PropertiesUtils

/**
 * Created by susanh on 12/21/16.
 */
class UiTest implements Plugin<Project>
{
    UiTestExtension testRunnerExt

    @Override
    void apply(Project project)
    {
        testRunnerExt = project.extensions.create("uiTest", UiTestExtension, project)
        addTasks(project)
    }

    protected void addTasks(Project project)
    {
        project.task("uiTests",
                group: GroupNames.VERIFICATION,
                description: "Run UI (Selenium) tests for this module",
                type: RunUiTest
        )
    }
}

class UiTestExtension
{
    String propertiesFile = "test.properties"

    private Properties properties = null
    private Project project

    UiTestExtension(Project project)
    {
        this.project = project
        setProperties(project);
    }

    private void setProperties(Project project)
    {
        // read database configuration, but don't include jdbcUrl and other non-"database"
        // properties because they "cause problems" (quote from the test/build.xml file)
        Properties dbProperties = PropertiesUtils.readDatabaseProperties(project)
        this.properties = new Properties();
        this.properties.setProperty("debugSuspendSelenium", "n")
        for (String name : dbProperties.stringPropertyNames())
        {
            if (name.contains("database"))
                this.properties.put(name, dbProperties.getProperty(name))
        }
        // read test.properties file
        PropertiesUtils.readProperties(project.file(propertiesFile), this.properties)
        for (String name : properties.propertyNames())
        {
            // two of the test.property names ('test' and 'clean') are the same as the
            // names of default tasks that come with the Java plugin.  All tasks are also
            // properties of a project, so we test for a String type (passed through the
            // command line) and override the property in the file only if we have a new
            // String.
            if (project.hasProperty(name) && project.property(name) instanceof String)
            {
                properties.setProperty(name, project.property(name).toString())
            }

        }
    }

    Properties getProperties()
    {
        return this.properties;
    }

    Object getTestProperty(String name)
    {
        if (project.hasProperty(name))
            return project.property(name)
        else
        {
            return properties.get(name)
        }
    }

}
