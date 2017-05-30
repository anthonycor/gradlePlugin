package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.DeleteSpec
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Delete
import org.labkey.gradle.plugin.extension.ServerDeployExtension
import org.labkey.gradle.plugin.extension.StagingExtension
import org.labkey.gradle.task.*
import org.labkey.gradle.util.GroupNames

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
/**
 * First stages then deploys the application locally to the tomcat directory
 */
class ServerDeploy implements Plugin<Project>
{

    private ServerDeployExtension serverDeploy

    @Override
    void apply(Project project)
    {
        serverDeploy = project.extensions.create("serverDeploy", ServerDeployExtension)

        serverDeploy.dir = ServerDeployExtension.getServerDeployDirectory(project)
        serverDeploy.modulesDir = "${serverDeploy.dir}/modules"
        serverDeploy.webappDir = "${serverDeploy.dir}/labkeyWebapp"
        serverDeploy.binDir = "${serverDeploy.dir}/bin"
        serverDeploy.rootWebappsDir = "${project.rootDir}/webapps"
        serverDeploy.pipelineLibDir = "${serverDeploy.dir}/pipelineLib"
        addTasks(project)
    }

    private void addTasks(Project project)
    {
        Task deployAppTask = project.task(
                "deployApp",
                group: GroupNames.DEPLOY,
                type: DeployApp,
                description: "Deploy the application locally into ${serverDeploy.dir}"
        )

        StagingExtension staging = project.getExtensions().getByType(StagingExtension.class)

        // The staging step complicates things, but it is currently needed for the following reasons:
        // - we currently depend on it for generating the apiFilesList that determines which libraries to keep and which to remove from WEB-INF/lib
        //   (could be accomplished with a sync task perhaps)
        // - We need to put certain libraries in WEB-INF/lib because the RecompilingJspClassLoader uses that in its classpath
        // - We want to make sure tomcat doesn't restart multiple times when deploying the application.
        //   (seems like it could be avoided as the copy being done here is just as atomic as the copy from deployModules)
        Task stageModulesTask = project.task(
                "stageModules",
                group: GroupNames.DEPLOY,
                description: "Stage the modules for the application into ${staging.dir}",
        ).doLast({
            project.ant.copy(
                    todir: staging.modulesDir,
                    preserveLastModified: true // this is important so we don't re-explode modules that have not changed
            )
                    {
                        project.configurations.modules { Configuration collection ->
                            collection.addToAntBuilder(project.ant, "fileset", FileCollection.AntType.FileSet)
                        }
                    }
        })

        stageModulesTask.dependsOn project.configurations.modules

        Task stageJarsTask = project.task(
                "stageJars",
                group: GroupNames.DEPLOY,
                description: "Stage select jars into ${staging.dir}"
        ).doLast({
            project.ant.copy(
                    todir: staging.libDir,
                    preserveLastModified: true
            )
                    {
                        project.configurations.jars { Configuration collection ->
                            collection.addToAntBuilder(project.ant, "fileset", FileCollection.AntType.FileSet)
                        }
                    }
        })
        stageJarsTask.dependsOn project.configurations.jars


        Task stageTomcatJarsTask = project.task(
                "stageTomcatJars",
                group: GroupNames.DEPLOY,
                description: "Stage files for copying into the tomcat/lib directory into ${staging.tomcatLibDir}"
        ).doLast({
            project.ant.copy(
                    todir: staging.tomcatLibDir,
                    preserveLastModified: true
            )
                    {
                        project.configurations.tomcatJars { Configuration collection ->
                            collection.addToAntBuilder(project.ant, "fileset", FileCollection.AntType.FileSet)
                        }
                        // Put unversioned files into the tomcatLibDir.  These files are meant to be copied into
                        // the tomcat/lib directory when deploying a build or a distribution.  When version numbers change,
                        // you will end up with multiple versions of these jar files on the classpath, which will often
                        // result in problems of compatibility.  Additionally, we want to maintain the (incorrect) names
                        // of the files that have been used with the Ant build process.
                        //
                        // We may employ CATALINA_BASE in order to separate our libraries from the ones that come with
                        // the tomcat distribution. This will require updating our instructions for installation by clients
                        // but would allow us to use artifacts with more self-documenting names.
                        chainedmapper()
                                {
                                    flattenmapper()
                                    // get rid of the version numbers on the jar files
                                    regexpmapper(from: "^(.*?)(-\\d+(\\.\\d+)*(-\\.*)?(-SNAPSHOT)?)?\\.jar", to: "\\1.jar")
                                    filtermapper()
                                            {
                                                replacestring(from: "mysql-connector-java", to: "mysql") // the Ant build used mysql.jar
                                                replacestring(from: "javax.mail", to: "mail") // the Ant build used mail.jar
                                            }
                                }
                    }
        })

        stageTomcatJarsTask.dependsOn project.configurations.tomcatJars


        Task stageRemotePipelineJarsTask = project.task(
                "stageRemotePipelineJars",
                group: GroupNames.DEPLOY,
                description: "Copy files needed for using remote pipeline jobs into ${staging.pipelineLibDir}"
        ).doLast(
                {
                    project.ant.copy(
                            todir: staging.pipelineLibDir,
                            preserveLastModified: true
                    )
                            {
                                project.configurations.remotePipelineJars { Configuration collection ->
                                    collection.addToAntBuilder(project.ant, "fileset", FileCollection.AntType.FileSet)

                                }
                            }
                }
        )

        stageRemotePipelineJarsTask.dependsOn project.configurations.remotePipelineJars

        project.task(
                "stageApp",
                group: GroupNames.DEPLOY,
                description: "Stage modules and jar files into ${staging.dir}",
                { Task task ->
                    task.dependsOn stageModulesTask
                    task.dependsOn stageJarsTask
                    task.dependsOn stageTomcatJarsTask
                    task.dependsOn stageRemotePipelineJarsTask
                }
        )
        deployAppTask.dependsOn(project.tasks.stageApp)

        Task setup = project.task(
                "setup",
                group: GroupNames.DEPLOY,
                type: DoThenSetup,
                description: "Installs labkey.xml and various jar files into the tomcat directory.  Sets default database properties."
        )
        deployAppTask.dependsOn(setup)
        // stage the application first to try to avoid multiple Tomcat restarts
        setup.mustRunAfter(project.tasks.stageApp)

        Task log4jTask = project.task(
                'configureLog4j',
                group: GroupNames.DEPLOY,
                type: ConfigureLog4J,
                description: "Edit and copy log4j.xml file",
        )
        deployAppTask.dependsOn(log4jTask)
        // stage the application first to try to avoid multiple Tomcat restarts
        log4jTask.mustRunAfter(project.tasks.stageApp)

        project.task(
                "stageDistribution",
                group: GroupNames.DISTRIBUTION,
                description: "Populate the staging directory using a LabKey distribution file from build/dist or directory specified with distDir property. Use property distType to specify zip or tar.gz (default).",
                type: StageDistribution
        )

        Task deployDistTask = project.task(
                "deployDistribution",
                type: DeployApp,
                group: GroupNames.DISTRIBUTION,
                description: "Deploy a LabKey distribution file from build/dist or directory specified with distDir property.  Use property distType to specify zip or tar.gz (default).",
        )
        deployDistTask.dependsOn(project.tasks.stageDistribution)
        deployDistTask.dependsOn(log4jTask)
        deployDistTask.dependsOn(setup)
        // This may prevent multiple Tomcat restarts
        setup.mustRunAfter(project.tasks.stageDistribution)
        log4jTask.mustRunAfter(project.tasks.stageDistribution)

        project.task(
                'undeployModules',
                group: GroupNames.DEPLOY,
                description: "Removes all module files and directories from the deploy and staging directories",
                type: UndeployModules
        )

        project.task(
                'cleanStaging',
                group: GroupNames.DEPLOY,
                type: Delete,
                description: "Removes the staging directory ${staging.dir}",
                { DeleteSpec spec ->
                    spec.delete staging.dir
                }
        )

        Task cleanDeploy = project.task(
                'cleanDeploy',
                group: GroupNames.DEPLOY,
                type: Delete,
                description: "Removes the deploy directory ${serverDeploy.dir}",
                dependsOn: project.tasks.stopTomcat,
                { DeleteSpec spec ->
                    spec.delete serverDeploy.dir
                }
        )

        project.task(
                "cleanTomcatLib",
                group: GroupNames.DEPLOY,
                description: "Remove the jar files deployed to the tomcat/lib directory"
        ).doLast {
            deleteTomcatLibs(project)
        }

        project.task(
                "cleanAndDeploy",
                group: GroupNames.DEPLOY,
                type: DeployApp,
                description: "Removes the deploy directory ${serverDeploy.dir} then deploys the application locally",
                dependsOn: project.tasks.cleanDeploy
        )

        Task cleanBuild = project.task(
                "cleanBuild",
                group: GroupNames.DEPLOY,
                type: Delete,
                description: "Remove the build directory ${project.rootProject.buildDir}",
                dependsOn: project.tasks.stopTomcat,
                { DeleteSpec spec ->
                    spec.delete project.rootProject.buildDir
                }
        )

    }

