package org.labkey.gradle.task

import org.apache.commons.lang3.SystemUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

import java.nio.file.CopyOption
import java.nio.file.StandardCopyOption

class DeployApp extends DefaultTask
{
    @InputDirectory
    File stagingModulesDir = new File((String) project.labkey.stagingModulesDir)

    @InputDirectory
    File externalLibDir = new File("${project.labkey.externalLibDir}/server")

    @InputDirectory
    File stagingWebappDir = new File((String) project.labkey.stagingWebappDir)

    @InputDirectory
    File internalModuleLibDir = new File((String) project.project(":server:internal").labkey.libDir)

    @InputDirectory
    File apiModuleLibDir = new File((String) project.project(":server:api").labkey.libDir)

    @InputDirectory
    File clientApiLibDir = new File("${project.rootProject.buildDir}/client-api/java/jar")

    @InputDirectory
    File schemasLibDir = new File((String) project.project(':schemas').labkey.libDir)

    @OutputDirectory
    File deployModulesDir = new File((String) project.serverDeploy.modulesDir)

    @OutputDirectory
    File deployWebappDir = new File((String) project.serverDeploy.webappDir)

    @OutputDirectory
    File stagingServerLibDir = new File((String) project.labkey.webappLibDir)

    @OutputDirectory
    File stagingServerJspLibDir = new File((String) project.labkey.webappJspDir)

    CopyOption[] options = [StandardCopyOption.COPY_ATTRIBUTES,
                            StandardCopyOption.ATOMIC_MOVE,
                            StandardCopyOption.REPLACE_EXISTING]

    @TaskAction
    public void action()
    {
        stageServerLibs()
        stageJspLibs()
        deployWebappDir()
        deployModules()
        deployNlpEngine()
        deployPlatformBinaries()
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

    private void deployWebappDir()
    {
        ant.copy(
                todir: deployWebappDir,
                preserveLastModified: true
        )
                {
                    fileset(dir: stagingWebappDir)
                }

        ant.copy(
                todir: deployWebappDir,
                preserveLastModified: true,
                overwrite: true,
        )
                {
                    fileset(dir: stagingWebappDir)
                            {
                                include( name: "WEB-INF/classes/log4j.xml")
                            }

                }
    }

    private void deployModules()
    {
        ant.move(
                todir: deployModulesDir,
                preserveLastModified: true,
        )
                {
                    fileset(dir: stagingModulesDir)
                            {
                                include( name: "*")
                            }
                }
    }

    private void deployPlatformBinaries()
    {
        File deployBinDir = new File((String) project.serverDeploy.binDir)
        deployBinDir.mkdirs()

        ant.copy(
                todir: project.serverDeploy.binDir,
                preserveLastModified: true
        )
                {
                    // Use cutdirsmapper to strop off the parent directory name to merge each subdirectory into a single parent
                    ant.cutdirsmapper(dirs: 1)
                    // first grab all the JAR files, which are the same for all platforms
                    fileset(dir: "${project.labkey.externalDir}/windows")
                            {
                                include ( name: "**/*.jar")
                            }
                }
        if (SystemUtils.IS_OS_MAC)
            deployBinariesViaCp("osx")
        else if (SystemUtils.IS_OS_LINUX)
            deployBinariesViaCp("linux")
        else if (SystemUtils.IS_OS_WINDOWS)
            deployBinariesViaAndCopy("windows")
    }

    private void deployBinariesViaCp(String osDirectory)
    {
        "cp -Rn ${project.labkey.externalDir}/${osDirectory}/* ${project.serverDeploy.binDir}".execute()
    }

    private void deployBinariesViaAndCopy(String osDirectory)
    {
        ant.copy(
                todir: project.serverDeploy.binDir,
                preserveLastModified: true
        )
                {
                    ant.cutdirsmapper(dirs: 1)
                    fileset(dir: "${project.labkey.externalDir}/${osDirectory}")
                            {
                                exclude(name: "**.*")
                            }
                }

    }

    private void deployNlpEngine()
    {

        File nlpSource = new File((String) project.labkey.externalDir, "nlp")
        if (nlpSource.exists())
        {
            File nlpDir = new File((String) project.serverDeploy.binDir, "nlp")
            nlpDir.mkdirs();
            ant.copy(
                    toDir: nlpDir,
                    preserveLastModified: true
            )
                    {
                        fileset(dir: nlpSource)
                                {
                                    exclude(name: "**/*.py?")
                                }
                    }
        }
    }
}
