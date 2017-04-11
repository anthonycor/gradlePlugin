package org.labkey.gradle.task

import org.apache.commons.lang3.SystemUtils
import org.gradle.api.DefaultTask
import org.gradle.api.file.CopySpec
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class DeployApp extends DefaultTask
{
    @InputDirectory
    File stagingModulesDir = new File((String) project.staging.modulesDir)

    @InputDirectory
    File stagingWebappDir = new File((String) project.staging.webappDir)

    @InputDirectory
    File stagingTomcatJarDir = new File((String) project.staging.tomcatLibDir)

    @InputDirectory
    File stagingPipelineJarDir = new File((String) project.staging.pipelineLibDir)

    @OutputDirectory
    File deployModulesDir = new File((String) project.serverDeploy.modulesDir)

    @OutputDirectory
    File deployWebappDir = new File((String) project.serverDeploy.webappDir)

    @OutputDirectory
    File deployPipelineLibDir = new File((String) project.serverDeploy.pipelineLibDir)


    @TaskAction
    void action()
    {
        deployWebappDir()
        deployModules()
        deployTomcatJars()
        deployPipelineJars()
        deployNlpEngine()
        deployPlatformBinaries()
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
        ant.copy (
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

    private void deployTomcatJars()
    {
        project.copy( { CopySpec copy ->
            copy.from  stagingTomcatJarDir
            copy.into "${project.tomcatDir}/lib"
        })
        project.copy ( { CopySpec copy ->
            copy.from project.project(":server:bootstrap").tasks.jar
            copy.into "${project.tomcatDir}/lib"
        })
    }

    private void deployPipelineJars()
    {
        project.copy( { CopySpec copy ->
            copy.from stagingPipelineJarDir
            copy.into deployPipelineLibDir
        })
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
                    // Use cutdirsmapper to strip off the parent directory name to merge each subdirectory into a single parent
                    ant.cutdirsmapper(dirs: 1)
                    // first grab all the JAR files, which are the same for all platforms
                    fileset(dir: "${project.labkey.externalDir}/windows")
                            {
                                include ( name: "**/*.jar")
                            }
                }
        if (SystemUtils.IS_OS_MAC)
            deployBinariesViaProjectCopy("osx")
        else if (SystemUtils.IS_OS_LINUX)
            deployBinariesViaProjectCopy("linux")
        else if (SystemUtils.IS_OS_WINDOWS)
            deployBinariesViaAntCopy("windows")
    }

    // Use this method to preserve file permissions, since ant.copy does not, but this does not preserve last modified times
    private void deployBinariesViaProjectCopy(String osDirectory)
    {
        project.copy { CopySpec copy ->
            copy.from project.fileTree("${project.labkey.externalDir}/${osDirectory}").files
            copy.into "${project.serverDeploy.binDir}"
        }
    }

    private void deployBinariesViaAntCopy(String osDirectory)
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
