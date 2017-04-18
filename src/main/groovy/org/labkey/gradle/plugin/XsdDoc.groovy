package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.labkey.gradle.task.CreateXsdDocs
import org.labkey.gradle.util.GroupNames
/**
 * Created by susanh on 10/30/16.
 */
class XsdDoc implements Plugin<Project>
{
    @Override
    void apply(Project project)
    {
        project.extensions.create("xsdDoc", XsdDocExtension)
        addConfiguration(project)
        addTasks(project)
    }

    private void addConfiguration(Project project)
    {
        project.configurations {
            xsdDoc
        }
    }

    private void addTasks(Project project)
    {
       project.task(
                "xsddoc",
                group: GroupNames.DOCUMENTATION,
                type: CreateXsdDocs,
                description: 'Generating documentation for classes generated from XSD files'
        )
    }
}

class XsdDocExtension
{
    File[] xsdFiles = []
}
