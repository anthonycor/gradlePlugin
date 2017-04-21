package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileTree
import org.labkey.gradle.task.ClientLibsCompress
import org.labkey.gradle.util.GroupNames

/**
 * Creates minified, compressed javascript files using the script declarations in a modules .lib.xml file(s).
 */
class ClientLibraries implements Plugin<Project>
{
    static boolean isApplicable(Project project)
    {
        FileTree libXmlFiles = project.fileTree(dir: project.projectDir,
                includes: ["**/*${ClientLibsCompress.LIB_XML_EXTENSION}"]
        )
        return !libXmlFiles.isEmpty()
    }

    @Override
    void apply(Project project)
    {
        addTasks(project)
    }

    private static void addTasks(Project project)
    {
        Task compressLibsTask = project.task("compressClientLibs",
                group: GroupNames.CLIENT_LIBRARIES,
                type: ClientLibsCompress,
                description: 'create minified, compressed javascript file using .lib.xml sources',
                dependsOn: project.tasks.processResources
        )
        project.tasks.assemble.dependsOn(compressLibsTask)
    }
}

