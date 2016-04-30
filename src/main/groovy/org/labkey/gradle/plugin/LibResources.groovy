package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Created by susanh on 4/11/16.
 * This is meant to copy external jar files in a module's lib directory to the webapp lib directory.
 * CONSIDER: will the "application" plugin do a better job here?
 */
class LibResources implements Plugin<Project>
{
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
                            srcDirs = ['lib']
                            include '*.jar'
                        }
                        output.resourcesDir = project.labkey.webappLibDir
                    }
                }
        project.tasks.processResources.dependsOn('processLibsResources')
    }
}
