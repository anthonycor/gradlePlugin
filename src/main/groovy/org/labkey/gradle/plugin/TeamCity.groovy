package org.labkey.gradle.plugin

import org.apache.commons.lang3.SystemUtils
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Copy
import org.gradle.process.JavaExecSpec
import org.labkey.gradle.task.DoThenSetup
import org.labkey.gradle.task.RunTestSuite
import org.labkey.gradle.task.UndeployModules
import org.labkey.gradle.util.DatabaseProperties
import org.labkey.gradle.util.GroupNames
import org.labkey.gradle.util.PropertiesUtils
import org.labkey.gradle.util.SqlUtils

import java.util.regex.Matcher
/**
 * Creates tasks for TeamCity to run its tests suites based on properties set in a build configuration (particularly for
 * the database properties)
 */
class TeamCity extends Tomcat
{
    private static final String TEAMCITY_INFO_FILE = "teamcity-info.xml"
    private static final String TEST_CONFIGS_DIR = "configs/config-test"
    private static final String NLP_CONFIG_FILE = "nlpConfig.xml"
    private static final String PIPELINE_CONFIG_FILE =  "pipelineConfig.xml"

    private TeamCityExtension extension

    @Override
    void apply(Project project)
    {
        super.apply(project)
        project.tomcat.assertionFlag = "-ea"
        extension = project.extensions.create("teamCity", TeamCityExtension, project)
        if (project.file("${project.tomcatDir}/localhost.truststore").exists())
        {
            project.tomcat.trustStore = "-Djavax.net.ssl.trustStore=${project.tomcatDir}/localhost.truststore"
            project.tomcat.trustStorePassword = "-Djavax.net.ssl.trustStorePassword=changeit"
        }
        project.tomcat.recompileJsp = false
        project.tomcat.debugPort = extension.getTeamCityProperty("tomcat.debug") // Tomcat intermittently hangs on shutdown if we don't specify a debug port
        project.tomcat.catalinaOpts = "-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=${project.tomcat.debugPort} -Dproject.root=${project.rootProject.projectDir.absolutePath} -Xnoagent -Djava.compiler=NONE "

        println("In TeamCity apply method, catalinaOpts is ${project.tomcat.catalinaOpts}");
        addTasks(project)
    }

