package org.labkey.gradle.task

import org.apache.commons.lang3.SystemUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

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

            env(
                    key: "PATH",
                    path: "${project.project(":server").serverDeploy.binDir}${File.pathSeparator}${System.getProperty("PATH")}"
            )
            env(
                    key: "CATALINA_OPTS",
                    value: "-ea -Ddevmode=true -Xmx1G"
            )

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
