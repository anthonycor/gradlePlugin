package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileTree
import org.labkey.gradle.task.ClientLibsCompress

class ClientLibraries implements Plugin<Project>
{
    private static final String LIB_XML_EXTENSION = ".lib.xml"
    @Override
    void apply(Project project)
    {
        addTasks(project)
    }

    private void addTasks(Project project)
    {
        def FileTree libXmlFiles = project.fileTree(dir: project.projectDir, includes: ["**/*${LIB_XML_EXTENSION}"])
        def List<Task> libXmlTasks = new ArrayList<>(libXmlFiles.size());
        libXmlFiles.files.each() {
            def file ->

                def name = file.getName();
                def task = project.task(
                        'compress' + name.capitalize().substring(0, name.indexOf(LIB_XML_EXTENSION)) + "ClientLibs",
                        group: "Client Libraries",
                        type: ClientLibsCompress,
                        description: "create minified, compressed javascript file from ${file.getName()}",
                        {
                            libXmlFile = file
                        }
                )
                libXmlTasks.add(task)
        }
        def compressLibsTask = project.task("compressClientLibs",
                group: 'Client Libraries',
                description: 'create minified, compressed javascript file use .lib.xml sources',
                dependsOn: libXmlTasks
        )
        compressLibsTask.dependsOn(project.tasks.processResources);
        project.tasks.assemble.dependsOn(compressLibsTask)
    }
}

