package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
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
        def libXmlFiles = project.fileTree(dir: project.projectDir, includes: ["**/*${LIB_XML_EXTENSION}"])
        libXmlFiles.files.each() {
            def file ->

                def name = file.getName();
                def task = project.task(
                        'compress' + name.capitalize().substring(0, name.indexOf(LIB_XML_EXTENSION)) + "ClientLibs",
                        group: "Client Libraries",
                        type: ClientLibsCompress,
                        description: "create minified, compressed javascript file using .lib.xml sources",
                        {
                            libXmlFile = file
                        }
                )
                task.dependsOn(project.tasks.processResources)
        }
    }
}

