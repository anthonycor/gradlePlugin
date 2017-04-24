package org.labkey.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import org.labkey.gradle.plugin.FileModule
import org.labkey.gradle.plugin.Module
import org.labkey.gradle.plugin.JavaModule
/**
 * Removes modules from the deploy and staging directories.  If a value for dbType is provided,
 * it removes those not supporting the given dbType.  If dbType is null, removes all modules from
 * the current set of projects.
 */
class UndeployModules extends DefaultTask
{
    String dbType = null

    @TaskAction
    void action()
    {
        project.rootProject.allprojects.each { Project p ->
            if ((p.plugins.findPlugin(JavaModule.class) != null || p.plugins.findPlugin(Module.class) != null) &&
                    (dbType == null || !FileModule.shouldDoBuild(p) || !JavaModule.isDatabaseSupported(p, dbType)))
            {
                project.logger.info("Undeploying module ${p.path} for dbType ${dbType}")
                JavaModule.undeployModule(p)
            }
            else
            {
                project.logger.info("Module ${p.path} left in deployment for dbType ${dbType}")
            }
        }
    }
}
