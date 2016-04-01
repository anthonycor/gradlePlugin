package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.Delete
import org.labkey.gradle.task.SchemaCompile

class XmlBeansPluginExtension
{
    def String schemasDir = "schemas" // the directory containing the schemas to be compiled
    def String classDir = "xb" // the name of the directory in build or build/gensrc for the source and class files
}

/**
 * Class that will convert xsd files into a jar file
 */
class XmlBeansPlugin implements Plugin<Project>
{
    def void apply(Project project)
    {
        project.extensions.create("xmlBeans", XmlBeansPluginExtension)
        def Task schemaCompile = project.task('schemaCompile',
                group: "xmlBeans",
                type: SchemaCompile,
                description: "compile XML schemas from $project.xmlBeans.schemasDir",
                {
                    inputs.dir  project.xmlBeans.schemasDir
                    outputs.dir "$project.buildDir/$project.xmlBeans.classDir"
                }
        )
        schemaCompile.onlyIf {
            project.file(project.xmlBeans.schemasDir).exists()
        }

        def Task schemaJar = project.task('schemaJar',
                group: "xmlBeans",
                type: Jar,
                description: "produce schema jar file from $project.xmlBeans.classDir", {
            from project.xmlBeans.classDir
            exclude '**/*.java'
            baseName 'schemas'
            //baseName 'schemas'
            archiveName 'schemas.jar' // TODO remove this in favor of a versioned jar file when other items have change
            destinationDir = project.libDir
        })
        schemaJar.dependsOn(schemaCompile)
        schemaJar.onlyIf {
            project.file(project.xmlBeans.schemasDir).exists()
        }

        project.task("cleanSchemaJar",
                group: "xmlBeans",
                type: Delete,
                description: "remove schema jar file", {
                    delete "$schemaJar.destinationDir/$schemaJar.archiveName"
                }
        )

        project.task("cleanSchemaCompile",
            group: "xmlBeans",
            type: Delete,
            description: "remove source and class files generated from xsd files", {
                delete "$project.buildDir/$project.xmlBeans.classDir",
                        "$project.srcGenDir/$project.xmlBeans.classDir"
        })
    }
}

