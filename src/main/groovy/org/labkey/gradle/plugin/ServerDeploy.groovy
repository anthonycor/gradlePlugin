package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.CopySpec
import org.gradle.api.file.DeleteSpec
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Delete
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

        // FIXME staging step complicates things, but we currently depend on it for generating the
        // apiFilesList that determines which libraries to keep and which to remove from WEB-INF/lib
        // We also need to put libraries in WEB-INF/lib because the RecompilingJspClassLoader uses that in its classpath
        Task stageModulesTask = project.task(
                "stageModules",
                group: GroupNames.DEPLOY,
                type: Copy,
                description: "Stage the modules for the application into ${staging.dir}",
                {
                    from project.configurations.modules
                    into staging.modulesDir
                }
        )

        // N.B. It might be preferable to not have the stageApiTask and declare
        // dependencies or exclusions such that we can pull the :server:api transitive
        // dependencies but exclude the jars from the tomcat lib.
        Task stageApiTask = project.task(
                "stageApi",
                group: GroupNames.DEPLOY,
                type: Copy,
                description: "Stage the api jar files and the dependencies into ${staging.dir}",
                {
                    from project.project(":server:api").configurations.external
                    into staging.libDir
                }
        )

        Task stageJarsTask = project.task(
                "stageJars",
                group: GroupNames.DEPLOY,
                type: Copy,
                description: "Stage select jars into ${staging.dir}",
                {
                    from project.configurations.jars
                    into staging.libDir
                }
        )

        Task stageJspJarsTask = project.task(
                "stageJspJars",
                group: GroupNames.DEPLOY,
                type: Copy,
                description: "Stage select jsp jar files into ${staging.dir} ",
                { CopySpec copy ->
                    copy.from project.configurations.jspJars
                    copy.into staging.jspDir
                }
        )

        Task stageTomcatJarsTask = project.task(
                "stageTomcatJars",
                group: GroupNames.DEPLOY,
                type: Copy,
                description: "Stage files for copying into the tomcat/lib directory into ${staging.tomcatLibDir}",
                {
                    CopySpec copy ->
                        copy.from project.configurations.tomcatJars
                        copy.into staging.tomcatLibDir
                }
        )

        Task stageRemotePipelineJarsTask = project.task(
                "stageRemotePipelineJars",
                group: GroupNames.DEPLOY,
                type: Copy,
                description: "Copy files needed for using remote pipeline jobs into ${staging.pipelineLibDir}",
                {
                    CopySpec copy ->
                        copy.from project.configurations.remotePipelineJars
                        copy.into staging.pipelineLibDir
                }
        )

        project.task(
                "stageApp",
                group: GroupNames.DEPLOY,
                description: "Stage modules and jar files into ${staging.dir}",
                { Task task ->
                    task.dependsOn stageApiTask
                    task.dependsOn stageModulesTask
                    task.dependsOn stageJspJarsTask
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

        Task log4jTask = project.task(
                'configureLog4j',
                group: GroupNames.DEPLOY,
                type: ConfigureLog4J,
                description: "Edit and copy log4j.xml file",
        )
        deployAppTask.dependsOn(log4jTask)

        project.task(
                "stageDistribution",
                group: GroupNames.DISTRIBUTION,
                description: "Populate the staging directory using a LabKey distribution file from build/dist or directory specified with distDir property. Use property distType to specify zip or tar.gz (default).",
                type: StageDistribution
        )

        Task deployDistTask = project.task(
                "deployDistribution",
                group: GroupNames.DISTRIBUTION,
                description: "Deploy a LabKey distribution file from build/dist or directory specified with distDir property.  Use property distType to specify zip or tar.gz (default).",
        )
        deployDistTask.dependsOn(project.tasks.stageDistribution)
        deployDistTask.dependsOn(setup)

        project.task(
                'undeployModules',
                group: GroupNames.DEPLOY,
                description: "Moves the module files out of the deploy directory and back to staging",
                type: UndeployModules
        )

        project.task(
                'cleanStaging',
                group: GroupNames.DEPLOY,
                type: Delete,
                description: "Removes the staging directory (${staging.dir})",
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
        cleanDeploy.doLast {
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
                { DeleteSpec spec ->
                    spec.delete project.rootProject.buildDir
                }
        )
        cleanBuild.doLast {
            deleteTomcatLibs(project)
        }

    }

    private static void deleteTomcatLibs(Project project)
    {
        Files.newDirectoryStream(Paths.get(project.tomcatDir, "lib"), "${ServerBootstrap.JAR_BASE_NAME}*.jar").each { Path path ->
            project.delete path.toString()
        }
        project.configurations.tomcatJars.files.each {File jarFile ->
            File libFile = new File("${project.tomcatDir}/lib/${jarFile.getName()}")
            if (libFile.exists())
                project.delete libFile.getAbsolutePath()
        }
    }
}


class ServerDeployExtension
{
    String dir
    String modulesDir
    String webappDir
    String binDir
    String rootWebappsDir
    String pipelineLibDir

    static String getServerDeployDirectory(Project project)
    {
        return "${project.rootProject.buildDir}/deploy"
    }
}

