package org.labkey.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.file.RelativePath
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class StageDistribution extends DefaultTask
{
    File distributionFile = null

    @OutputDirectory
    File modulesStagingDir = new File((String) project.staging.modulesDir)

    @OutputDirectory
    File stagingDir = new File((String) project.staging.dir)

    @OutputDirectory
    File pipelineJarStagingDir = new File((String) project.staging.pipelineLibDir)

    @OutputDirectory
    File tomcatJarStagingDir = new File((String) project.staging.tomcatLibDir)


    @TaskAction
    void action()
    {
        File distDir = project.hasProperty("distDir") ? new File((String) project.property("distDir")) : new File("${project.rootProject.buildDir}/dist")
        if (!distDir.exists())
            throw new GradleException("Distribution directory ${distDir} not found")
        String extension = project.hasProperty("distType") ? project.property('distType') : "tar.gz"
        File[] distFiles = distDir.listFiles(new FilenameFilter() {

            @Override
            boolean accept(File dir, String name) {
                return name.endsWith(".${extension}");
            }
        })
        if (distFiles == null || distFiles.length == 0)
            throw new GradleException("No distribution found in directory ${distDir} with extension ${extension}")
        else if (distFiles.length > 1)
            throw new GradleException("${distDir} contains ${distFiles.length} files with extension ${extension}.  Only one is allowed.")
        distributionFile = distFiles[0]

        Boolean isTar = extension.equals("tar.gz")

        project.copy({ CopySpec spec ->
            spec.from isTar ? project.tarTree(distributionFile).files : project.zipTree(distributionFile).files
            spec.into modulesStagingDir
            spec.include "**/*.module"
        })

        String baseName = distributionFile.getName().substring(0, distributionFile.getName().length()-extension.length())

        project.copy({ CopySpec spec ->
            spec.from isTar ? project.tarTree(distributionFile) : project.zipTree(distributionFile)
            spec.into stagingDir
            spec.eachFile {
                FileCopyDetails fcp ->
                if (fcp.relativePath.pathString.startsWith("${baseName}/labkeywebapp")) {
                    // remap the file to the root
                    String[] segments = fcp.relativePath.segments
                    String[] pathSegments = segments[1..-1] as String[]
                    fcp.relativePath = new RelativePath(!fcp.file.isDirectory(), pathSegments)
                } else {
                    fcp.exclude()
                }
            }
            spec.includeEmptyDirs = false
        })

        project.copy({ CopySpec spec ->
            spec.from isTar ? project.tarTree(distributionFile) : project.zipTree(distributionFile)
            spec.into pipelineJarStagingDir
            spec.eachFile {
                FileCopyDetails fcp ->
                if (fcp.relativePath.pathString.startsWith("${baseName}/pipeline-lib")) {
                    // remap the file to the root
                    String[] segments = fcp.relativePath.segments
                    String[] pathSegments = segments[2..-1] as String[]
                    fcp.relativePath = new RelativePath(!fcp.file.isDirectory(), pathSegments)
                } else {
                    fcp.exclude()
                }
            }
            spec.includeEmptyDirs = false
        })

        project.copy({ CopySpec spec ->
            spec.from isTar ? project.tarTree(distributionFile).files : project.zipTree(distributionFile).files
            spec.into tomcatJarStagingDir
            spec.include "labkeyBootstrap*.jar"
        })
    }
}
