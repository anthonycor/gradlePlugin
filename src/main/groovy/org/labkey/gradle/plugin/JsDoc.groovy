package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.CopySpec
import org.gradle.api.tasks.Copy
import org.labkey.gradle.plugin.extension.JsDocExtension
import org.labkey.gradle.task.CreateJsDocs
import org.labkey.gradle.util.GroupNames
import org.labkey.gradle.util.PropertiesUtils

import java.util.regex.Matcher
/**
 * Plugin that provides tasks for created JavaScript documentation using jsDoc tools
 */
class JsDoc implements Plugin<Project>
{
    @Override
    void apply(Project project)
    {
        project.extensions.create("jsDoc", JsDocExtension)
        project.jsDoc.root = "${project.rootDir}/tools/jsdoc-toolkit/"
        project.jsDoc.outputDir = new File("${project.rootProject.buildDir}/client-api/javascript/docs")
        addTasks(project)
    }

    void addTasks(Project project)
    {
        Task jsdocTemplateTask = project.task('jsdocTemplate',
                type: Copy,
                description: "insert the proper version number into the JavaScript documentation",
                { CopySpec copy ->
                    copy.from project.file("${project.jsDoc.root}/templates/jsdoc")
                    copy.filter( { String line ->
                        Matcher matcher = PropertiesUtils.PROPERTY_PATTERN.matcher(line);
                        String newLine = line;
                        while (matcher.find())
                        {
                            if (matcher.group(1).equals("product.version"))
                                newLine = newLine.replace(matcher.group(), (String) project.version)
                        }
                        return newLine;

                    })
                    destinationDir = new File((String) "${project.jsDoc.root}/templates/jsdoc_substituted")
                }
        )
        Task jsDocTask = project.task(
                "jsdoc",
                group: GroupNames.DOCUMENTATION,
                type: CreateJsDocs,
                description: 'Generating Client API docs'
        )
        jsDocTask.dependsOn(jsdocTemplateTask)
    }
}

