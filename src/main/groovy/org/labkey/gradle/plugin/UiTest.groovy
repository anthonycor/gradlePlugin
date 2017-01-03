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
        // For now we return false here because the server/test project references the test/src directory as well
        // and IntelliJ doesn't like it when two projects reference the same source.
        return false;
        // TODO we might be able to get rid of the dependency on the :server:test project if we publish the test jar,
        // but some modules probably reach into the server/test directory in undocumented ways.
//        return project.file(TEST_SRC_DIR).exists() && project.findProject(":server:test") != null
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
                    srcDirs = [TEST_SRC_DIR]
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
