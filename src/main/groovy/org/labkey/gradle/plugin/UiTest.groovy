package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.labkey.gradle.plugin.extension.UiTestExtension
import org.labkey.gradle.task.RunUiTest
import org.labkey.gradle.util.BuildUtils
import org.labkey.gradle.util.GroupNames

/**
 * Created by susanh on 12/21/16.
 */
class UiTest implements Plugin<Project>
{
    UiTestExtension testRunnerExt

    public static final String TEST_SRC_DIR = "test/src"

    static Boolean isApplicable(Project project)
    {
        // For now we rely on the enableUiTests property here to allow use of individual test running
        // from the command line because the server/test project references the test/src directory as well
        // and IntelliJ doesn't like it when two projects reference the same source.

        // TODO we might be able to get rid of the dependency on the :server:test project if we publish the test jar,
        // but some modules probably reach into the server/test directory in undocumented ways.
        return project.hasProperty("enableUiTests") && project.file(TEST_SRC_DIR).exists() && project.findProject(":server:test") != null
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

