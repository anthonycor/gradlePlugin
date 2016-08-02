package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Created by susanh on 4/11/16.
 */
class Webapp implements Plugin<Project>
{
    private static final String EXTJS_DIRNAME = "ext-3.4.1"
    private static final String EXTJS42_DIRNAME = "ext-4.2.1"

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
                            if (project.labkey.deployMode != LabKey.DeployMode.dev)
                            {
                                // We should only redistribute the ExtJS resource files, not the full dev kit
                                exclude "${EXTJS_DIRNAME}/src/**"
                                exclude "${EXTJS42_DIRNAME}/builds/**"
                                exclude "${EXTJS42_DIRNAME}/cmd/**"
                                exclude "${EXTJS42_DIRNAME}/locale/**"
                                exclude "${EXTJS42_DIRNAME}/src/**"
                                exclude "d3/examples/**"
                                exclude "d3/test/**"
                            }
                            else
                            {
                                include 'share/**'
                                include 'WEB-INF/**'
                            }
                        }
                        output.resourcesDir = "${project.labkey.explodedModuleDir}/web"
                    }
                }
        project.tasks.processResources.dependsOn('processWebappResources')
    }
}
