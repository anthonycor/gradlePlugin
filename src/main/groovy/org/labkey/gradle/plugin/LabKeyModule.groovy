package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileTree
import org.labkey.gradle.task.JspCompile

/**
 * Created by susanh on 4/5/16.
 */
class LabKeyModule implements Plugin<Project>
{
    def String skipBuildFile = "skipBuild.txt" // Deprecated: set the skipBuild property to true in the module's build.gradle file instead
    def String modulePropertiesFile = "module.properties"

    @Override
    void apply(Project project)
    {
        project.apply plugin: 'xmlBeans'
        project.apply plugin:'java-base'

        project.extensions.create("jspCompile", JspCompileExtension)
        def FileTree jspTree = project.fileTree("src").include('**/*.jsp');
        jspTree += project.fileTree("resources").include("**/*.jsp");

        project.task("listJsps") << {
            jspTree.each( { File file ->
                println file
            })
        }

        def Task build = project.task("module")

        build.onlyIf({
            def List<String> indicators = new ArrayList<>();
            if (project.file(skipBuildFile).exists())
                indicators.add(skipBuildFile + " exists")
            if (!project.file(modulePropertiesFile).exists())
                indicators.add(modulePropertiesFile + " does not exist")
            if (project.skipBuild)
                indicators.add("skipBuild property set for Gradle project")

            if (indicators.size() > 0)
            {
                project.logger.info("$project.name build skipped because: " + indicators.join("; "))
            }
            return indicators.isEmpty()
        })

//        def Task jspCompile = project.task('jspCompile',
//                group: "jsp",
//                type: JspCompile,
//                description: "compile jsp files into Java classes",
//                {
//                    inputs.files jspTree
//                    outputs.dir "$project.buildDir/$project.jspCompile.classDir"
//                }
//        )

//
//        def Task jspJar = project.task('jspJar',
//                group: "jsp",
//                type: Jar,
//                description: "produce jar file of jsps", {
//            from project.xmlBeans.classDir
//            exclude '**/*.java'
//            baseName 'schemas'
//            //baseName 'schemas'
//            archiveName 'schemas.jar' // TODO remove this in favor of a versioned jar file when other items have change
//            destinationDir = project.libDir
//        })
//        jspJar.dependsOn(jspCompile)

    }
}

class JspCompileExtension
{
    def String tempDir = "jspTempDir"
    def String extraLibDir = "lib"
    def String classDir = "$tempDir/classes"
}
