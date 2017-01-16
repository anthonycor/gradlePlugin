package org.labkey.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Zip
import org.labkey.gradle.task.RunTestSuite
import org.labkey.gradle.util.GroupNames

/**
 * Created by susanh on 12/7/16.
 */
class TestRunner extends UiTest
{

    protected void addTasks(Project project)
    {
        super.addTasks(project)

        addJarTask(project)

        addPasswordTasks(project)

        addDataFileTasks(project)

        addExtensionsTasks(project)

        addTestSuiteTask(project)

        addAspectJ(project)

    }

    @Override
    protected void addSourceSets(Project project)
    {
        project.sourceSets {
            uiTest {
                java {
                    srcDirs = []
                    // we add the test/src directories from all projects because the test suites encompass tests
                    // across modules.
                    project.rootProject.allprojects { Project otherProj ->
                        if (otherProj.file(UiTest.TEST_SRC_DIR).exists())
                        {
                            srcDirs += otherProj.file(UiTest.TEST_SRC_DIR)
                        }
                    }
                }
                output.classesDir = "${project.buildDir}/classes"
            }
        }

    }

    @Override
    protected void addConfigurations(Project project)
    {
        super.addConfigurations(project)
        project.configurations {
            aspectj
        }
    }

    @Override
    protected void addDependencies(Project project)
    {
        super.addDependencies(project)
        project.dependencies {
            aspectj "org.aspectj:aspectjtools:${project.aspectjVersion}"
            compile "org.aspectj:aspectjrt:${project.aspectjVersion}"
            compile "org.aspectj:aspectjtools:${project.aspectjVersion}"

            compile project.files("${System.properties['java.home']}/../lib/tools.jar")
            compile "org.seleniumhq.selenium:selenium-server:${project.seleniumVersion}"
            compile "com.googlecode.sardine:sardine:${project.sardineVersion}"
            compile "junit:junit:${project.junitVersion}"
        }
    }

    @Override
    protected void addArtifacts(Project project)
    {
        project.artifacts {
            uiTestCompile project.tasks.testJar
        }
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
                                [project.configurations.uiTestCompile, project.tasks.testJar]
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
                                        [project.configurations.uiTestCompile, project.tasks.testJar]
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
        project.task("uiTests",
                overwrite: true, // replace the task that would run all of the tests
                group: GroupNames.VERIFICATION,
                description: "Run a LabKey test suite as defined by ${project.file(testRunnerExt.propertiesFile)} and overridden on the command line by -P<prop>=<value>",
                type: RunTestSuite
        )
    }

    private void addJarTask(Project project)
    {
        project.task("testJar",
                group: GroupNames.BUILD,
                type: Jar,
                description: "produce jar file of test classes",
                {
                    from project.sourceSets.uiTest.output
                    baseName "labkeyTest"
                    version project.version
                    destinationDir = new File("${project.buildDir}/libs")
                })
    }

    private void addAspectJ(Project project)
    {
        project.tasks.compileUiTestJava.doLast {
            ant.taskdef(
                    resource: "org/aspectj/tools/ant/taskdefs/aspectjTaskdefs.properties",
                    classpath: project.configurations.aspectj.asPath
            )
            ant.iajc(
                    destdir: "${project.buildDir}/classes",
                    source: project.labkey.sourceCompatibility,
                    target: project.labkey.targetCompatibility,
                    classpath: project.configurations.uiTestCompile.asPath,
                    {
                        project.sourceSets.uiTest.java.srcDirs.each {
                            src(path: it)
                        }
                    }
            )
        }
    }
}
