/*
 * Copyright (c) 2017 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.gradle.task

import org.apache.commons.lang3.SystemUtils
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.labkey.gradle.plugin.extension.TeamCityExtension

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
        if (rPath == null)
            logger.error("Unable to locate R executable. Make sure R_HOME is defined and points at your R install directory")
        if (rLibsUserDir == null)
            logger.error("Unable to install R dependencies. Make sure R_LIBS_USER is defined and points at sampledata/rlabkey")
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
        project.mkdir("${getRLibsUserPath(project)}/logs")
        installRPackage(installScript)
    }

    Boolean isPackageInstalled(String name)
    {
        String exitCode = ""
        ant.exec(executable: rPath,
                dir: project.projectDir,
                input:project.file("check-installed.R"),
                failifexecutionfails: true,
                searchpath: true,
                resultproperty: exitCode )
                {
                    arg(line: "--slave --vanilla --args ${name}")
                    env(key: "R_LIBS_USER", value: rLibsUserDir.getAbsolutePath())
                }
        return exitCode == "0"
    }

    static String getRTermPath()
    {
        String rHome = getRHome()
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
        String rHome = getRHome()
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

    private static String getRHome()
    {
        String rHome = System.getenv("R_HOME")
        return rHome != null ? rHome : ""
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
                input: "${project.projectDir}/${scriptName}",
                output: "${getRLibsUserPath(project)}/logs/${scriptName}.log",
                logError: true
        )
                {
                    arg(line: "--vanilla")
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
