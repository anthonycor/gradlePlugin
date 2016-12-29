package org.labkey.gradle.task

import org.gradle.api.tasks.testing.Test
import org.labkey.gradle.plugin.TomcatExtension
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

        reports { TestTaskReports -> reports
            reports.junitXml.enabled = false
            reports.junitXml.setDestination( new File("${project.buildDir}/${LOG_DIR}"))
            reports.html.enabled = true
            reports.html.setDestination(new File( "${project.buildDir}/${LOG_DIR}"))
        }
        setClasspath (project.sourceSets.uiTest.runtimeClasspath)
        setTestClassesDir (project.sourceSets.uiTest.output.classesDir)

        // listen to standard out and standard error of the test JVM(s)
        onOutput { descriptor, event ->
            logger.lifecycle("[" + descriptor.getName() + "] " + event.message )
        }

        outputs.upToDateWhen( { return false })
    }

    void setJvmArgs()
    {
        List<String> jvmArgsList = ["-Xmx512m",
                                    "-Xdebug",
                                    "-Xrunjdwp:transport=dt_socket,server=y,suspend=${testExt.getTestConfig("debugSuspendSelenium")},address=${testExt.getTestConfig("selenium.debug.port")}",
                                    "-Dfile.encoding=UTF-8"]

        TomcatExtension tomcat = project.extensions.findByType(TomcatExtension.class)

        if (tomcat != null && !tomcat.trustStore.isEmpty() && !tomcat.trustStorePassword.isEmpty())
        {
            jvmArgsList += [tomcat.trustStore, tomcat.trustStorePassword]
        }

        jvmArgs jvmArgsList
    }
}
