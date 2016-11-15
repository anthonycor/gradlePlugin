package org.labkey.gradle.task

import org.gradle.api.tasks.JavaExec

/**
 * Task for stopping a tomcat instance
 */
class StopTomcat extends JavaExec
{
    public StopTomcat()
    {
        main = "org.apache.catalina.startup.Bootstrap"
        classpath  { ["${project.tomcatDir}/bin/bootstrap.jar", "${project.tomcatDir}/bin/tomcat-juli.jar"] }
        systemProperties["user.dir"] = project.tomcatDir
        args = ["stop"]
    }
}
