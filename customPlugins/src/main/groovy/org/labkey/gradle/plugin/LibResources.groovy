package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * This is meant to copy external jar files in a module's lib directory to the webapp lib directory.
 * CONSIDER: will the "application" plugin do a better job here?
 */
class LibResources implements Plugin<Project>
{
    private static final String DIR_NAME = "lib"

    public static boolean isApplicable(Project project)
    {
        return project.file(DIR_NAME).exists()
    }

    @Override
    void apply(Project project)
    {
        project.apply plugin: 'java-base'
        addSourceSet(project)
    }

    private void addSourceSet(Project project)
    {
        project.sourceSets
                {
                    libs {
                        resources {
                            srcDirs = [DIR_NAME]
                            include '*.jar'
                        }
                        output.resourcesDir = project.labkey.webappLibDir
                    }
                }
        project.tasks.processResources.dependsOn('processLibsResources')
    }
}
