package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.Delete
import org.labkey.gradle.task.SchemaCompile

class XmlBeansPluginExtension
{
    def String schemasDir = "schemas"
    def String classDir = "xb"
    def String srcGenDir = "gensrc/xb"
}

/**
 * Class that will convert xsd files into a jar file
 */
class XmlBeansPlugin implements Plugin<Project>
{
    def void apply(Project project)
    {
        project.extensions.create("xmlBean", XmlBeansPluginExtension)
        def Task schemaCompile = project.task('schemaCompile',
                group: "xml",
                type: SchemaCompile,
                description: "compile XML schemas from $project.xmlBean.schemasDir",
                {
                    inputs.dir  project.xmlBean.schemasDir
                    outputs.dir project.xmlBean.classDir
                }
        )
        schemaCompile.onlyIf {
            project.file(project.xmlBean.schemasDir).exists()
        }

        def Task schemaJar = project.task('schemaJar',
                group: "xml",
                type: Jar,
                description: "produce schema jar file from $project.xmlBean.classDir", {
            from project.xmlBean.classDir
            exclude '**/*.java'
            baseName 'schemas'
            //baseName 'schemas'
            archiveName 'schemas.jar' // TODO remove this in favor of a versioned jar file when other items have change
            destinationDir = project.libDir
        })
        schemaJar.dependsOn(schemaCompile)
        schemaJar.onlyIf {
            project.file(project.xmlBean.schemasDir).exists()
        }

        def Task cleanSchemaJar = project.task('cleanSchemaJar',
                group: "xml",
                description: "Clean files generated from compiling .xsd files",
                type: Delete,
                {
                    delete project.xmlBean.classDir, project.xmlBean.srcGenDir
                }
        )
//       cleanSchemaJar.delete(project.xmlBean.classDir, project.xmlBean.srcGenDir)

    }
}

