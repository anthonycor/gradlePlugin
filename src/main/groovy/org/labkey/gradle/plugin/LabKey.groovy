package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.labkey.gradle.util.ModuleFinder
/**
 * Defines a set of extension properties for ease of reference. This also adds a two extensions
 * for some basic properties.
 * Created by susanh on 4/13/16.
 */
class LabKey implements Plugin<Project>
{
    public static final String LABKEY_GROUP = "org.labkey"
    public static final String CLIENT_LIBS_CLASSIFER = "web"
    public static final String SOURCES_CLASSIFIER = "sources"
    public static final String JAVADOC_CLASSIFIER = "javadoc"
    public static final String FAT_JAR_CLASSIFIER = "all"

    @Override
    void apply(Project project)
    {
        project.group = LABKEY_GROUP
        project.subprojects { Project subproject ->
            if (ModuleFinder.isDistributionProject(subproject))
                subproject.buildDir = "${project.rootProject.buildDir}/installer/${subproject.name}"
            else
                subproject.buildDir = "${project.rootProject.buildDir}/modules/${subproject.name}"

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
                    jspJars { transitive = false }
                    // we don't want this to be transitive because we use this configuration to
                    // clean out the tomcat/lib directory when we do a cleanDeploy and the transitive
                    // dependencies include some of the jars that are native to tomcat.
                    tomcatJars { transitive = false }
                    remotePipelineJars
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

    String dir
    String webappClassesDir
    String libDir
    String jspDir
    String webInfDir
    String webappDir
    String modulesDir
    String tomcatLibDir
    String pipelineLibDir

    void setDirectories(Project project)
    {
        dir = "${project.rootProject.buildDir}/${STAGING_DIR}"
        webappClassesDir = "${project.rootProject.buildDir}/${STAGING_WEBINF_DIR}/classes"
        libDir = "${project.rootProject.buildDir}/${STAGING_WEBINF_DIR}/lib"
        jspDir = "${project.rootProject.buildDir}/${STAGING_WEBINF_DIR}/jsp"
        webInfDir =  "${project.rootProject.buildDir}/${STAGING_WEBINF_DIR}"
        webappDir =  "${project.rootProject.buildDir}/${STAGING_WEBAPP_DIR}"
        modulesDir = "${project.rootProject.buildDir}/${STAGING_MODULES_DIR}"
        tomcatLibDir = "${dir}/tomcat-lib"
        pipelineLibDir = "${dir}/pipelineLib"
    }
}

class LabKeyExtension
{
    private static final String DEPLOY_MODE_PROPERTY = "deployMode"
    private static enum  DeployMode {

        dev("Development"),
        prod("Production")

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

    Boolean skipBuild = false // set this to true in an individual module's build.gradle file to skip building

    String explodedModuleDir
    String explodedModuleWebDir
    String explodedModuleConfigDir
    String explodedModuleLibDir

    String srcGenDir
    String externalDir
    String externalLibDir
    String ext3Dir = "ext-3.4.1"
    String ext4Dir = "ext-4.2.1"

    String server = "http://localhost"
    String port = "8080"
    String contextPath = "/labkey"

    /**
     * @param project the project in question
     * @return  true if the project given is one of the modules whose jar file needs to be in the labkeyWebapp/WEB-INF/lib directory
     * at startup
     */
    static Boolean isBootstrapModule(Project project)
    {
        return [":server:internal", ":server:api", ":schemas", ":remoteapi:java"].contains(project.path)
    }

    static String getDeployModeName(Project project)
    {
        if (!project.hasProperty(DEPLOY_MODE_PROPERTY))
            return DeployMode.dev.getDisplayName()
        else
            return DeployMode.valueOf(project.property(DEPLOY_MODE_PROPERTY).toString().toLowerCase()).getDisplayName()
    }

    static boolean isDevMode(Project project)
    {
        return project.hasProperty(DEPLOY_MODE_PROPERTY) && DeployMode.dev.toString().equalsIgnoreCase((String) project.property(DEPLOY_MODE_PROPERTY))
    }

    void setDirectories(Project project)
    {
        explodedModuleDir = "${project.buildDir}/explodedModule"
        explodedModuleWebDir = "${explodedModuleDir}/web"
        explodedModuleConfigDir = "${explodedModuleDir}/config"
        explodedModuleLibDir = "${explodedModuleDir}/lib"
        srcGenDir = "${project.buildDir}/gensrc"

        externalDir = "${project.rootDir}/external"
        externalLibDir = "${externalDir}/lib"
    }

    static Properties getBasePomProperties(String artifactPrefix, String description)
    {
        Properties pomProperties = new Properties()
        pomProperties.put("ArtifactId", artifactPrefix)
        pomProperties.put("Organization", "LabKey")
        pomProperties.put("OrganizationURL", "http://www.labkey.org")
        if (description != null)
            pomProperties.put("Description", description )
        pomProperties.put("License", "The Apache Software License, Version 2.0")
        pomProperties.put("LicenseURL", "http://www.apache.org/licenses/LICENSE-2.0.txt")
        return pomProperties
    }
}

