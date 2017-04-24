package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.labkey.gradle.plugin.extension.LabKeyExtension
import org.labkey.gradle.plugin.extension.StagingExtension
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
                    // we don't want this to be transitive because we use this configuration to
                    // clean out the tomcat/lib directory when we do a cleanDeploy and the transitive
                    // dependencies include some of the jars that are native to tomcat.
                    tomcatJars { transitive = false }
                    remotePipelineJars
                    external
                }
    }
}



