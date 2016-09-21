package org.labkey.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/**
 * Copies various files into a staging directory to prepare for deployment of the application.
 */
class StageApp extends DefaultTask
{
    @InputDirectory
    File externalLibDir = new File("${project.labkey.externalLibDir}/server")

    @InputDirectory
    File internalModuleLibDir = new File((String) project.project(":server:internal").labkey.explodedModuleLibDir)

    @InputDirectory
    File apiModuleLibDir = new File((String) project.project(":server:api").labkey.explodedModuleLibDir)

    @InputDirectory
    File schemasLibDir = new File((String) project.project(':schemas').labkey.explodedModuleLibDir)

    @OutputDirectory
    File stagingServerLibDir = new File((String) project.staging.libDir)

    @OutputDirectory
    File stagingServerJspLibDir = new File((String) project.staging.jspDir)

    @TaskAction
    public void action()
    {
        stageServerLibs()
        stageJspLibs()
    }

    // TODO what does this look like when the libraries are not on disk?
    private void stageServerLibs()
    {

        project.copy({
            from project.project(":remoteapi:java").tasks.jar
            into stagingServerLibDir
            include "*.jar"
        })

//        project.allprojects { Project p ->
//            p.copy {
//                from p.configurations.external
//                into stagingServerLibDir
//            }
//        }
        project.copy({
            from externalLibDir
            into stagingServerLibDir
            include "*.jar"
            exclude "*-sources.jar"
            exclude "*-javadoc.jar"
        }
        )

        project.copy({
            from schemasLibDir
            into stagingServerLibDir
            include "*.jar"
        })

    }

    private void stageJspLibs()
    {
        project.copy({
            from internalModuleLibDir
            into stagingServerJspLibDir
            include "*_jsp*.jar"
        })
        project.copy({
            from apiModuleLibDir
            into stagingServerJspLibDir
            include "*_jsp*.jar"
        })
    }
}
