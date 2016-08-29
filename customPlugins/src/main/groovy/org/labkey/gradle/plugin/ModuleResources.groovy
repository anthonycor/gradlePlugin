package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Created by susanh on 4/11/16.
 */
class ModuleResources implements Plugin<Project>
{
    private static final String DIR_NAME = "resources"

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
                    module {
                        resources {
                            srcDirs = [DIR_NAME]
                            exclude "schemas/**/obsolete/**"
                        }
                        output.resourcesDir = project.labkey.explodedModuleDir
                    }
                }
        project.tasks.processResources.dependsOn('processModuleResources')
    }
}
