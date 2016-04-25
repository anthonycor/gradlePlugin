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
                            include 'share/**', 'WEB-INF/**'
                            // we can exclude the spring configuration files when in dev mode because
                            // the Java code will also look in the source/webapp directory for such files
                            if (project.labkey.deployMode == LabKey.DeployMode.dev)
                                exclude "WEB-INF/${project.name}/**"
                        }
                        output.resourcesDir = "${project.labkey.explodedModuleDir}/web"
                    }
                }
        project.tasks.processResources.dependsOn('processWebappResources')
    }
}
