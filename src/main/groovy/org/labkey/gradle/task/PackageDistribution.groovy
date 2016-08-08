package org.labkey.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.TaskAction
/**
 * Created by susanh on 8/5/16.
 */
class PackageDistribution extends DefaultTask
{
    public static final String DIST_PROPERTY_NAME = "dist"
    public static final String ALL_DISTRIBUTIONS = "all"

    def String distNames;

    private Set<File> distFiles;

    @TaskAction
    public void action()
    {
        setUpModuleDistDirectories()
        gatherModules()
        packageRedistributables()
    }

    private void gatherModules()
    {
        ant.copy (
                toDir: project.dist.distModulesDir
        )
                {
                    project.configurations.distribution.each {
                        File moduleFile ->
                            file(name: moduleFile.getPath())
                    }
                }
    }

    private void setAntPropertiesForInstaller()
    {
        ant.properties['dist_sub.dir'] = project.dist.subDirName
        ant.properties['extraFileIdentifier'] = project.dist.extraFileIdentifier
        ant.properties['project.root'] = project.rootDir
        // TODO other properties to set here
    }
    private void packageRedistributables()
    {
        setAntPropertiesForInstaller()
        ant.ant(antFile: "${project.project(':server').projectDir}/installer/build.xml", target: "build")
    }

    private void packageSource()
    {
        setAntPropertiesForInstaller()
        ant.ant(antFile: "${project.project(':server').projectDir}/installer/build.xml", target: "archive_source")

    }

    private void unpackDist()
    {
        setAntPropertiesForInstaller()
        ant.ant(antFile: "${project.project(':server').projectDir}/installer/build.xml", target: "bin-untar")
    }

    private void findDistributionFiles()
    {
        if (distNames == null)
            return

        // TODO for "distAll" task, find all projects with the Distribution plugin applied.
        if (distName.equals(ALL_DISTRIBUTIONS))
        {
            FileTree fileTree = project.rootProject.fileTree(dir: project.rootProject.projectDir, includes: ["**/distributions/*.xml"])
            distFiles = fileTree.files
        }
        else
        {
            distFiles = new HashSet<>()
            for (String distName : distNames.split(","))
            {
                FileTree tree = project.rootProject.fileTree(dir: project.rootProject.projectDir, includes: ["**/distributions/${distName}.xml"])
                if (tree.files.isEmpty())
                    project.logger.error("Could not find distribution ${distName}")
                distFiles.addAll(tree.files)
            }
        }

    }

    private void setUpModuleDistDirectories()
    {
        File distDir = new File((String) project.dist.distModulesDir)
        distDir.deleteDir();
        distDir.mkdirs();
    }

    // This will "work", but it still uses the ant tasks dependencies
    private void doAntDistAll()
    {
        ant.properties.name = project.hasProperty(PackageDistribution.DIST_PROPERTY_NAME) ? project.property(PackageDistribution.DIST_PROPERTY_NAME) : PackageDistribution.ALL_DISTRIBUTIONS
        ant.ant(
                antfile: "build.xml",
                target: "dist_all"
        )
    }
}
