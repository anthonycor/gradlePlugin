package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.bundling.Jar
import org.labkey.gradle.task.SchemaCompile

class XmlBeansExtension
{
    def String schemasDir = "schemas" // the directory containing the schemas to be compiled
    def String classDir = "xb" // the name of the directory in build or build/gensrc for the source and class files
}

/**
 * Class that will convert xsd files into a jar file
 */
class XmlBeans implements Plugin<Project>
{
    def void apply(Project project)
    {
        project.extensions.create("xmlBeans", XmlBeansExtension)
        addDependencies(project)
        addTasks(project)
    }

    private void addDependencies(Project project)
    {
        project.configurations
                {
                    xmlbeans
                }
        project.dependencies
                {
                    xmlbeans 'org.apache.xmlbeans:xbean:2.5.0'
                }
    }

    private void addTasks(Project project)
    {
        def Task schemasCompile = project.task('schemasCompile',
                group: "xmlSchema",
                type: SchemaCompile,
                description: "compile XML schemas from directory '$project.xmlBeans.schemasDir' into Java classes",
                {
                    inputs.dir  project.xmlBeans.schemasDir
                    outputs.dir "$project.labkey.srcGenDir/$project.xmlBeans.classDir"
                }
        )
        schemasCompile.onlyIf {
            project.file(project.xmlBeans.schemasDir).exists()
        }

        def Task schemasJar = project.task('schemasJar',
                group: "xmlSchema",
                type: Jar,
                description: "produce schemas jar file from directory '$project.xmlBeans.classDir'",
                {
                    from "$project.buildDir/$project.xmlBeans.classDir"
                    exclude '**/*.java'
                    baseName project.name.equals("schemas") ? "schemas": "${project.name}_schemas"
                    version project.version
                    group project.group
                    destinationDir = project.file(project.labkey.libDir)
                }
        )
        schemasJar.dependsOn(schemasCompile)
        schemasJar.onlyIf
                {
                    project.file(project.xmlBeans.schemasDir).exists()
                }

        project.task("cleanSchemasJar",
                group: "xmlSchema",
                type: Delete,
                description: "remove schema jar file",
                {
                    delete "$schemasJar.destinationDir/$schemasJar.archiveName"
                }
        )

        project.task("cleanSchemasCompile",
                group: "xmlSchema",
                type: Delete,
                description: "remove source and class files generated from xsd files",
                {
                    delete "$project.buildDir/$project.xmlBeans.classDir",
                    "$project.labkey.srcGenDir/$project.xmlBeans.classDir"
                }
        )
    }
}

