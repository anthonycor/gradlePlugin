package org.labkey.gradle.plugin

import org.apache.commons.lang3.SystemUtils
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.labkey.gradle.util.GroupNames
import org.labkey.gradle.util.PropertiesUtils

import java.util.regex.Matcher
/**
 * TODO:
 *  - unpack_dist
 *
 */
class TeamCity extends Tomcat
{
    private static final String TEAMCITY_INFO_FILE = "teamcity-info.xml";
    private static final String TEST_CONFIGS_DIR = "configs/config-test"
    private static final String NLP_CONFIG_FILE = "nlpConfig.xml"
    private static final String PIPELINE_CONFIG_FILE =  "pipelineConfig.xml"

    @Override
    void apply(Project project)
    {
        if (project.file("${project.tomcatDir}/localhost.truststore").exists())
        {
            project.tomcat.trustStore = "-Djavax.net.ssl.trustStore=${project.tomcatDir}/localhost.truststore"
            project.tomcat.trustStorePassword = "-Djavax.net.ssl.trustStorePassword=changeit"
        }
        project.tomcat.catalinaOpts = "-Xdebug -Dproject.root=${project.rootDir} -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=${project.tomcat.debug} "

        addTasks(project)
    }

    private void addTasks(Project project)
    {
        project.task("setTeamcityAgentPassword",
                group: GroupNames.TEST_SERVER,
                description: "Set the password for use in running tests",
                {
                    doLast project.javaexec({
                        main = "org.labkey.test.util.PasswordUtil"
                        classpath {
                            [project.configurations.testCompile, project.project(":server:test").tasks.classes.outputs]
                        }
                        systemProperties["labkey.server"] = project.labkey.server
                        args = ["set", "teamcity@labkey.test", "yekbal1!"]
                    })
                }
        )
        // The ant task was written such that multiple zip files could be incorporated into the one.
        // TODO Verify that a single file is sufficient.
        project.task("copyJavascriptDocs",
            group: GroupNames.TEST_SERVER,
                description: "create client-api docs file for presentation in TeamCity",
                type: Copy,
                {
                    from "${project.labkey.distDir}/client-api/javascript" // TODO this should be a proper dependency on installer a distribution task
                    include *.zip
                    into "${project.labkey.distDir}/TeamCityTabs" {
                        rename "JavascriptAPIDocs.zip"
                    }
                    include *.zip
                }
        )

        project.task("cleanTestLogs",
                group: GroupNames.TEST_SERVER,
                description: "Removes log files from Tomcat and TeamCity",
                {
                    dependsOn project.tasks.cleanLogs, project.tasks.cleanTemp,
                    doLast {
                        project.delete "${project.projectDir}/${TEAMCITY_INFO_FILE}"
                    }
                }
        )


        project.tasks.stopTomcat.dependsOn(project.tasks.debugClasses)
        project.tasks.stopTomcat.doLast (
                {
                    ensureShutdown(project)
                }
        )

        project.task("killChrome",
            group: GroupNames.TEST_SERVER,
            description: "Kill Chrome processes",
                {
                    doLast {
                        if (SystemUtils.IS_OS_WINDOWS)
                        {
                            project.exec({
                                commandLine "taskkill", "/F /IM chromedriver.exe"
                            })
                        }
                        else if (SystemUtils.IS_OS_UNIX)
                        {
                            project.exec( {
                                commandLine "killall", "-q -KILL chromedriver"
                            })
                            project.exec( {
                                commandLine "killall", "-q -KILL chrome"
                            })
                            project.exec( {
                                commandLine "killall", "-q KILL BrowserBlocking"
                            })
                        }
                    }
                }
        )

        project.task("killFirefox",
                group: GroupNames.TEST_SERVER,
                description: "Kill Firefox processes",
                {
                    doLast {
                        if (SystemUtils.IS_OS_WINDOWS)
                        {
                            project.exec({
                                commaneLine "taskkill", "/F /IM firefox.exe"
                            })
                        }
                        else if (SystemUtils.IS_OS_UNIX)
                        {
                            project.exec( {
                                commandLine "killall", "-q firefox"
                            })
                        }
                    }
                }
        )

        project.task("createPipelineConfig",
            group: GroupNames.TEST_SERVER,
            description: "Create pipeline configs for running tests on the test server",
            type: Copy,
                {
                    from project.project(":server").file(TEST_CONFIGS_DIR)
                    include PIPELINE_CONFIG_FILE
                    filter( { String line ->
                        Matcher matcher = PropertiesUtils.PROPERTY_PATTERN.matcher(line);
                        String newLine = line;
                        while (matcher.find())
                        {
                            if (matcher.group(1).equals("SEQUENCEANALYSIS_CODELOCATION") || matcher.gruop(1).equals("SEQUENCEANALYSIS_TOOLS"))
                                newLine = newLine.replace(matcher.group(), getProperty(project, "additional.pipeline.tools"))
                            else if (matcher.group(1).equals("SEQUENCEANALYSIS_EXTERNALDIR"))
                                newLine = newLine.replace(matcher.group(), project.project("externalModules:labModules:SequenceAnalysis").file("pipeline_code/external").getAbsolutePath())
                        }
                        return newLine;

                    })
                    destinationDir = new File("${ServerDeployExtension.getServerDeployDirectory(project)}/config")
                }
        )

        project.task("createNlpConfig",
            group: GroupNames.TEST_SERVER,
            description: "Create NLP engine configs for the test server",
            type: Copy,
                {
                    from project.project(":server").file(TEST_CONFIGS_DIR)
                    include NLP_CONFIG_FILE
                    filter ({String line ->
                        Matcher matcher = PropertiesUtils.PROPERTY_PATTERN.matcher(line);
                        String newLine = line;
                        while (matcher.find())
                        {
                            if (matcher.group(1).equals("enginePath"))
                                newLine = newLine.replace(matcher.group(), new File((String) project.labkey.externalDir, "nlp").getAbsolutePath())
                        }
                        return newLine;
                    }
                    )
                })

        project.tasks.startTomcat.dependsOn(project.tasks.createPipelineConfig)
        project.tasks.startTomcat.dependsOn(project.tasks.createNlpConfig)
    }

    static boolean isOnTeamCity(Project project)
    {
        return project.hasProperty('teamcity')
    }

    static String getProperty(Project project, String name)
    {
        return getProperty(project, name, "")
    }

    static Object getProperty(Project project, String name, Object defaultValue)
    {
        if (isOnTeamCity(project))
            return project.teamcity[name] != null ? project.teamcity[name] : defaultValue
        else
            return defaultValue;
    }

    private void ensureShutdown(Project project)
    {
        if (!getProperty(project, "tomcat.debug").isEmpty())
        {
            project.javaexec({
                main = "org.labkey.test.debug.ThreadDumpAndKill"
                classpath { [project.sourceSets.debug.output.classesDir, project.configurations.debugCompile] }
                args = [project.teamcity['tomcat.debug']]
            })
        }
    }
}
