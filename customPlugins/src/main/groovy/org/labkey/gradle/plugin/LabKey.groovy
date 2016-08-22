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
    private static final String STAGING_DIR = "staging"
    private static final String STAGING_MODULES_DIR = "${STAGING_DIR}/modules/"
    private static final String STAGING_WEBAPP_DIR = "${STAGING_DIR}/labkeyWebapp"
    private static final String STAGING_WEBINF_DIR = "${STAGING_WEBAPP_DIR}/WEB-INF/"
    public static final String LABKEY_GROUP = "org.labkey"

    @Override
    void apply(Project project)
    {
        project.extensions.create("labkey", LabKeyExtension)
        project.group = LABKEY_GROUP
        project.subprojects { subproject ->
            buildDir = "${project.rootProject.buildDir}/modules/${subproject.name}"
        }

        project.labkey {
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
    def String webappClassesDir
    def String libDir
    def String jspDir
    def String webInfDir
    def String webappDir
    def String modulesDir
}

// TODO split this into separate extensions for deploy, staging, et al.
class LabKeyExtension
{
    private static final String DEPLOY_MODE_PROPERTY = "deployMode"
    private static enum  DeployMode {

        dev("Development"),
        prod("Production"),
        test("Test")

        private String _displayName;

        private DeployMode(String displayName)
        {
            _displayName = displayName;
        }

        String getDisplayName()
        {
            return _displayName
        }
    }

    def String sourceCompatibility = '1.8'
    def String targetCompatibility = '1.8'
    def Boolean skipBuild = false // set this to true in an individual module's build.gradle file to skip building
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
    def String ext3Dir = "ext-3.4.1"
    def String ext4Dir = "ext-4.2.1"


    public static String getDeployModeName(Project project)
    {
        if (!project.hasProperty(DEPLOY_MODE_PROPERTY))
            return DeployMode.dev.getDisplayName()
        else
            return DeployMode.valueOf(project.property(DEPLOY_MODE_PROPERTY).toString().toLowerCase()).getDisplayName()
    }

    public static boolean isDevMode(Project project)
    {
        return project.hasProperty(DEPLOY_MODE_PROPERTY) && DeployMode.dev.toString().equalsIgnoreCase((String) project.property(DEPLOY_MODE_PROPERTY));
    }
}

