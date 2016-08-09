package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileTree
/**
 * Defines a set of extension properties for ease of reference. This also and adds a labkey extension
 * for some basic properties.  Other plugins can derive from this class to trigger the creation of the
 * labkey extension for the project.
 * Created by susanh on 4/13/16.
 */
class LabKey implements Plugin<Project>
{
    private static final String STAGING_DIR = "staging"
    private static final String STAGING_MODULES_DIR = "${STAGING_DIR}/modules/"
    private static final String STAGING_WEBAPP_DIR = "${STAGING_DIR}/labkeyWebapp"
    private static final String STAGING_WEBINF_DIR = "${STAGING_WEBAPP_DIR}/WEB-INF/"
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
        project.versioning { // TODO
            scm = "svn"
//            user = "${project.svn_user}"
//            password = "${project.svn_password}"
        }
        project.labkey {
            modulesApiDir = "${project.rootProject.buildDir}/modules-api"

            webappClassesDir = "${project.rootProject.buildDir}/${STAGING_WEBINF_DIR}/classes"
            webappLibDir = "${project.rootProject.buildDir}/${STAGING_WEBINF_DIR}/lib"
            webappJspDir = "${project.rootProject.buildDir}/${STAGING_WEBINF_DIR}/jsp"

            stagingWebInfDir = "${project.rootProject.buildDir}/${STAGING_WEBINF_DIR}"
            stagingWebappDir = "${project.rootProject.buildDir}/${STAGING_WEBAPP_DIR}"
            stagingModulesDir = "${project.rootProject.buildDir}/${STAGING_MODULES_DIR}"
            explodedModuleDir = "${project.buildDir}/explodedModule"
            explodedModuleWebDir = "${explodedModuleDir}/web"
            libDir = "${explodedModuleDir}/lib"
            srcGenDir = "${project.buildDir}/gensrc"

            externalDir = "${project.rootDir}/external"
            externalLibDir = "${externalDir}/lib"

            webappDir = "${project.projectDir}/webapp"
        }
        addTasks(project)
    }

    protected static File getJavaRtDir()
    {
        File rtDir = new File(System.getenv('JAVA_HOME'), "/lib/java8");
        if (!rtDir.exists())
        {
            rtDir = new File(System.getenv('JAVA_HOME'), "/lib")
        }
        return rtDir;
    }

    protected static FileTree getJavaBootClasspath(Project project)
    {
        File rtDir = getJavaRtDir();
        if (rtDir.exists())
        {
            FileTree tree = project.fileTree(dir: rtDir.getPath(), include: ["*.jar"])
        }
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
                this.showRepositories(project, null);
            }
        })
    }

}

class StagingExtension
{
    def String stagingWebInfDir
    def String stagingModulesDir
    def String stagingWebappDir
}

// TODO split this into separate extensions for deploy, staging, et al.
class LabKeyExtension
{
    // TODO this should be based on a property (read from a .properties file or command line)
    def LabKey.DeployMode deployMode = LabKey.DeployMode.dev
    def String sourceCompatibility = '1.8'
    def String targetCompatibility = '1.8'
    def Boolean skipBuild = false // set this to true in an individual module's build.gradle file to skip building
    def String modulesApiDir
    def String webappClassesDir
    def String webappLibDir
    def String webappJspDir
    def String explodedModuleDir
    def String explodedModuleWebDir
    def String stagingWebInfDir
    def String stagingModulesDir
    def String stagingWebappDir
    def String libDir
    def String srcGenDir
    def String externalDir
    def String externalLibDir
    def String webappDir
}
