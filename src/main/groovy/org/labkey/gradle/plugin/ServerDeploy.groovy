package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Delete
import org.labkey.gradle.task.ConfigureLog4J
import org.labkey.gradle.task.DeployApp
import org.labkey.gradle.task.DoThenSetup
import org.labkey.gradle.task.UndeployModules
import org.labkey.gradle.util.GroupNames

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * First stages then deploys the application locally to the tomcat directory
 */
class ServerDeploy implements Plugin<Project>
{

    @Override
    void apply(Project project)
    {
        project.extensions.create("serverDeploy", ServerDeployExtension)

        project.serverDeploy {
            dir = ServerDeployExtension.getServerDeployDirectory(project)
            modulesDir = "${dir}/modules"
            webappDir = "${dir}/labkeyWebapp"
            binDir = "${dir}/bin"
            rootWebappsDir = "${project.rootDir}/webapps"
        }
        addTasks(project)
    }

    private static void addTasks(Project project)
    {
        Task deployAppTask = project.task(
                "deployApp",
                group: GroupNames.DEPLOY,
                type: DeployApp,
                description: "Deploy the application locally into ${project.serverDeploy.dir}"
        )

        // FIXME staging step complicates things, but we currently depend on it for generating the
        // apiFilesList that determines which libraries to keep and which to remove from WEB-INF/lib
        // We need to put libraries in WEB-INF/lib because the RecompilingJspClassLoader uses that in its classpath
        // for recompiling JSP's.
        def Task stageModulesTask = project.task(
                "stageModules",
                group: GroupNames.DEPLOY,
                type: Copy,
                description: "Stage the modules for the application into ${project.staging.dir}",
                {
                    from project.configurations.modules
                    into project.staging.modulesDir
                }
        )

        def Task stageApiTask = project.task(
                "stageApi",
                group: GroupNames.DEPLOY,
                type: Copy,
                description: "Stage the api jar files and the dependencies into ${project.staging.dir}",
                {
                    from project.project(":server:api").configurations.external
                    into project.staging.libDir
                }
        )

        // N.B. It might be preferable to not have the stageApiTask and declare
        // dependencies or exclusions such that we can pull the :server:api transitive
        // dependencies but exclude the jars from the tomcat lib.
        def Task stageJarsTask = project.task(
                "stageJars",
                group: GroupNames.DEPLOY,
                type: Copy,
                description: "Stage select jars into ${project.staging.dir}",
                {
                    from project.configurations.jars
                    into project.staging.libDir
                }
        )

        def Task stageJspJarsTask = project.task(
                "stageJspJars",
                group: GroupNames.DEPLOY,
                type: Copy,
                description: "Stage select jsp jar files into ${project.staging.dir} ",
                {
                    from project.configurations.jspJars
                    into project.staging.jspDir
                }
        )

        project.task(
                "stageApp",
                group: GroupNames.DEPLOY,
                description: "Stage modules and jar files into ${project.staging.dir}",
                {
                    dependsOn stageApiTask
                    dependsOn stageModulesTask
                    dependsOn stageJspJarsTask
                    dependsOn stageJarsTask
                }
        )
        deployAppTask.dependsOn(project.tasks.stageApp)

        def Task setup = project.task(
                "setup",
                group: GroupNames.DEPLOY,
                type: DoThenSetup,
                description: "Installs labkey.xml and various jar files into the tomcat directory.  Sets default database properties."
        )
        deployAppTask.dependsOn(setup)

        def Task log4jTask = project.task(
                'configureLog4j',
                group: GroupNames.DEPLOY,
                type: ConfigureLog4J,
                description: "Edit and copy log4j.xml file",
        )
        deployAppTask.dependsOn(log4jTask)

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
                description: "Removes the staging directory (${project.staging.dir})",
                {
                    delete project.staging.dir
                }
        )

        project.task(
                'cleanDeploy',
                group: GroupNames.DEPLOY,
                type: Delete,
                description: "Removes the deploy directory (${project.serverDeploy.dir})",
                {
                    delete project.serverDeploy.dir
                }
        )

        def Task cleanAndDeploy = project.task(
                "cleanAndDeploy",
                group: GroupNames.DEPLOY,
                type: DeployApp,
                description: "Removes the deploy directory ${project.serverDeploy.dir} then deploys the application locally",
        )
        cleanAndDeploy.doFirst{
            project.delete(project.serverDeploy.dir)
        }

        project.task(
                "cleanBuild",
                group: GroupNames.DEPLOY,
                type: Delete,
                description: "Remove the build directory ${project.rootProject.buildDir}",
                {
                    delete project.rootProject.buildDir
                    Files.newDirectoryStream(Paths.get(project.tomcatDir, "lib"), "${ServerBootstrap.JAR_BASE_NAME}*.jar").each { Path path ->
                      delete path.toString()
                    }
                }
        )
    }
}


class ServerDeployExtension
{
    String dir
    String modulesDir
    String webappDir
    String binDir
    String rootWebappsDir

    static String getServerDeployDirectory(Project project)
    {
        return "${project.rootProject.buildDir}/deploy";
    }
}

