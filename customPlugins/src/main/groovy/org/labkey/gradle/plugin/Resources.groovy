package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Created by susanh on 4/11/16.
 */
class Resources implements Plugin<Project>
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
                    // N.B. putting this resources section in the main sourceSet in Module.groovy
                    // causes an infinite recursion when creating the jar file.
                    base {
                        resources {
                            srcDirs = ['resources']
                            exclude "schemas/**/obsolete/**"
                        }
                        output.resourcesDir = project.labkey.explodedModuleDir
                    }
                }
        project.tasks.processResources.dependsOn('processBaseResources')
    }
}
