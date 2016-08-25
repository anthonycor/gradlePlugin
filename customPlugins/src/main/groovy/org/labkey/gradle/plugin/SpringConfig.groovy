package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Used for copying the Spring config files to the module's build directory.
 */
class SpringConfig implements Plugin<Project>
{
    private static final DIR_PREFIX = "webapp/WEB-INF"
    def Project _project;
    def String _dirName;

    public static boolean isApplicable(Project project)
    {
        return project.file("${DIR_PREFIX}/${project.name}").exists()
    }

    @Override
    void apply(Project project)
    {
        _project = project;
        _dirName = "${DIR_PREFIX}/${_project.name}"
        project.apply plugin: 'java-base'

        addSourceSet(project)
    }

    private void addSourceSet(Project project)
    {
        _project.sourceSets
                {
                    spring {
                        resources {
                            srcDirs = [_dirName]
                        }
                        output.resourcesDir = project.labkey.explodedModuleConfigDir
                    }
                }
        _project.tasks.processResources.dependsOn('processSpringResources')
    }
}