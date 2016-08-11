package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.JavaExec

/**
 * Created by susanh on 8/10/16.
 */
class ServerBootstrap implements Plugin<Project>
{
    @Override
    void apply(Project project)
    {
        addTasks(project)
    }

    private void addTasks(Project project)
    {
        // TODO convert this to Gradle
        def Task buildBootstrapJar = project.task(
                'buildBootstrapJar',
                group: ServerDeploy.GROUP_NAME,
                description: "Build the LabKey bootstrap jar file",
                {
                    ant.setProperty('tomcat.home', project.tomcatDir)
                    ant.setProperty('build.dir', project.rootProject.buildDir )
                    ant.setProperty('java.source.and.target', project.labkey.sourceCompatibility)
                    ant.setProperty('basedir', project.project(":server").projectDir)
                    ant.ant(dir: "${project.rootDir}/server", target: 'build_bootstrap_jar')
                }
        )

        def Task copyBootstrapJar = project.task(
                'copyBootstrapJar',
                group: ServerDeploy.GROUP_NAME,
                type: Copy,
                description: "Copy LabKey bootstrap jar to Tomcat",
                {
                    from "${project.rootProject.buildDir}/labkeyBootstrap.jar"
                    into "${project.tomcatDir}/lib"
                }
        )
        copyBootstrapJar.dependsOn(buildBootstrapJar)
        project.project(":server").tasks.deployApp.dependsOn(copyBootstrapJar)

        def Task createApiFilesList = project.task(
                'createApiFilesList',
                group: ServerDeploy.GROUP_NAME,
                description: 'Create an index of the files in the application so extraneous files can be removed during bootstrapping',
                type: JavaExec,
                {
                    main = "org.labkey.bootstrap.DirectoryFileListWriter"
                    workingDir = project.labkey.stagingWebappDir
                    classpath {
                        [
                                project.file("${project.rootProject.buildDir}/bootstrap")
                        ]
                    }
                }
        )
        createApiFilesList.dependsOn(buildBootstrapJar)
        project.project(":server").tasks.deployApp.dependsOn(createApiFilesList)
    }
}
