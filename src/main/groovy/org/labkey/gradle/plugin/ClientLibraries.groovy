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
        project.extensions.create("clientLibs", ClientLibrariesExtension)
        addTasks(project)
    }

    private void addTasks(Project project)
    {
        if (project.clientLibs.libXmlParentDirectory == null)
            return;

        def FileTree libXmlFiles = project.fileTree(dir: project.projectDir,
                includes: ["${project.clientLibs.libXmlParentDirectory}/**/*${LIB_XML_EXTENSION}"]
        )
        def List<Task> libXmlTasks = new ArrayList<>(libXmlFiles.size());
        libXmlFiles.files.each() {
            def file ->
                def name = file.getName();
                def task = project.task(
                        'compress' + name.capitalize().substring(0, name.indexOf(LIB_XML_EXTENSION)) + 'ClientLibs',
                        group: "Client Libraries",
                        type: ClientLibsCompress,
                        description: "create minified, compressed javascript file from ${file.getName()}",
                        {
                            libXmlFile = file
                        }
                )
                libXmlTasks.add(task)
                task.dependsOn(project.tasks.processResources)
        }
        def compressLibsTask = project.task("compressClientLibs",
                group: 'Client Libraries',
                description: 'create minified, compressed javascript file use .lib.xml sources',
                dependsOn: libXmlTasks
        )
        project.tasks.assemble.dependsOn(compressLibsTask)
    }
}

class ClientLibrariesExtension
{
    // the name of the directory, relative to the project directory, that contains the path to the *.lib.xml file that is to be compressed
    def String libXmlParentDirectory = "resources/web"
}

