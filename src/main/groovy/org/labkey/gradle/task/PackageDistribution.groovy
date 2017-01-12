package org.labkey.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.labkey.gradle.plugin.DistributionExtension

import java.nio.file.Files
import java.nio.file.Paths

class PackageDistribution extends DefaultTask
{
    public static final String ALL_DISTRIBUTIONS = "all"

    public static final String[] STANDARD_MODULES = [
            ':server:modules:announcements',
            ':server:modules:audit',
            ':server:modules:bigiron',
            ':server:modules:core',
            ':server:modules:dataintegration',
            ':server:modules:elisa',
            ':server:modules:elispotassay',
            ':server:modules:experiment',
            ':server:customModules:fcsexpress',
            ':server:modules:filecontent',
            ':server:modules:flow',
            ':server:modules:issues',
            ':server:modules:list',
            ':server:modules:luminex',
            ':server:modules:microarray',
            ':server:modules:ms1',
            ':server:modules:ms2',
            ':server:modules:nab',
            ':server:modules:pipeline',
            ':server:modules:query',
            ':server:modules:search',
            ':server:modules:study',
            ':server:modules:survey',
            ':server:customModules:targetedms',
            ':server:modules:visualization',
            ':server:modules:wiki'
    ]

    @TaskAction
    void action()
    {
        setUpModuleDistDirectories()

        // TODO enum would be better for these types
        if ("modules".equalsIgnoreCase(project.dist.type))
        {
            writeDistributionFile()
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
        else if ("clientApis".equalsIgnoreCase(project.dist.type))
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
        if (project.dist.includeMassSpecBinaries != null)
            ant.properties['includeMassSpecBinaries'] = project.dist.includeMassSpecBinaries
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

    private void setUpModuleDistDirectories()
    {
        File distDir = new File((String) project.dist.distModulesDir)
        distDir.deleteDir();
        distDir.mkdirs();
    }

    private void writeDistributionFile()
    {
        File distExtraDir = new File(project.rootProject.buildDir, DistributionExtension.DIST_FILE_DIR);
        if (!distExtraDir.exists())
            distExtraDir.mkdirs()
        Files.write(Paths.get(distExtraDir.absolutePath, DistributionExtension.DIST_FILE_NAME), project.name.getBytes())
    }
}
