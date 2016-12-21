package org.labkey.gradle.task

import org.gradle.api.tasks.testing.Test
import org.labkey.gradle.plugin.UiTestExtension

/**
 * Class that sets up jvmArgs and our standard output options
 */
class RunUiTest extends Test
{
    public static final String LOG_DIR = "test/logs"
    protected UiTestExtension testExt

    RunUiTest()
    {
        testExt = (UiTestExtension) project.getExtensions().getByType(UiTestExtension.class)
        setJvmArgs()

        reports {
            junitXml.enabled = false
            junitXml.setDestination( new File("${project.buildDir}/${LOG_DIR}"))
            html.enabled = true
            html.setDestination(new File( "${project.buildDir}/${LOG_DIR}"))
        }

        // listen to standard out and standard error of the test JVM(s)
        onOutput { descriptor, event ->
            logger.lifecycle("[" + descriptor.getName() + "] " + event.message )
        }
    }

    void setJvmArgs()
    {
        List<String> jvmArgsList = ["-Xmx512m",
                                    "-Xdebug",
                                    "-Xrunjdwp:transport=dt_socket,server=y,suspend=${testExt.getTestProperty("debugSuspendSelenium")},address=${testExt.getTestProperty("selenium.debug.port")}",
                                    "-Dfile.encoding=UTF-8"]
        if (!project.tomcat.trustStore.isEmpty() && !project.tomcat.trustStorePassword.isEmpty())
        {
            jvmArgsList += [project.tomcat.trustStore, project.tomcat.trustStorePassword]
        }

        jvmArgs jvmArgsList
    }
}
