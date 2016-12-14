package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.specs.AndSpec
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.JavaExec
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
        project.jsDoc.outputDir = "${project.rootProject.buildDir}/client-api/javascript/docs"
        addTasks(project)
    }

    void addTasks(Project project)
    {
        Task jsdocTemplateTask = project.task('jsdocTemplate',
                type: Copy,
                description: "insert the proper version number into the JavaScript documentation",
                {
                    from project.file("${project.jsDoc.root}/templates/jsdoc")
                    filter( { String line ->
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
        project.task(
                "jsdoc",
                group: GroupNames.DOCUMENTATION,
                type: JavaExec,
                description: 'Generating Client API docs',
                {
                    inputs.files project.jsDoc.paths
                    outputs.dir project.jsDoc.outputDir

                    // Workaround for incremental build (GRADLE-1483)
                    outputs.upToDateSpec = new AndSpec()

                    main = "-jar"
                    // FIXME not sure why the project.jsDoc.paths can't be included progammatically here.
                    args = ["${project.jsDoc.root}/jsrun.jar",
                            "${project.jsDoc.root}/app/run.js",
                            "--template=${jsdocTemplateTask.destinationDir}",
                            "--directory=${project.jsDoc.outputDir}",
                            "--verbose",
                            "api/webapp/clientapi",
                            "api/webapp/clientapi/dom",
                            "api/webapp/clientapi/core",
                            "api/webapp/clientapi/ext3",
                            "api/webapp/clientapi/ext4",
                            "api/webapp/clientapi/ext4/data",
                            "internal/webapp/labkey.js",
                            "modules/visualization/resources/web/vis/genericChart/genericChartHelper.js",
                            "modules/visualization/resources/web/vis/timeChart/timeChartHelper.js",
                            "internal/webapp/vis/src"]
                    dependsOn(jsdocTemplateTask)
                }
        )
    }
}

class JsDocExtension
{
    String root
    String[] paths =[ "api/webapp/clientapi",
                          "api/webapp/clientapi/dom",
                          "api/webapp/clientapi/core",
                          "api/webapp/clientapi/ext3",
                          "api/webapp/clientapi/ext4",
                          "api/webapp/clientapi/ext4/data",
                          "internal/webapp/labkey.js",
                          "modules/visualization/resources/web/vis/genericChart/genericChartHelper.js",
                          "modules/visualization/resources/web/vis/timeChart/timeChartHelper.js",
                          "internal/webapp/vis/src"]
    String outputDir
}
