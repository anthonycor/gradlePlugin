package org.labkey.gradle.plugin.extension

import org.gradle.api.Project

/**
 * Created by susanh on 4/23/17.
 */
class DistributionExtension
{
    public static final String DIST_FILE_DIR = "labkeywebapp/WEB-INF/classes"
    public static final String DIST_FILE_NAME = "distribution"
    public static final String VERSION_FILE_NAME = "VERSION"

    String dir = "${project.rootProject.projectDir}/dist"
    String installerSrcDir = "${project.rootProject.projectDir}/server/installer"
    String archiveDataDir = "${installerSrcDir}/archivedata"
    String artifactId
    String description

    private Project project

    DistributionExtension(Project project)
    {
        this.project = project
    }

}
