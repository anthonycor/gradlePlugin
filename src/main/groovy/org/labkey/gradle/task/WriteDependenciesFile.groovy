package org.labkey.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class WriteDependenciesFile extends DefaultTask
{
    // we assume that if a version number has changed, we should generate a new dependencies file
    @InputFile
    File globalProperties = project.rootProject.file("gradle.properties")

    @OutputFile
    File dependenciesFile = project.file("resources/credits/dependencies.txt")

    public WriteDependenciesFile()
    {
        if (project.file("build.gradle").exists())
        {
            this.inputs.file(project.file("build.gradle"))
        }
        if (project.file("gradle.properties").exists())
        {
            this.inputs.file(project.file("gradle.properties"))
        }
        // add a configuration that has the external dependencies but is not transitive
        project.configurations {
            externalsNotTrans.extendsFrom(project.configurations.external)
            externalsNotTrans {
                transitive = false
            }
        }
        onlyIf {
            !project.configurations.externalsNotTrans.isEmpty()
        }
    }

    @TaskAction
    public void writeFile()
    {
        FileOutputStream outputStream = null;
        try
        {
            outputStream = new FileOutputStream(dependenciesFile)
            outputStream.write("# direct external dependencies for project ${project.path}\n".getBytes())

            project.configurations.externalsNotTrans.each { File file ->
                outputStream.write((file.getName() + "\n").getBytes());
            }
        }
        finally
        {
            outputStream.close()
        }
    }
}