    private void addTasks(Project project)
    {
        project.task("setTeamCityAgentPassword",
                group: GroupNames.TEST_SERVER,
                description: "Set the password for use in running tests",
                {   Task task ->
                    task.dependsOn(project.tasks.testJar)
                    task.doLast {
                        project.javaexec({ JavaExecSpec spec ->
                            spec.main = "org.labkey.test.util.PasswordUtil"
                            spec.classpath {
                                [project.configurations.uiTestCompile, project.tasks.testJar]
                            }
                            spec.systemProperties["labkey.server"] = project.labkey.server
                            spec.args = ["set", "teamcity@labkey.test", "yekbal1!"]
                        })
                    }
                }
        )

        project.task("cleanTestLogs",
                group: GroupNames.TEST_SERVER,
                description: "Removes log files from Tomcat and TeamCity",
                {
                    Task task ->
                    task.dependsOn project.tasks.cleanLogs, project.tasks.cleanTemp
                    task.doLast {
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
                    Task task ->
                    task.doLast {
                        killChrome(project)
                    }
                }
        )

        project.task("killFirefox",
                group: GroupNames.TEST_SERVER,
                description: "Kill Firefox processes",
                {
                    Task task ->
                    task.doLast {
                        killFirefox(project)
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
                            Matcher matcher = PropertiesUtils.PROPERTY_PATTERN.matcher(line)
                            String newLine = line
                            while (matcher.find())
                            {
                                if (matcher.group(1).equals("SEQUENCEANALYSIS_CODELOCATION") || matcher.group(1).equals("SEQUENCEANALYSIS_TOOLS"))
                                    newLine = newLine.replace(matcher.group(), extension.getTeamCityProperty("additional.pipeline.tools"))
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
                        Matcher matcher = PropertiesUtils.PROPERTY_PATTERN.matcher(line)
                        String newLine = line
                        while (matcher.find())
                        {
                            if (matcher.group(1).equals("enginePath"))
                                newLine = newLine.replace(matcher.group(), new File((String) project.labkey.externalDir, "nlp/nlp_engine.py").getAbsolutePath())
                        }
                        return newLine
                    }
                    )
                    destinationDir = new File("${ServerDeployExtension.getServerDeployDirectory(project)}/config")
                })

        if (project.findProject(":externalModules:labModules:SequenceAnalysis") != null)
        {
            project.tasks.startTomcat.dependsOn(project.tasks.createPipelineConfig)
        }
        project.tasks.startTomcat.dependsOn(project.tasks.createNlpConfig)

        project.task("validateConfiguration",
                {
                    doFirst
                            {
                                if (!extension.isValidForTestRun())
                                    throw new GradleException("TeamCity configuration problem(s): ${extension.validationMessages.join('; ')}")

                                project.logger.info("teamcity.build.branch.is_default: ${extension.getTeamCityProperty('teamcity.build.branch.is_default')}")
                                project.logger.info("teamcity.build.branch: ${extension.getTeamCityProperty('teamcity.build.branch')}")
                            }
                })


        List<Task> ciTests = new ArrayList<>()
        for (DatabaseProperties properties : project.teamCity.databaseTypes)
        {
            String suffix = properties.dbTypeAndVersion.capitalize()
            Task setUpDbTask = project.task("setUp${suffix}",
                group: GroupNames.TEST_SERVER,
                description: "Get database properties set up for running tests for ${suffix}",
                type: DoThenSetup,
                    {DoThenSetup task ->
                        task.setDatabaseProperties(properties)
                        task.fn = {
                            properties.mergePropertiesFromFile(false)
                            if (extension.dropDatabase)
                                SqlUtils.dropDatabase(project, properties.getConfigProperties())
                            properties.setJdbcUrl()
                        }
                        task.doLast {
                            properties.writeJdbcUrl()
                        }
                    }
            )

            String undeployTaskName = "undeployModulesNotFor${properties.shortType.capitalize()}"
            Task undeployTask = project.tasks.findByName(undeployTaskName)
            if (undeployTask == null)
            {
                undeployTask = project.task(undeployTaskName,
                        group: GroupNames.DEPLOY,
                        description: "Undeploy modules that are either not supposed to be built or are not supported by database ${properties.dbTypeAndVersion}",
                        type: UndeployModules,
                        {UndeployModules task ->
                            task.dbType = properties.shortType
                        }
                )
                project.tasks.startTomcat.mustRunAfter(undeployTask)
            }

            project.project(":server:test").tasks.startTomcat.mustRunAfter(setUpDbTask)
            Task ciTestTask = project.task("ciTests" + properties.dbTypeAndVersion.capitalize(),
                    group: GroupNames.TEST_SERVER,
                    description: "Run a test suite for ${properties.dbTypeAndVersion} on the TeamCity server",
                    type: RunTestSuite,
                    dependsOn: [setUpDbTask, undeployTask],
                    { RunTestSuite task ->
                        task.dbProperties = properties
                    }
            )
            ciTests.add(ciTestTask)
            ciTestTask.mustRunAfter(project.tasks.validateConfiguration)
            ciTestTask.mustRunAfter(project.tasks.cleanTestLogs)
            ciTestTask.mustRunAfter(project.tasks.startTomcat)
        }

        project.task("ciTests",
            group: GroupNames.TEST_SERVER,
            dependsOn: ciTests + project.tasks.validateConfiguration,
            description: "Run a test suite on the TeamCity server",
                {
                    doLast
                    {
                        killFirefox(project)
                    }
                })
        project.tasks.ciTests.dependsOn(project.tasks.startTomcat)
        project.tasks.ciTests.dependsOn(project.tasks.cleanTestLogs)
        project.tasks.startTomcat.mustRunAfter(project.tasks.cleanTestLogs)
    }

    private static void killChrome(Project project)
    {
        if (SystemUtils.IS_OS_WINDOWS)
        {
            project.ant.exec(executable: "taskkill")
                    {
                        arg(line:"/F /IM chromedriver.exe" )
                    }
        }
        else if (SystemUtils.IS_OS_UNIX)
        {
            project.ant.exec(executable: "killall")
                    {
                        arg(line:  "-q -KILL chromedriver")
                    }
            project.ant.exec(executable: "killall")
                    {
                        arg(line: "-q -KILL chrome")
                    }
            project.ant.exec(executable: "killall")
                    {
                        arg(line: "-q KILL BrowserBlocking")
                    }
        }
    }

    private static void killFirefox(Project project)
    {
        if (SystemUtils.IS_OS_WINDOWS)
        {
            project.ant.exec(executable: "taskkill")
                    {
                        arg(line: "/F /IM firefox.exe")
                    }
        }
        else if (SystemUtils.IS_OS_UNIX)
        {
            project.ant.exec(executable: "killall")
                    {
                        arg(line: "-q firefox")
                    }
        }
    }


    private void ensureShutdown(Project project)
    {
        String debugPort = extension.getTeamCityProperty("tomcat.debug")
        if (!debugPort.isEmpty())
        {
            println("Ensuring shutdown using port ${debugPort}")
            project.javaexec({ JavaExecSpec spec ->
                spec.main = "org.labkey.test.debug.ThreadDumpAndKill"
                spec.classpath { [project.sourceSets.debug.output.classesDir, project.configurations.debugCompile] }
                spec.args = [debugPort]
            })
        }
    }
}

class TeamCityExtension
{
    String databaseName
    Boolean dropDatabase = false
    List<DatabaseProperties> databaseTypes = new ArrayList<>()
    List<String> validationMessages = new ArrayList<>()
    Project project

