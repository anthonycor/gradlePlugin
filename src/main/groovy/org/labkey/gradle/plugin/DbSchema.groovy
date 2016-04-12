package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Created by susanh on 4/11/16.
 */
class DbSchema implements Plugin<Project>
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
                    schemas {
                        resources {
                            srcDirs = ['resources']
                            exclude "schemas/**/obsolete/**"
                        }
                        output.resourcesDir = project.explodedModuleDir
                    }
                }
    }
}
