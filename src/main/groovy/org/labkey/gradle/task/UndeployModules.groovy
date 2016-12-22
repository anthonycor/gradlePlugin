package org.labkey.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/**
 * Moves .modules files from the deploy directory into the staging directory
 */
class UndeployModules extends DefaultTask
{
    @OutputDirectory
    File stagingModulesDir = new File((String) project.staging.modulesDir)

    @InputDirectory
    File deployModulesDir = new File((String) project.serverDeploy.modulesDir)


    @TaskAction
    void action()
    {
        undeployModules()
    }


    private void undeployModules()
    {
        ant.move(
                todir: stagingModulesDir,
                preserveLastModified: true,
        )
                {
                    fileset(dir: deployModulesDir)
                            {
                                include( name: "*.module")
                            }
                }
    }
}
