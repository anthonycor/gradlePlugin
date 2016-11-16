package org.labkey.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Task for stopping a tomcat instance
 */
class StopTomcat extends DefaultTask
{
    @TaskAction
    public void exec()
    {
        project.javaexec( {
            main = "org.apache.catalina.startup.Bootstrap"
            classpath  { ["${project.tomcatDir}/bin/bootstrap.jar", "${project.tomcatDir}/bin/tomcat-juli.jar"] }
            systemProperties["user.dir"] = project.tomcatDir
            args = ["stop"]
        })
    }
}
