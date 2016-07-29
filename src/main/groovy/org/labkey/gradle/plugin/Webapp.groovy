package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Created by susanh on 4/11/16.
 */
class Webapp implements Plugin<Project>
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
                    webapp {
                        resources {
                            srcDirs = ['webapp']
                            // The spring configuration files are copied by the SpringConfig plugin
                            exclude "WEB-INF/${project.name}/**"
                            // when in dev mode, the webapp files will be picked up from their original locations
                            if (project.labkey.deployMode == LabKey.DeployMode.dev)
                                include 'share/**', 'WEB-INF/**'

                        }
                        output.resourcesDir = "${project.labkey.explodedModuleDir}/web"
                    }
                }
        project.tasks.processResources.dependsOn('processWebappResources')
    }
}
