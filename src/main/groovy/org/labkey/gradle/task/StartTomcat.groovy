package org.labkey.gradle.task

import org.apache.commons.lang3.SystemUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.labkey.gradle.plugin.TeamCity

/**
 * Created by susanh on 11/15/16.
 */
class StartTomcat extends DefaultTask
{
    @TaskAction
    public void action()
    {
        if (SystemUtils.IS_OS_UNIX)
        {
            project.ant.chmod(dir: "${project.tomcatDir}/bin", includes: "**/*.sh", perm: "ug+rx")
        }
        ant.exec(
                spawn: true,
                dir: SystemUtils.IS_OS_WINDOWS ? "${project.tomcatDir}/bin" : project.tomcatDir,
                executable: SystemUtils.IS_OS_WINDOWS ? "cmd" : "bin/catalina.sh"
        )
        {

            if (TeamCity.isTeamCity(project))
            {
                if (SystemUtils.IS_OS_WINDOWS)
                {
                    env(
                            key: "PATH",
                            path: "${project.rootDir}/external/windows/core${File.pathSeparator}${System.getProperty("PATH")}"
                    )
                }
            }
            else
            {
                env(
                        key: "PATH",
                        path: "${project.project(":server").serverDeploy.binDir}${File.pathSeparator}${System.getProperty("PATH")}"
                )
            }
            // TODO incorporate teamcity.build.id if necessary (where/how is it set in ant???);
            // TODO incorporate sequencePipelineEnabled for Unix
            env(
                    key: "CATALINA_OPTS",
                    value: "${project.tomcat.assertionFlag} -Ddevmode=${project.tomcat.devMode} ${project.tomcat.catalinaOpts} -Xmx${project.tomcat.maxMemory} ${project.tomcat.recompileJsp ? "" : "-Dlabkey.disableRecompileJsp=true"} ${project.tomcat.trustStore} ${project.tomcat.trustStorePassword}"
            )
            if (TeamCity.isTeamCity(project))
            {
                env(
                        key: "R_LIBS_USER",
                        value: System.getProperty("R_LIBS_USER") != null ? System.getProperty("R_LIBS_USER") : project.rootProject.file("sampledata/rlabkey")
                )
                env (
                        key: "JAVA_HOME",
                        value: System.getProperty("JAVA_HOME")
                )
                env (
                        key: "JRE_HOME",
                        value: System.getProperty("JAVA_HOME")
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
        println("Waiting 5 seconds for tomcat to start...")
        project.ant.sleep(seconds: 5)
        println("Tomcat started.")
    }
}
