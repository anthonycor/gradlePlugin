package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Defines a set of extension properties for ease of reference.  Other plugins should
 * derive from this class.  This also and adds a labkey extension for some basic properties.
 *
 * Created by susanh on 4/13/16.
 */
class LabKey implements Plugin<Project>
{
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
        project.ext {
            modulesApiDir = "${project.rootProject.buildDir}/modules-api"
            webappLibDir = "${project.rootProject.buildDir}/${STAGING_WEBINF_DIR}/lib"
            webappJspDir = "${project.rootProject.buildDir}/${STAGING_WEBINF_DIR}/jsp"

            explodedModuleDir = "${project.buildDir}/explodedModule"
            explodedModuleWebDir = "${explodedModuleDir}/web"
            libDir = "${explodedModuleDir}/lib"
            srcGenDir = "${project.buildDir}/gensrc"
        }
    }
}

class LabKeyExtension
{
    def LabKey.DeployMode deployMode = LabKey.DeployMode.dev
    def String sourceCompatibility = '1.8'
    def String targetCompatibility = '1.8'
    def Boolean skipBuild = false // set this to true in an individual module's build.gradle file to skip building
}