package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.labkey.gradle.task.ClientLibsCompress

class ClientLibraries implements Plugin<Project>
{
    @Override
    void apply(Project project)
    {
        project.extensions.create("clientLibs", ClientLibrariesExtension)
        project.clientLibs.libXmlParentDirectory = new File("${project.labkey.explodedModuleDir}/web")
        project.clientLibs.outputDir = new File("${project.labkey.explodedModuleDir}/web")
        addTasks(project)
    }

    private void addTasks(Project project)
    {
        def compressLibsTask = project.task("compressClientLibs",
                group: 'Client libraries',
                type: ClientLibsCompress,
                description: 'create minified, compressed javascript file use .lib.xml sources',
                dependsOn: project.tasks.processResources,
        )
        project.tasks.assemble.dependsOn(compressLibsTask)
    }
}

class ClientLibrariesExtension
{
    // the name of the directory, relative to the project directory, that contains the path to the *.lib.xml file that is to be compressed
    def File libXmlParentDirectory
    // the directory into which the catenated, compressed js and css files will be created; also (confusingly) the directory in which
    // the .lib.xml file will have been copied before conversion.  This copying happens before this task in the processResources task.
    def File outputDir
}

