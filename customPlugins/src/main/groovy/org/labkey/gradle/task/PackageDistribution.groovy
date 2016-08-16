package org.labkey.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class PackageDistribution extends DefaultTask
{
    public static final String ALL_DISTRIBUTIONS = "all"

    @TaskAction
    public void action()
    {
        setUpModuleDistDirectories()
        if ("modules".equalsIgnoreCase(project.dist.type))
        {
            gatherModules()
            packageRedistributables()
        }
        else if ("source".equalsIgnoreCase(project.dist.type))
        {
            packageSource()
        }
        else if ("pipelineConfigs".equalsIgnoreCase(project.dist.type))
        {
            packagePipelineConfigs()
        }
        else if ("client-apis".equalsIgnoreCase(project.dist.type))
        {
            packageClientApis()
        }
    }

    private void gatherModules()
    {
        ant.copy (
                toDir: project.dist.distModulesDir
        )
                {
                    project.configurations.distribution.each {
                        File moduleFile ->
                            file(name: moduleFile.getPath())
                    }
                }
    }

    private void setAntPropertiesForInstaller()
    {
        // TODO should we log errors if the subDirName or extraFileIdentifier is missing?
        if (project.dist.subDirName != null)
            ant.properties['dist_sub.dir'] = project.dist.subDirName
        if (project.dist.extraFileIdentifier != null)
            ant.properties['extraFileIdentifier'] = project.dist.extraFileIdentifier
        ant.properties['project.root'] = project.rootDir
        if (project.dist.skipWindowsInstaller != null)
            ant.properties['skip.windowsInstaller'] = project.dist.skipWindowsInstaller
        if (project.dist.skipZipDistribution != null)
            ant.properties['skip.zipDistribution'] = project.dist.skipZipDistribution
        if (project.dist.skipTarGZDistribution != null)
            ant.properties['skip.tarGZDistribution'] = project.dist.skipTarGZDistribution
        if (project.dist.versionPrefix != null)
            ant.properties['versionPrefix'] = project.dist.versionPrefix
    }

    private void packageRedistributables()
    {
        setAntPropertiesForInstaller()
        ant.ant(dir: "${project.project(':server').projectDir}/installer", antFile: "${project.project(':server').projectDir}/installer/build.xml", target: "build")
    }

    private void packageSource()
    {
        setAntPropertiesForInstaller()
        ant.ant(dir: "${project.project(':server').projectDir}/installer", antFile: "${project.project(':server').projectDir}/installer/build.xml", target: "archive_source")
    }

    private void packagePipelineConfigs()
    {
        setAntPropertiesForInstaller()
        ant.ant(dir: "${project.project(':server').projectDir}/installer", antFile: "${project.project(':server').projectDir}/installer/build.xml", target: "pipeline-configs")
    }

    private void packageClientApis()
    {
        setAntPropertiesForInstaller()
        ant.ant(dir: "${project.project(':server').projectDir}/installer", antFile: "${project.project(':server').projectDir}/installer/build.xml", target: "client-apis")
    }

    // TODO this is called only by the teamcity targets
    private void unpackDist()
    {
        setAntPropertiesForInstaller()
        ant.ant(dir: "${project.project(':server').projectDir}/installer", antFile: "${project.project(':server').projectDir}/installer/build.xml", target: "bin-untar")
    }


    private void setUpModuleDistDirectories()
    {
        File distDir = new File((String) project.dist.distModulesDir)
        distDir.deleteDir();
        distDir.mkdirs();
    }
}