    private static void deleteTomcatLibs(Project project)
    {
        Files.newDirectoryStream(Paths.get(project.tomcatDir, "lib"), "${ServerBootstrap.JAR_BASE_NAME}*.jar").each { Path path ->
            project.delete path.toString()
        }
        // FIXME this fails when cleaning after changing version but before publishing any artifacts.
        // We may want to do some globbing on prefix without the version number
//        FileTree tree = project.fileTree(dir: "${project.tomcatDir}/lib",
//                includes: ["ant.jar", "mail.jar", "jtds.jar", "mysql.jar", "postgresql.jar", "${ServerBootstrap.JAR_BASE_NAME}*.jar"]
//        )
//        project.delete tree

        project.configurations.tomcatJars.files.each {File jarFile ->
            File libFile = new File("${project.tomcatDir}/lib/${jarFile.getName()}")
            if (libFile.exists())
                project.delete libFile.getAbsolutePath()
        }

        // also get rid of (un-versioned) jars that were deployed from ant, if there are any
        List<String> unversionedJars = ["ant.jar", "mail.jar", "jtds.jar", "mysql.jar", "postgresql.jar"]

        unversionedJars.each{String name ->
            File libFile = new File("${project.tomcatDir}/lib/${name}")
            if (libFile.exists())
                project.delete libFile.getAbsolutePath()
        }
    }
}



