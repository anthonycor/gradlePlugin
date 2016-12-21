package org.labkey.gradle.task

import org.apache.commons.lang3.SystemUtils
import org.gradle.api.file.CopySpec
import org.labkey.gradle.plugin.LabKeyExtension
import org.labkey.gradle.plugin.TeamCity
import org.labkey.gradle.util.DatabaseProperties
/**
 * Class that sets our test/Runner.class as the junit test suite and configures a bunch of system properties for
 * running these suites of tests.
 */
class RunTestSuite extends RunUiTest
{
    DatabaseProperties dbProperties

    RunTestSuite()
    {
        super()
        setSystemProperties()

        scanForTestClasses = false
        include "org/labkey/test/Runner.class"

        dependsOn(project.tasks.writeSampleDataFile)

        dependsOn(project.tasks.ensurePassword)
        if (project.findProject(":tools:Rpackages:install") != null)
            dependsOn(project.project(':tools:Rpackages:install'))
        if (!project.getPlugins().hasPlugin(TeamCity.class))
            dependsOn(project.tasks.packageChromeExtensions)
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
                project.copy({ CopySpec copy ->
                    copy.from "${project.tomcatDir}/logs"
                    copy.into "${project.buildDir}/logs/${dbProperties.dbTypeAndVersion}"
                })
            })
        }
    }

    private void setSystemProperties()
    {
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
        systemProperty "failure.output.dir", "${project.buildDir}/${LOG_DIR}"
        systemProperty "labkey.root", project.rootDir

        systemProperty "user.home", System.getProperty('user.home')
        systemProperty "tomcat.home", project.ext.tomcatDir
        systemProperty "test.credentials.file", "${project.projectDir}/test.credentials.json"
    }
}
