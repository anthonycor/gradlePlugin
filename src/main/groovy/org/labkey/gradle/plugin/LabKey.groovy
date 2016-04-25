package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Defines a set of extension properties for ease of reference. This also and adds a labkey extension
 * for some basic properties.  Other plugins can derive from this class to trigger the creation of the
 * labkey extension for the project.
 * Created by susanh on 4/13/16.
 */
class LabKey implements Plugin<Project>
{
    private static final String STAGING_MODULES_DIR = "staging/modules/"
    private static final String STAGING_WEBINF_DIR = "staging/labkeyWebapp/WEB-INF/"
    public static final String LABKEY_GROUP = "org.labkey"

    public enum DeployMode { dev, test, prod }

    @Override
    void apply(Project project)
    {
        project.extensions.create("labkey", LabKeyExtension)
        project.group = LABKEY_GROUP
        project.subprojects { subproject ->
            buildDir = "${project.rootProject.buildDir}/modules/${subproject.name}"
        }
        project.labkey {
            modulesApiDir = "${project.rootProject.buildDir}/modules-api"
            webappLibDir = "${project.rootProject.buildDir}/${STAGING_WEBINF_DIR}/lib"
            webappJspDir = "${project.rootProject.buildDir}/${STAGING_WEBINF_DIR}/jsp"

            stagingModulesDir = "${project.rootProject.buildDir}/${STAGING_MODULES_DIR}"
            explodedModuleDir = "${project.buildDir}/explodedModule"
            libDir = "${explodedModuleDir}/lib"
            srcGenDir = "${project.buildDir}/gensrc"
        }
        addTasks(project)
    }

    protected void showRepositories(Project project, String message)
    {
        println "=== ${project.name} ==="
        if (message != null)
            println message
        project.repositories.each( {
            repository ->
                for (File file : repository.getDirs())
                {
                    println(file.getAbsolutePath());
                }
        })
    }

    private void addTasks(Project project)
    {
        project.task("showRepos", {
            doLast {
                project.showRepositories(project, null);
            }
        })
    }

}

class LabKeyExtension
{
    def LabKey.DeployMode deployMode = LabKey.DeployMode.dev
    def String sourceCompatibility = '1.8'
    def String targetCompatibility = '1.8'
    def Boolean skipBuild = false // set this to true in an individual module's build.gradle file to skip building
    def String modulesApiDir
    def String webappLibDir
    def String webappJspDir
    def String explodedModuleDir
    def String explodedModuleWebDir
    def String stagingModulesDir
    def String libDir
    def String srcGenDir
}