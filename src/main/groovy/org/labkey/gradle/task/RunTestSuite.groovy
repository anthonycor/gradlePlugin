package org.labkey.gradle.task

import org.gradle.api.file.CopySpec
import org.labkey.gradle.plugin.TeamCity
import org.labkey.gradle.plugin.TeamCityExtension
import org.labkey.gradle.plugin.UiTestExtension
import org.labkey.gradle.util.DatabaseProperties
/**
 * Class that sets our test/Runner.class as the junit test suite and configures a bunch of system properties for
 * running these suites of tests.
 */
class RunTestSuite extends RunUiTest
{
    DatabaseProperties dbProperties
    TeamCityExtension tcExtension = null

    RunTestSuite()
    {
        super()
        if (TeamCityExtension.isOnTeamCity(project))
            tcExtension  = (UiTestExtension) project.getExtensions().getByType(TeamCityExtension.class)
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

            doLast( {
                project.copy({ CopySpec copy ->
                    copy.from "${project.tomcatDir}/logs"
                    copy.into "${project.buildDir}/logs/${dbProperties.dbTypeAndVersion}"
                })
            })
        }
    }

    protected String getDebugPort()
    {
        return tcExtension == null ? super.getDebugPort() : tcExtension.getTeamCityProperty("tomcat.debug")
    }

    private void setSystemProperties()
    {
        super.setSystemProperties()
        if (TeamCityExtension.isOnTeamCity(project))
        {
            systemProperty "teamcity.tests.recentlyFailedTests.file", tcExtension.getTeamCityProperty('teamcity.tests.recentlyFailedTests.file')
            systemProperty "teamcity.build.changedFiles.file", tcExtension.getTeamCityProperty('teamcity.build.changedFiles.file')
            String runRiskGroupTestsFirst = tcExtension.getTeamCityProperty('tests.runRiskGroupTestsFirst')
            if (runRiskGroupTestsFirst != null)
            {
                systemProperty "testNewAndModified", "${runRiskGroupTestsFirst.contains("newAndModified")}"
                systemProperty "testRecentlyFailed", "${runRiskGroupTestsFirst.contains("recentlyFailed")}"
            }
            systemProperty "teamcity.buildType.id", tcExtension.getTeamCityProperty('teamcity.buildType.id')
            systemProperty "tomcat.home", tcExtension.getTeamCityProperty("tomcat.home")
            systemProperty "tomcat.port", tcExtension.getTeamCityProperty("tomcat.port")
            systemProperty "tomcat.debug", tcExtension.getTeamCityProperty("tomcat.debug")
        }
    }
}
