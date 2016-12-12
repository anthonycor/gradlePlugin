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
    public static final String LABKEY_GROUP = "org.labkey"
    public static final String CLIENT_LIBS_CLASSIFER = "web"
    public static final String SOURCES_CLASSIFIER = "sources"
    public static final String JAVADOC_CLASSIFIER = "javadoc"

    @Override
    void apply(Project project)
    {
        project.group = LABKEY_GROUP
        project.subprojects { subproject ->
            buildDir = "${project.rootProject.buildDir}/modules/${subproject.name}"
        }

        addConfigurations(project)

        LabKeyExtension labKeyExt = project.extensions.create("labkey", LabKeyExtension)
        labKeyExt.setDirectories(project)

        StagingExtension stagingExt = project.extensions.create("staging", StagingExtension)
        stagingExt.setDirectories(project)
    }

    // These configurations are used for deploying the app.  We declare them here
    // because we need them available for all projects to declare their dependencies
    // to these configurations.
    private static void addConfigurations(Project project)
    {
        project.configurations
                {
                    modules
                    jars
                    jspJars
                    external
                }
    }
}

class StagingExtension
{
    public static final String STAGING_DIR = "staging"
    public static final String STAGING_MODULES_DIR = "${STAGING_DIR}/modules/"
    public static final String STAGING_WEBAPP_DIR = "${STAGING_DIR}/labkeyWebapp"
    public static final String STAGING_WEBINF_DIR = "${STAGING_WEBAPP_DIR}/WEB-INF/"

    def String dir
    def String webappClassesDir
    def String libDir
    def String jspDir
    def String webInfDir
    def String webappDir
    def String modulesDir

    public void setDirectories(Project project)
    {
        dir = "${project.rootProject.buildDir}/${STAGING_DIR}"
        webappClassesDir = "${project.rootProject.buildDir}/${STAGING_WEBINF_DIR}/classes"
        libDir = "${project.rootProject.buildDir}/${STAGING_WEBINF_DIR}/lib"
        jspDir = "${project.rootProject.buildDir}/${STAGING_WEBINF_DIR}/jsp"
        webInfDir =  "${project.rootProject.buildDir}/${STAGING_WEBINF_DIR}"
        webappDir =  "${project.rootProject.buildDir}/${STAGING_WEBAPP_DIR}"
        modulesDir = "${project.rootProject.buildDir}/${STAGING_MODULES_DIR}"
    }
}

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

    def String explodedModuleDir
    def String explodedModuleWebDir
    def String explodedModuleConfigDir
    def String explodedModuleLibDir

    def String srcGenDir
    def String externalDir
    def String externalLibDir
    def String ext3Dir = "ext-3.4.1"
    def String ext4Dir = "ext-4.2.1"

    def String server = "http://localhost"
    def String port = "8080"
    def String contextPath = "/labkey"

    /**
     * @param project the project in question
     * @return  true if the project given is one of the modules whose jar file needs to be in the labkeyWebapp/WEB-INF/lib directory
     * at startup
     */
    public static Boolean isBootstrapModule(Project project)
    {
        return [":server:internal", ":server:api", ":schemas", ":remoteapi:java"].contains(project.path)
    }

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

    public void setDirectories(Project project)
    {
        explodedModuleDir = "${project.buildDir}/explodedModule"
        explodedModuleWebDir = "${explodedModuleDir}/web"
        explodedModuleConfigDir = "${explodedModuleDir}/config"
        explodedModuleLibDir = "${explodedModuleDir}/lib"
        srcGenDir = "${project.buildDir}/gensrc"

        externalDir = "${project.rootDir}/external"
        externalLibDir = "${externalDir}/lib"
    }
}

