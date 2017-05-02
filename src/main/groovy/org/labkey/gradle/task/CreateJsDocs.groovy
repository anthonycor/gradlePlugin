package org.labkey.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
/**
 * Created by susanh on 5/1/17.
 */
class CreateJsDocs extends DefaultTask
{
    @InputFiles
    List<File> getInputFiles()
    {
        List<File> files = []
        project.jsDoc.paths.each{ String path ->
            files += project.file(path)
        }
        return files
    }

    @OutputDirectory
    File getOutputDirectory()
    {
        return project.jsDoc.outputDir
    }

    @TaskAction
    void createDocs()
    {
        List<File> inputPaths = getInputFiles()
        project.javaexec { exec ->
            exec.main = "-jar"
            exec.args = ["${project.jsDoc.root}/jsrun.jar",
                         "${project.jsDoc.root}/app/run.js",
                         "--template=${project.tasks.jsdocTemplate.destinationDir}",
                         "--directory=${getOutputDirectory()}",
                         "--verbose"]
            inputPaths.each { File file ->
                exec.args += file.path
            }
        }
    }
}
