package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Zip
import org.labkey.gradle.task.RunTestSuite
import org.labkey.gradle.util.GroupNames
import org.labkey.gradle.util.PropertiesUtils
/**
 * Created by susanh on 12/7/16.
 */
class TestRunner implements Plugin<Project>
{
    TestRunnerExtension testRunnerExt

    @Override
    void apply(Project project)
    {
        testRunnerExt = project.extensions.create("testRunner", TestRunnerExtension, project)

        addTasks(project)
    }

    private void addTasks(Project project)
    {
        addJarTask(project)

        addPasswordTasks(project)

        addDataFileTasks(project)

        addExtensionsTasks(project)

        addTestSuiteTask(project)

    }

    private void addPasswordTasks(Project project)
    {

        project.task("setPassword",
                group: GroupNames.TEST,
                description: "Set the password for use in running tests",
                {
                    dependsOn(project.tasks.testJar)
                    doFirst({
                        project.javaexec({
                            main = "org.labkey.test.util.PasswordUtil"
                            classpath {
                                [project.configurations.compile, project.tasks.testJar]
                            }
                            systemProperties["labkey.server"] = project.labkey.server
                            args = ["set"]
                            standardInput = System.in
                        })
                    })
                }
        )


        project.task("ensurePassword",
                group: GroupNames.TEST,
                description: "Ensure that the password property used for running tests has been set",
                {
                    dependsOn(project.tasks.testJar)
                    doFirst(
                            {
                                project.javaexec({
                                    main = "org.labkey.test.util.PasswordUtil"
                                    classpath {
                                        [project.configurations.compile, project.tasks.testJar]
                                    }
                                    systemProperties["labkey.server"] = project.labkey.server
                                    args = ["ensure"]
                                    standardInput = System.in
                                })
                            })
                }
        )
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

    private void addTestSuiteTask(Project project)
    {
        project.task("uiTest",
                overwrite: true,
                group: "Verification",
                description: "Run a LabKey test suite as defined by ${project.file(testRunnerExt.propertiesFile)} and overridden on the command line by -P<prop>=<value>",
                type: RunTestSuite
        )
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
        for (String name : properties.propertyNames())
        {
            // two of the test.property names ('test' and 'clean') are the same as the
            // names of default tasks that come with the Java plugin.  All tasks are also
            // properties of a project, so we test for a String type (passed through the
            // command line) and override the property in the file only if we have a new
            // String.
            if (project.hasProperty(name) && project.property(name) instanceof String)
            {
                properties.setProperty(name, project.property(name).toString())
            }

        }
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
