package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.labkey.gradle.task.ClientLibsCompress
import org.labkey.gradle.util.GroupNames

/**
 * Creates minified, compressed javascript files using the script declarations in a modules .lib.xml file(s).
 */
class ClientLibraries implements Plugin<Project>
{
    public static boolean isApplicable(Project project)
    {
        def FileTree libXmlFiles = project.fileTree(dir: project.projectDir,
                includes: ["**/*${ClientLibsCompress.LIB_XML_EXTENSION}"]
        )
        return !libXmlFiles.isEmpty()
    }

    @Override
    void apply(Project project)
    {
        project.extensions.create("clientLibs", ClientLibrariesExtension)
        project.clientLibs.workingDir = new File((String) project.labkey.explodedModuleWebDir)
        addTasks(project)
    }

    private void addTasks(Project project)
    {
        def compressLibsTask = project.task("compressClientLibs",
                group: GroupNames.CLIENT_LIBRARIES,
                type: ClientLibsCompress,
                description: 'create minified, compressed javascript file using .lib.xml sources',
                dependsOn: project.tasks.processResources,
        )
        project.tasks.assemble.dependsOn(compressLibsTask)
    }
}

class ClientLibrariesExtension
{
    // the directory into which the catenated, compressed js and css files will be created; also (confusingly) the directory in which
    // the .lib.xml file will have been copied before conversion.  This copying happens before this task in the processResources task.
    def File workingDir
}

