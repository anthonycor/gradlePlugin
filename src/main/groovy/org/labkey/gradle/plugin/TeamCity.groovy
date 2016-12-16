package org.labkey.gradle.plugin

import org.apache.commons.lang3.SystemUtils
import org.apache.commons.lang3.tuple.ImmutablePair
import org.apache.commons.lang3.tuple.Pair
import org.gradle.api.GradleException
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
    private static final Map<String, Pair<String, String>> SUPPORTED_DATABASES = new HashMap<>();
    static
    {
        SUPPORTED_DATABASES.put("postgres9.2", new ImmutablePair<>("pg", "9.2"))
        SUPPORTED_DATABASES.put("postgres9.3", new ImmutablePair<>("pg", "9.3"))
        SUPPORTED_DATABASES.put("postgres9.4", new ImmutablePair<>("pg", "9.4"))
        SUPPORTED_DATABASES.put("postgres9.5", new ImmutablePair<>("pg", "9.5"))
        SUPPORTED_DATABASES.put("postgres9.6", new ImmutablePair<>("pg", "9.6"))
        SUPPORTED_DATABASES.put("sqlserver2012", new ImmutablePair<>("mssql", "2012"))
        SUPPORTED_DATABASES.put("sqlserver2014", new ImmutablePair<>("mssql", "2014"))
        SUPPORTED_DATABASES.put("sqlserver2016", new ImmutablePair<>("mssql", "2016"))

    }

    @Override
    void apply(Project project)
    {
        super.apply(project)
        project.extensions.create("teamCity", TeamCityExtension)
        if (project.file("${project.tomcatDir}/localhost.truststore").exists())
        {
            project.tomcat.trustStore = "-Djavax.net.ssl.trustStore=${project.tomcatDir}/localhost.truststore"
            project.tomcat.trustStorePassword = "-Djavax.net.ssl.trustStorePassword=changeit"
        }
        project.tomcat.catalinaOpts = "-Xdebug -Dproject.root=${project.rootDir} -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=${getProperty(project, "tomcat.debug")} "

        addTasks(project)
    }

    private void addTasks(Project project)
    {
        project.task("setTeamCityAgentPassword",
                group: GroupNames.TEST_SERVER,
                description: "Set the password for use in running tests",
                {
                    dependsOn(project.tasks.testJar)
                    doLast {
                        project.javaexec({
                            main = "org.labkey.test.util.PasswordUtil"
                            classpath {
                                [project.configurations.testCompile, project.tasks.testJar ]
                            }
                            systemProperties["labkey.server"] = project.labkey.server
                            args = ["set", "teamcity@labkey.test", "yekbal1!"]
                        })
                    }
                }
        )

        // The ant task was written such that multiple zip files could be incorporated into the one.
        // TODO Verify that a single file is sufficient.
//        project.task("copyJavascriptDocs",
//            group: GroupNames.TEST_SERVER,
//                description: "create client-api docs file for presentation in TeamCity",
//                type: Copy,
//                {
//                    from "${project.dist.dir}/client-api/javascript" // TODO this should be a proper dependency on a distribution task
//                    include *.zip
//                    into "${project.dist.dir}/TeamCityTabs" {
//                        rename "JavascriptAPIDocs.zip"
//                    }
//                    include *.zip
//                }
//        )

        project.task("cleanTestLogs",
                group: GroupNames.TEST_SERVER,
                description: "Removes log files from Tomcat and TeamCity",
                {
                    dependsOn project.project(":server").tasks.cleanLogs, project.project(":server").tasks.cleanTemp,
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

        if (project.findProject(":externalModules:labModules:SequenceAnalysis") != null)
        {
            project.task("createPipelineConfig",
                    group: GroupNames.TEST_SERVER,
                    description: "Create pipeline configs for running tests on the test server",
                    type: Copy,
                    {
                        from project.project(":server").file(TEST_CONFIGS_DIR)
                        include PIPELINE_CONFIG_FILE
                        filter({ String line ->
                            Matcher matcher = PropertiesUtils.PROPERTY_PATTERN.matcher(line);
                            String newLine = line;
                            while (matcher.find())
                            {
                                if (matcher.group(1).equals("SEQUENCEANALYSIS_CODELOCATION") || matcher.group(1).equals("SEQUENCEANALYSIS_TOOLS"))
                                    newLine = newLine.replace(matcher.group(), getProperty(project, "additional.pipeline.tools"))
                                else if (matcher.group(1).equals("SEQUENCEANALYSIS_EXTERNALDIR"))
                                    newLine = newLine.replace(matcher.group(), project.project(":externalModules:labModules:SequenceAnalysis").file("pipeline_code/external").getAbsolutePath())
                            }
                            return newLine;

                        })
                        destinationDir = new File("${ServerDeployExtension.getServerDeployDirectory(project)}/config")
                    }
            )
        }

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
                                newLine = newLine.replace(matcher.group(), new File((String) project.labkey.externalDir, "nlp/nlp_engine.py").getAbsolutePath())
                        }
                        return newLine;
                    }
                    )
                    destinationDir = new File("${ServerDeployExtension.getServerDeployDirectory(project)}/config")
                })

        project.tasks.startTomcat.doFirst(
                {
                    String database = getProperty(project, "database")
                    if (database.isEmpty())
                        throw new GradleException("${project.path} No database type provided")
                    else if (!SUPPORTED_DATABASES.containsKey(database))
                        throw new GradleException("${project.path} Database ${database} not supported")
                    project.rootProject.allprojects.each {Project p ->
                        if (!SimpleModule.shouldDoBuild(project) || !SimpleModule.isDatabaseSupported(p, database))
                        {
                            SimpleModule.undeployModule(project)
                        }
                    }
                }
        )
        project.tasks.startTomcat.dependsOn(project.tasks.createPipelineConfig)
        project.tasks.startTomcat.dependsOn(project.tasks.createNlpConfig)

        project.task("ciTest",
            group: GroupNames.TEST_SERVER,
            description: "Run a test suite on the TeamCity server",
                {
                    doFirst
                            {
                                List<String> messages = getValidationMessages(project)
                                if (!messages.isEmpty())
                                    throw new GradleException("TeamCity configuration problem(s): ${messages.join('; ')}")

                                project.logger.info("teamcity.build.branch.is_default: ${getProperty(project, 'teamcity.build.branch.is_default')}")
                                project.logger.info("teamcity.build.branch: ${getProperty(project, 'teamcity.build.branch')}")
                                project.teamCity.databaseName = getDatabaseName(project)
                                println("Database name is ${getDatabaseName(project)}")
                            }
                })
    }

    private static List<String> getValidationMessages(Project project)
    {
        List<String> messages = new ArrayList<>()
        if (getProperty(project, "suite").isEmpty())
            messages.add("'suite' property not specified")

        if (getProperty(project, "tomcat.home").isEmpty())
            messages.add("'tomcat.home' property not specified")
        if (getProperty(project, "tomcat.port").isEmpty())
            messages.add("'tomcat.port' property not specified")
        String databaseTypes = getProperty(project, "database.types")
        if (databaseTypes.isEmpty())
            messages.add("'database.types' property not specified")
        else
        {
            Boolean databaseFound = false;
            for (String database : SUPPORTED_DATABASES.keySet())
            {
                databaseFound = (Boolean) getProperty(project, "database.${database}", false) &&
                        databaseTypes.contains(database)
            }
            if (!databaseFound)
                messages.add("'database.types' property (${databaseTypes}) does not specify a supported database.  Must be one of: ${SUPPORTED_DATABASES.keySet().join(", ")}.")
        }
        if (getProperty(project, 'agent.name').isEmpty())
            messages.add("'agent.name' property not specified")
        if (getProperty(project, 'teamcity.projectName').isEmpty())
            messages.add("'teamcity.projectName' property not specified")

        return messages

    }

    private static String getDatabaseName(Project project)
    {
        if ((Boolean) getProperty(project, "build.is.personal", false))
            return "LabKey_PersonalBuild"
        else
        {
            String name = getProperty(project, "teamcity.buildType.id")
            if (!(Boolean) getProperty(project, "teamcity.build.branch.is_default", true))
                name = "${getProperty(project, 'teamcity.build.branch')}_${name}"
            name.replaceAll("[/\\.\\s-]", "_")
        }
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

class TeamCityExtension
{
    String databaseName
}