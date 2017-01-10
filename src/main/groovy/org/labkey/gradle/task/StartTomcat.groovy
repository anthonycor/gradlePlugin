package org.labkey.gradle.task

import org.apache.commons.lang3.SystemUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.labkey.gradle.plugin.TeamCityExtension
/**
 * Created by susanh on 11/15/16.
 */
class StartTomcat extends DefaultTask
{
    @TaskAction
    void action()
    {
        // we need to create the logs directory if it doesn't exist because Tomcat won't start without it,
        // and, annoyingly, this is not seen as an error for this action.
        if (!project.file("${project.tomcatDir}/logs").exists())
            project.mkdir("${project.tomcatDir}/logs")
        if (SystemUtils.IS_OS_UNIX)
        {
            project.ant.chmod(dir: "${project.tomcatDir}/bin", includes: "**/*.sh", perm: "ug+rx")
        }
        project.ant.exec(
                spawn: true,
                dir: SystemUtils.IS_OS_WINDOWS ? "${project.tomcatDir}/bin" : project.tomcatDir,
                executable: SystemUtils.IS_OS_WINDOWS ? "cmd" : "bin/catalina.sh"
        )
        {
            if (TeamCityExtension.isOnTeamCity(project))
            {
                if (SystemUtils.IS_OS_WINDOWS)
                {
                    env(
                        key: "PATH",
                        path: "${project.rootDir}/external/windows/core${File.pathSeparator}${System.getenv("PATH")}"
                    )
                }
            }
            else
            {
                env(
                        key: "PATH",
                        path: "${project.project(":server").serverDeploy.binDir}${File.pathSeparator}${System.getenv("PATH")}"
                )
            }

            String catalinaOpts = "${project.tomcat.assertionFlag} -Ddevmode=${project.tomcat.devMode} ${project.tomcat.catalinaOpts} " +
                        "-Xmx${TeamCityExtension.getTeamCityProperty(project, "Xmx", project.tomcat.maxMemory)} " +
                        "${project.tomcat.recompileJsp ? "" : "-Dlabkey.disableRecompileJsp=true"} " +
                        "${project.tomcat.trustStore} ${project.tomcat.trustStorePassword} "

            if (TeamCityExtension.isOnTeamCity(project) && SystemUtils.IS_OS_UNIX)
            {
                catalinaOpts += "-DsequencePipelineEnabled=${TeamCityExtension.getTeamCityProperty(project, "sequencePiplineEnabled", false)}"
            }

            env(
                    key: "CATALINA_OPTS",
                    value: catalinaOpts
            )
            if (TeamCityExtension.isOnTeamCity(project))
            {
                env(
                        key: "R_LIBS_USER",
                        value: System.getenv("R_LIBS_USER") != null ? System.getenv("R_LIBS_USER") : project.rootProject.file("sampledata/rlabkey")
                )
                env (
                        key: "JAVA_HOME",
                        value: System.getenv("JAVA_HOME")
                )
                env (
                        key: "JRE_HOME",
                        value: System.getenv("JAVA_HOME")
                )
            }

            if (SystemUtils.IS_OS_WINDOWS)
            {
                env(
                        key: "CLOSE_WINDOW",
                        value: true
                )
                arg(line: "/c start")
                arg(value: "&quot;Tomcat Server&quot;")
                arg(value: "/B")
                arg(value: "${project.tomcatDir}/bin/catalina.bat")
            }
            else
            {
                arg(value: "start")
            }

        }
        println("System properties: ${System.properties}")
        if (project.hasProperty("env"))
            println("project.env: ${project.env}")
        println("Waiting 5 seconds for tomcat to start...")
        project.ant.sleep(seconds: 5)
        println("Tomcat started.")
    }
}
