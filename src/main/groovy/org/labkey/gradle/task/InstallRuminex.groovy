package org.labkey.gradle.task

import org.gradle.api.file.CopySpec
import org.gradle.api.tasks.TaskAction
/**
 * Created by susanh on 3/16/17.
 */
class InstallRuminex extends InstallRPackage
{
    @TaskAction
    void copyFiles()
    {
        File rLibsUserDir= getInstallDir()
        installRPackage("install-ruminex-dependencies.R")
        project.copy {
            CopySpec copy ->
                copy.from project.projectDir
                copy.into(rLibsUserDir)
                copy.include("Ruminex*.tar.gz")
                copy.rename("Ruminex*.tar.gz", "Ruminex.tar.gz")
        }
        installFromArchive("Ruminex.tar.gz")
    }


}
