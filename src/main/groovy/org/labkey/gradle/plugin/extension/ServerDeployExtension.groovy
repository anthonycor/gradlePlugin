package org.labkey.gradle.plugin.extension

import org.gradle.api.Project

/**
 * Created by susanh on 4/23/17.
 */
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

    static String getModulesDeployDirectory(Project project)
    {
        return "${getServerDeployDirectory(project)}/modules"
    }
}
