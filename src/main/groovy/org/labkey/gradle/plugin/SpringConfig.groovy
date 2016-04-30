package org.labkey.gradle.plugin

import org.gradle.api.Project

/**
 * Created by susanh on 4/20/16.
 */
class SpringConfig extends LabKey
{
    def Project _project;
    def String _configDir;

    @Override
    void apply(Project project)
    {
        _project = project;
        _configDir = "${_project.labkey.explodedModuleDir}/config"
        project.apply plugin: 'java-base'

        addSourceSet()
    }

    private void addSourceSet()
    {
        _project.sourceSets
                {
                    spring {
                        resources {
                            srcDirs = ["webapp/WEB-INF/${_project.name}"]
                        }
                        output.resourcesDir = _configDir
                    }
                }
        _project.tasks.processResources.dependsOn('processSpringResources')
    }
}