    private static final Map<String, DatabaseProperties> SUPPORTED_DATABASES = new HashMap<>()
    static
    {
        SUPPORTED_DATABASES.put("postgres9.2", new DatabaseProperties("postgres9.2", "pg", "9.2"))
        SUPPORTED_DATABASES.put("postgres9.3", new DatabaseProperties("postgres9.3", "pg", "9.3"))
        SUPPORTED_DATABASES.put("postgres9.4", new DatabaseProperties("postgres9.4", "pg", "9.4"))
        SUPPORTED_DATABASES.put("postgres9.5", new DatabaseProperties("postgres9.5", "pg", "9.5"))
        SUPPORTED_DATABASES.put("postgres9.6", new DatabaseProperties("postgres9.6", "pg", "9.6"))
        SUPPORTED_DATABASES.put("sqlserver2012", new DatabaseProperties("sqlserver2012", "mssql", "2012"))
        SUPPORTED_DATABASES.put("sqlserver2014", new DatabaseProperties("sqlserver2014", "mssql", "2014"))
        SUPPORTED_DATABASES.put("sqlserver2016", new DatabaseProperties("sqlserver2016", "mssql", "2016"))
    }

    TeamCityExtension(Project project)
    {
        this.project = project
        setDatabaseProperties()
        setValidationMessages()
    }

    static Boolean isDatabaseSupported(String database)
    {
        return SUPPORTED_DATABASES.containsKey(database)
    }

    Boolean isValidForTestRun()
    {
        return validationMessages.isEmpty()
    }

    void setValidationMessages()
    {
        if (getTeamCityProperty("suite").isEmpty())
            validationMessages.add("'suite' property not specified")

        if (getTeamCityProperty("tomcat.home").isEmpty())
            validationMessages.add("'tomcat.home' property not specified")
        if (getTeamCityProperty("tomcat.port").isEmpty())
            validationMessages.add("'tomcat.port' property not specified")
        if (this.databaseTypes.isEmpty())
            validationMessages.add("'database.types' property not specified or does not specify a supported database.  Must be one of: ${SUPPORTED_DATABASES.keySet().join(", ")}.")
        if (getTeamCityProperty('agent.name').isEmpty())
            validationMessages.add("'agent.name' property not specified")
        if (getTeamCityProperty('teamcity.projectName').isEmpty())
            validationMessages.add("'teamcity.projectName' property not specified")
        if (getTeamCityProperty('tomcat.debug').isEmpty())
            validationMessages.add("'tomcat.debug' property (for debug port) not specified")
    }

    private void setDatabaseProperties()
    {
        if ((Boolean) getTeamCityProperty("build.is.personal", false))
        {
            this.databaseName = "LabKey_PersonalBuild"
            this.dropDatabase = true
        }
        else
        {
            String name = getTeamCityProperty("teamcity.buildType.id")
            if (!(Boolean) getTeamCityProperty("teamcity.build.branch.is_default", true))
                name = "${getTeamCityProperty('teamcity.build.branch')}_${name}"
            this.databaseName = name.replaceAll("[/\\.\\s-]", "_")
            String dbProperty = getTeamCityProperty('drop.database')
            this.dropDatabase = dbProperty.equals("1") || dbProperty.equalsIgnoreCase("true")
        }
        String databaseTypesProp = getTeamCityProperty("database.types")
        Boolean databaseAvailable = false
        if (!databaseTypesProp.isEmpty())
        {
            for (String type : databaseTypesProp.split(","))
            {
                if (SUPPORTED_DATABASES.containsKey(type))
                {
                    if ((Boolean) getTeamCityProperty("database.${type}", false))
                    {
                        DatabaseProperties props = SUPPORTED_DATABASES.get(type)
                        props.setProject(project)
                        props.jdbcDatabase = getDatabaseName()
                        if (!getTeamCityProperty("database.${type}.jdbcURL").isEmpty())
                        {
                            props.setJdbcURL(getTeamCityProperty("database.${type}.jdbcURL"))
                            if (getTeamCityProperty("database.${type}.port").isEmpty() && this.dropDatabase)
                                validationMessages.add("'database.${type}.port' not specified. Unable to drop database.")
                        }
                        else if (getTeamCityProperty("database.${type}.port").isEmpty())
                            validationMessages.add("database.${type}.jdbcURL and database.${type}.port not specified. Connection not possible.")
                        if (!getTeamCityProperty("database.${type}.port").isEmpty())
                            props.setJdbcPort(getTeamCityProperty("database.${type}.port"))
                        this.databaseTypes.add(props)
                        databaseAvailable = true
                    }
                }
            }
            if (!databaseAvailable)
            {
                validationMessages.add("None of the selected databases (${databaseTypesProp}) is supported on this server.")
            }
        }
    }

    static boolean isOnTeamCity(Project project)
    {
        return project.hasProperty('teamcity')
    }

    String getTeamCityProperty(String name)
    {
        return getTeamCityProperty(name, "")
    }

    Object getTeamCityProperty(String name, Object defaultValue)
    {
        getTeamCityProperty(project, name, defaultValue)
    }

    static Object getTeamCityProperty(Project project, String name, Object defaultValue)
    {
        if (isOnTeamCity(project))
            return project.teamcity[name] != null ? project.teamcity[name] : defaultValue
        else if (project.hasProperty(name))
            return project.property(name)
        else
            return defaultValue
    }
}