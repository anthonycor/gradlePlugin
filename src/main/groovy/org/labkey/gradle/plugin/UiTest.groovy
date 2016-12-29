package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.labkey.gradle.task.RunUiTest
import org.labkey.gradle.util.BuildUtils
import org.labkey.gradle.util.DatabaseProperties
import org.labkey.gradle.util.GroupNames
import org.labkey.gradle.util.PropertiesUtils

/**
 * Created by susanh on 12/21/16.
 */
class UiTest implements Plugin<Project>
{
    UiTestExtension testRunnerExt

    public static final String TEST_SRC_DIR = "test/src"

    static Boolean isApplicable(Project project)
    {
        // TODO for now, we return false.  Most of the machinery is in place, but we need to work out the chrome extensions
        // Error: The driver executable does not exist: /Users/susanh/Development/labkey/trunk/server/modules/server/test/bin/mac/chromedriver
        return false
//        return project.file(TEST_SRC_DIR).exists()
    }

    @Override
    void apply(Project project)
    {
        testRunnerExt = project.extensions.create("uiTest", UiTestExtension, project)
        addSourceSets(project)
        addConfigurations(project)
        addDependencies(project)
        addTasks(project)
        addArtifacts(project)
    }

    protected void addConfigurations(Project project)
    {
        project.configurations {
            uiTestCompile.extendsFrom(compile)
        }
    }

    protected void addSourceSets(Project project)
    {
        project.sourceSets {
            uiTest {
                java {
                    srcDirs = ['test/src']
                }
                resources {
                    srcDirs = ["test/resources"]
                }
            }
        }
    }

    protected void addDependencies(Project project)
    {
        BuildUtils.addLabKeyDependency(project: project, config: 'uiTestCompile', depProjectPath: ":server:test", depProjectConfig: "uiTestCompile")

        BuildUtils.addLabKeyDependency(project: project, config: 'uiTestCompile', depProjectPath: ":schemas")
        BuildUtils.addLabKeyDependency(project: project, config: 'uiTestCompile', depProjectPath: ":server:api")
        BuildUtils.addLabKeyDependency(project: project, config: 'uiTestCompile', depProjectPath: ":remoteapi:java")
    }

    protected void addTasks(Project project)
    {
        project.task("uiTests",
                group: GroupNames.VERIFICATION,
                description: "Run UI (Selenium) tests for this module",
                type: RunUiTest
        )
    }

    protected void addArtifacts(Project project)
    {
        // nothing to do here for the base case.
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
        DatabaseProperties dbProperties = new DatabaseProperties(project, false)
        this.properties = new Properties()
        this.properties.setProperty("debugSuspendSelenium", "n")
        for (String name : dbProperties.getProperties().stringPropertyNames())
        {
            if (name.contains("database"))
                this.properties.put(name, dbProperties.getProperties().getProperty(name))
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
