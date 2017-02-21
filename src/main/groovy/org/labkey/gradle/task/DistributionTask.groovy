package org.labkey.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.OutputDirectory

abstract class DistributionTask extends DefaultTask
{
    Boolean includeWindowsInstaller = false
    Boolean includeZipDistribution = false
    Boolean includeTarGZDistribution = false
    String subDirName
    String extraFileIdentifier = ""
    Boolean includeMassSpecBinaries = false
    String versionPrefix = null

    @OutputDirectory
    File dir

    @OutputDirectory
    File installerBuildDir

    DistributionTask()
    {
        dir = project.rootProject.file("dist")

        installerBuildDir = new File("${project.rootDir}/build/installer/${project.name}")
        project.mkdir(project.buildDir)
    }

}
