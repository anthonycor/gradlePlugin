package org.labkey.gradle.plugin.extension

import org.gradle.api.Project

/**
 * Created by susanh on 4/23/17.
 */
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
        webInfDir = "${project.rootProject.buildDir}/${STAGING_WEBINF_DIR}"
        webappDir = "${project.rootProject.buildDir}/${STAGING_WEBAPP_DIR}"
        modulesDir = "${project.rootProject.buildDir}/${STAGING_MODULES_DIR}"
        tomcatLibDir = "${dir}/tomcat-lib"
        pipelineLibDir = "${dir}/pipelineLib"
    }
}
