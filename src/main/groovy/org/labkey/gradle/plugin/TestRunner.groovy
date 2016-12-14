package org.labkey.gradle.plugin

import org.apache.commons.lang3.SystemUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Zip
import org.labkey.gradle.util.GroupNames
import org.labkey.gradle.util.PropertiesUtils
/**
 * Created by susanh on 12/7/16.
 */
class TestRunner implements Plugin<Project>
{

    @Override
    void apply(Project project)
    {
        project.extensions.create("testRunner", TestRunnerExtension, project)

        addTasks(project)
    }

    private void addTasks(Project project)
    {
        addPasswordTasks(project)

        addDataFileTasks(project)

        addExtensionsTasks(project)

        addJarTask(project)

        configureUnitTestTask(project)

    }

    private void addPasswordTasks(Project project)
    {

        project.task("setPassword",
                group: GroupNames.TEST,
                description: "Set the password for use in running tests").doFirst({
            project.javaexec({
                main = "org.labkey.test.util.PasswordUtil"
                classpath {
                    [project.configurations.compile, project.tasks.jar]
                }
                systemProperties["labkey.server"] = project.labkey.server
                args = ["set"]
                standardInput = System.in
            })
        })

        project.task("ensurePassword",
                group: GroupNames.TEST,
                description: "Ensure that the password property used for running tests has been set").doFirst(
                {
                    project.javaexec({
                        main = "org.labkey.test.util.PasswordUtil"
                        classpath {
                            [project.configurations.compile, project.tasks.jar]
                        }
                        systemProperties["labkey.server"] = project.labkey.server
                        args = ["ensure"]
                        standardInput = System.in
                    })
                })
    }

    private void addDataFileTasks(Project project)
    {
        List<File> directories = new ArrayList<>();

        project.rootProject.allprojects({ Project p ->
            File dataDir = p.file("test/sampledata")
            if (dataDir.exists())
            {
                directories.add(dataDir)
            }
        })

        File sampleDataFile = new File("${project.buildDir}/sampledata.dirs")

        project.task("writeSampleDataFile",
                group: GroupNames.TEST,
                description: "Produce the file with all sampledata directories for use in running tests",
                {
                    inputs.files directories
                    outputs.file sampleDataFile
                }
        ).doLast({
            List<String> dirNames = new ArrayList<>();

            directories.each({File file ->
                dirNames.add(file.getAbsolutePath())
            })

            FileOutputStream outputStream = new FileOutputStream(sampleDataFile);
            Writer writer = null
            try
            {
                writer = new OutputStreamWriter(outputStream);
                dirNames.add("${project.rootDir}/server/test/data")
                writer.write(String.join(";", dirNames))
            }
            finally
            {
                if (writer != null)
                    writer.close();
            }
        })
    }

    private void addExtensionsTasks(Project project)
    {
        File extensionsDir = project.file("chromeextensions")
        if (extensionsDir.exists())
        {
            List<Task> extensionsZipTasks = new ArrayList<>();
            extensionsDir.eachDir({
                File dir ->

                    def extensionTask = project.task("package" + dir.getName().capitalize(),
                            description: "Package the ${dir.getName()} chrome extension used for testing",
                            type: Zip,
                            {
                                archiveName = "${dir.getName()}.zip"
                                from dir
                                destinationDir = new File("${project.buildDir}/chromextensions")
                            })
                    extensionsZipTasks.add(extensionTask)
            })
            project.task("packageChromeExtensions",
                    description: "Package all chrome extensions used for testing",
                    dependsOn: extensionsZipTasks)
        }
    }

    private void configureUnitTestTask(Project project)
    {
        TestRunnerExtension testEx = (TestRunnerExtension) project.getExtensions().getByName("testRunner")
        List<String> jvmArgsList = ["-Xmx512m",
                                    "-Xdebug",
                                    "-Xrunjdwp:transport=dt_socket,server=y,suspend=${project.testRunner.debugSuspendSelenium},address=${testEx.getTestProperty("selenium.debug.port")}",
                                    "-Dfile.encoding=UTF-8",
                                    "-Duser.timezone=${System.getenv().get("TZ")}"]
        if (!project.tomcat.trustStore.isEmpty() && !project.tomcat.trustStorePassword.isEmpty())
        {
            jvmArgsList += [project.tomcat.trustStore, project.tomcat.trustStorePassword]
        }

        project.tasks.test {
            jvmArgs jvmArgsList
            TestRunnerExtension testExtension = (TestRunnerExtension) project.extensions.getByName("testRunner")
            Properties testProperties = testExtension.getProperties()
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
                {
                    systemProperty "webdriver.chrome.driver", "${project.projectDir}/bin/linux/amd64/chromedriver"
                }
                else if (SystemUtils.OS_ARCH.equals("i386"))
                    systemProperties "webdriver.chrome.driver", "${project.projectDir}/bin/linux/i386/chromedriver"
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
        }
    }

    private void addJarTask(Project project)
    {
        project.task("testJar",
                group: "Build",
                type: Jar,
                description: "produce jar file of test classes",
                {
                    from project.sourceSets.test.output
                    baseName "labkeyTest"
                    version project.version
                    destinationDir = new File("${project.buildDir}/libs")
                })
    }
}

class TestRunnerExtension
{
    String propertiesFile = "test.properties"
    String debugSuspendSelenium = "n"

    private Properties properties = null
    private Project project

    String logDir = "test/logs"

    TestRunnerExtension(Project project)
    {
        this.project = project
        setProperties(project);
    }

    private void setProperties(Project project)
    {
        // read database configuration, but don't include jdbcUrl and other non-"database"
        // properties because they "cause problems" (quite from the test/build.xml file)
        Properties dbProperties = PropertiesUtils.readDatabaseProperties(project)
        this.properties = new Properties();
        for (String name : dbProperties.stringPropertyNames())
        {
            if (name.contains("database"))
                this.properties.put(name, dbProperties.getProperty(name))
        }
        // read test.properties file
        PropertiesUtils.readProperties(project.file(propertiesFile), this.properties)
    }

    Properties getProperties()
    {
        return this.properties;
    }

    Object getTestProperty(String name)
    {
        if (project.hasProperty(name))
            return project.property(name)
        else
        {
            return properties.get(name)
        }
    }

}
