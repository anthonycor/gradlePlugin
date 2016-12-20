package org.labkey.gradle.task

import org.apache.commons.lang3.SystemUtils
import org.gradle.api.tasks.testing.Test
import org.labkey.gradle.plugin.LabKeyExtension
import org.labkey.gradle.plugin.TeamCity
import org.labkey.gradle.plugin.TestRunnerExtension
import org.labkey.gradle.util.DatabaseProperties

/**
 * Class that sets our test/Runner.class as the junit test suite and configures a bunch of system properties for
 * running these suites of tests.
 */
class RunTestSuite extends Test
{
    DatabaseProperties dbProperties

    RunTestSuite()
    {
        TestRunnerExtension testExt = (TestRunnerExtension) project.getExtensions().getByName("testRunner")
        List<String> jvmArgsList = ["-Xmx512m",
                                    "-Xdebug",
                                    "-Xrunjdwp:transport=dt_socket,server=y,suspend=${project.testRunner.debugSuspendSelenium},address=${testExt.getTestProperty("selenium.debug.port")}",
                                    "-Dfile.encoding=UTF-8"]
        if (!project.tomcat.trustStore.isEmpty() && !project.tomcat.trustStorePassword.isEmpty())
        {
            jvmArgsList += [project.tomcat.trustStore, project.tomcat.trustStorePassword]
        }

        jvmArgs jvmArgsList

        Properties testProperties = testExt.getProperties()
        for (String key : testProperties.keySet())
        {
            systemProperty key, testProperties.get(key)
        }
        if (project.hasProperty('teamcity'))
        {
            systemProperty "teamcity.tests.recentlyFailedTests.file", project.teamcity['tests.recentlyFailedTests.file']
            systemProperty "teamcity.build.changedFiles.file", project.teamcity['build.changedFiles.file']
            systemProperty "testNewAndModified", "${((String) project.teamcity['tests.runRiskGroupTestsFirst']).contains("newAndModified")}"
            systemProperty "testRecentlyFailed", "${((String) project.teamcity['tests.runRiskGroupTestsFirst']).contains("recentlyFailed")}"
            systemProperty "teamcity.buildType.id", project.teamcity['buildType.id']

        }
        if (SystemUtils.IS_OS_WINDOWS)
        {
            if (SystemUtils.OS_ARCH.equals("amd64"))
                systemProperty "webdriver.ie.driver", "${project.projectDir}/bin/windows/amd64/IEDriverServer.exe"
            else if (SystemUtils.OS_ARCH.equals("i386"))
                systemProperty "webdriver.ie.driver", "${project.projectDir}/bin/windows/i386/IEDriverServer.exe"
            systemProperty "webdriver.chrome.driver", "${project.projectDir}/bin/windows/chromedriver.exe"
        }
        else if (SystemUtils.IS_OS_MAC)
        {
            systemProperty "webdriver.chrome.driver", "${project.projectDir}/bin/mac/chromedriver"
        }
        else if (SystemUtils.IS_OS_LINUX)
        {
            if (System.OS_ARCH.equals("amd64"))
                systemProperty "webdriver.chrome.driver", "${project.projectDir}/bin/linux/amd64/chromedriver"
            else if (SystemUtils.OS_ARCH.equals("i386"))
                systemProperty "webdriver.chrome.driver", "${project.projectDir}/bin/linux/i386/chromedriver"
        }

        systemProperty "devMode", LabKeyExtension.isDevMode(project)
        systemProperty "failure.output.dir", "${project.buildDir}/${project.testRunner.logDir}"
        systemProperty "labkey.root", project.rootDir

        systemProperty "user.home", System.getProperty('user.home')
        systemProperty "tomcat.home", project.ext.tomcatDir
        systemProperty "test.credentials.file", "${project.projectDir}/test.credentials.json"
        scanForTestClasses = false
        include "org/labkey/test/Runner.class"

        reports {
            junitXml.enabled = false
            junitXml.setDestination( new File("${project.buildDir}/${project.testRunner.logDir}"))
            html.enabled = true
            html.setDestination(new File( "${project.buildDir}/${project.testRunner.logDir}"))
        }

        // listen to standard out and standard error of the test JVM(s)
        onOutput { descriptor, event ->
            logger.lifecycle("[" + descriptor.getName() + "] " + event.message )
        }
        dependsOn(project.tasks.writeSampleDataFile)
        if (!project.getPlugins().hasPlugin(TeamCity.class))
            dependsOn(project.tasks.packageChromeExtensions)
        dependsOn(project.tasks.ensurePassword)
        if (project.findProject(":tools:Rpackages:install") != null)
            dependsOn(project.project(':tools:Rpackages:install'))
        if (project.getPlugins().hasPlugin(TeamCity.class))
        {
            dependsOn(project.tasks.killChrome)
            dependsOn(project.tasks.ensurePassword)
        }
        if (project.getPlugins().hasPlugin(TeamCity.class))
        {
            doLast( {
                project.copy({
                    from "${project.tomcatDir}/logs"
                    into "${project.buildDir}/logs/${dbProperties.dbTypeAndVersion}"
                })
            })
        }
    }
}
