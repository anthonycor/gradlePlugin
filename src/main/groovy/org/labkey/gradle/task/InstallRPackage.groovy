package org.labkey.gradle.task

import org.apache.commons.lang3.SystemUtils
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecSpec
import org.labkey.gradle.plugin.TeamCityExtension

/**
 * Created by susanh on 3/20/17.
 */
class InstallRPackage extends DefaultTask
{
    List<String> packageNames
    String installScript

    String rPath
    File rLibsUserDir

    InstallRPackage()
    {
        rPath = getRPath()
        rLibsUserDir = getInstallDir()
        onlyIf {
            if (rPath == null)
                return false
            if (packageNames != null)
            {
                for (String name : packageNames)
                    if (!isPackageInstalled(name))
                        return true
                return false
            }
            return true
        }
    }

    @OutputDirectory
    File getInstallDir()
    {
        String path = getRLibsUserPath(project)
        return path == null ? null : new File(path)
    }

    @TaskAction
    void doInstall()
    {
        installRPackage(installScript)
    }

    Boolean isPackageInstalled(String name)
    {
        ByteArrayOutputStream stream = new ByteArrayOutputStream()
        project.exec{
            ExecSpec spec -> spec.executable rPath
                spec.workingDir project.projectDir
                spec.standardInput = new FileInputStream(project.file("check-installed.R"))
                spec.ignoreExitValue = true
                spec.args = ["--slave", "--vanilla", "--args ${name}"]
                spec.environment("R_LIBS_USER", getRLibsUserPath(project))
                spec.standardOutput = stream
        }
        String output = stream.toString()
        return output.contains("library ${name} is installed")
    }

    static String getRTermPath()
    {
        String rHome = System.getenv("R_HOME")
        if (SystemUtils.IS_OS_WINDOWS)
        {
            File file = new File(rHome, "bin/Rterm.exe")
            if (file.exists())
                return file.getAbsolutePath()
            file = new File(rHome, "bin/i386/Rterm.exe")
            if (file.exists())
                return file.getAbsolutePath()
            file = new File(rHome, "bin/x64/Rterm.exe")
            if (file.exists())
                return file.getAbsolutePath()
            return "Rterm.exe"
        }
        return null
    }

    static String getRPath()
    {
        String rHome = System.getenv("R_HOME")
        if (SystemUtils.IS_OS_WINDOWS)
        {
            File file = new File(rHome, "bin/R.exe")
            if (file.exists())
                return file.getAbsolutePath()
            file = new File(rHome, "R.exe")
            if (file.exists())
                return file.getAbsolutePath()
        }
        else
        {
            File file = new File(rHome, "bin/R")
            if (file.exists())
                return file.getAbsolutePath()
            file = new File(rHome, "R")
            if (file.exists())
                return file.getAbsolutePath()
        }
        return null
    }

    static String getRLibsUserPath(Project project)
    {
        return TeamCityExtension.getTeamCityProperty(project, "R_LIBS_USER", System.getenv("R_LIBS_USER"))
    }

    void installRPackage(String scriptName)
    {
        ant.exec(
                executable: rPath,
                dir: project.projectDir,
                failifexecutionfails: false,
                searchpath: true,
                input: "${project.projectDir}/${scriptName}"
        )
                {
                    arg(line: "--vanilla --quiet")
                    env(key: "R_LIBS_USER", value: getRLibsUserPath(project)) // TODO is this actually necessary?
                }
    }

    void installFromArchive(String archiveFileName)
    {

        project.ant.exec(dir: getRLibsUserPath(project), executable: rPath, failifexecutionfails: "false", searchpath: true)
                {
                    arg(line: "CMD INSTALL -l ${rLibsUserDir} ${rLibsUserDir}/${archiveFileName}")
                }
    }
}
