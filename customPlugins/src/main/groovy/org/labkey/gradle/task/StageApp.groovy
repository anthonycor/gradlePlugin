package org.labkey.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/**
 * Copies varoius files into a staging directory to prepare for deployment of the application.
 */
class StageApp extends DefaultTask
{
    @InputDirectory
    File externalLibDir = new File("${project.labkey.externalLibDir}/server")

    @InputDirectory
    File internalModuleLibDir = new File((String) project.project(":server:internal").labkey.libDir)

    @InputDirectory
    File apiModuleLibDir = new File((String) project.project(":server:api").labkey.libDir)

    @InputDirectory
    File clientApiLibDir = new File("${project.rootProject.buildDir}/client-api/java/jar")

    @InputDirectory
    File schemasLibDir = new File((String) project.project(':schemas').labkey.libDir)

    @OutputDirectory
    File stagingServerLibDir = new File((String) project.labkey.webappLibDir)

    @OutputDirectory
    File stagingServerJspLibDir = new File((String) project.labkey.webappJspDir)

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
            from clientApiLibDir
            into stagingServerLibDir
            include "*.jar"
        })
        project.copy({
            from internalModuleLibDir
            into stagingServerLibDir
            include "*.jar"
            exclude "*_jsp*.jar"
        })
        project.copy({
            from externalLibDir
            into stagingServerLibDir
            include "*.jar"
        }
        )
        project.copy({
            from apiModuleLibDir
            into stagingServerLibDir
            include "*.jar"
            exclude "*_jsp*.jar"
        })
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
