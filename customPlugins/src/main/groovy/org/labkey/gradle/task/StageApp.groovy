package org.labkey.gradle.task

import org.gradle.api.DefaultTask
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

        project.copy({
            from project.project(":schemas").tasks.schemasJar
            into stagingServerLibDir
            include "*.jar"
        })

        project.copy({
            from project.project(":server:api").tasks.jar
            into stagingServerLibDir
            include "*.jar"
        })

    }

    private void stageJspLibs()
    {
        project.copy({
            from project.project(":server:internal").tasks.jspJar
            into stagingServerJspLibDir
        })
        project.copy({
            from project.project(":server:api").tasks.jspJar
            into stagingServerJspLibDir
        })
    }
}
