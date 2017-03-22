package org.labkey.gradle.task

import org.apache.commons.lang3.SystemUtils
import org.gradle.api.file.CopySpec
/**
 * Created by susanh on 3/16/17.
 */
class InstallRLabKey extends InstallRPackage
{
    @Override
    void doInstall()
    {
        File rLibsUserDir= getInstallDir()
        project.copy {
            CopySpec copy ->
                copy.from(project.rootProject.file('remoteapi/r/test'))
                copy.into(rLibsUserDir)
                copy.include("listArchive.zip")
                copy.include("vignette.R")
                copy.include("instwin.r")
        }
        installRPackage("install-rlabkey-dependencies.R")
        if (SystemUtils.IS_OS_WINDOWS)
        {
            project.copy {
                CopySpec copy ->
                    copy.from project.rootProject.file('remoteapi/r/latest')
                    copy.into rLibsUserDir
                    copy.include("Rlabkey*.zip")
            }
            project.ant.exec(dir: rLibsUserDir,  executable: getRTermPath(), input: "${rLibsUserDir}/instwin.r", failifexecutionfails: "false")
                    {
                        arg(line: "--vanilla --quiet")
                    }
        }
        else
        {
            project.copy {
                CopySpec copy ->
                    copy.from project.rootProject.file('remoteapi/r/latest')
                    copy.into(rLibsUserDir)
                    copy.include("Rlabkey*.tar.gz")
                    copy.rename("Rlabkey.*.tar.gz", "Rlabkey.tar.gz")
            }
            installFromArchive("Rlabkey.tar.gz")
        }
    }
}
