/*
 * Copyright (c) 2016-2017 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
                includes: ["**/*${ClientLibsCompress.LIB_XML_EXTENSION}"],
                excludes: ["node_modules"]
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

