package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.bundling.Jar
import org.labkey.gradle.task.JspCompile2Java

/**
 * Created by susanh on 4/11/16.
 */
class Jsp implements Plugin<Project>
{
    @Override
    void apply(Project project)
    {
        project.apply plugin: 'java-base'
        project.extensions.create("jspCompile", JspCompileExtension)

        addDependencies(project)
        addSourceSet(project)
        addConfiguration(project)
        addDependencies(project)
        addJspTasks(project)

    }

    private void addSourceSet(Project project)
    {
        project.sourceSets
                {
                    jsp {
                        java {
                            srcDirs = ["${project.buildDir}/${project.jspCompile.tempDir}/classes"]
                        }
                        output.classesDir = "${project.buildDir}/${project.jspCompile.tempDir}/classes"
                    }
                }
    }

    private void addConfiguration(Project project)
    {
        project.configurations
                {
                    jspCompile
                }
    }

    private void addDependencies(Project project)
    {
        project.configurations
                {
                    jspCompile
                    jsp
                }
        project.dependencies
                {
                    jspCompile  'org.apache.tomcat:jasper', // TODO check for proper group designation
                        'org.apache.tomcat:jsp-api',
                        'javax.servlet:servlet-api',
                        'org.apache.tomcat:tomcat-juli',
                        'org.apache.tomcat:tomcat-api',
                        'org.apache.tomcat:tomcat-util-scan',
                        'org.apache.tomcat:tomcat-util',
                        'org.apache.tomcat:el-api',
                        'org.apache:jasper-el',
                        'org.labkey:api',
                        "org.labkey:${project.name}-api"
                    jsp     'org.apache.tomcat:jasper',
                            'org.apaache.tomcat:bootstrap',
                            'org.apache.tomcat:tomcat-juli'
                }
    }

    private void addJspTasks(Project project)
    {
        def FileTree jspTree = project.fileTree("src").include('**/*.jsp');
        jspTree += project.fileTree("resources").include("**/*.jsp");

//        project.task("listJsps") << {
//            jspTree.each({ File file ->
//                println file
//            })
//        }

        def Task jspCompileTask = project.task('jsp2Java',
                group: "jsp",
                type: JspCompile2Java,
                description: "compile jsp files into Java classes",
                {
                    inputs.files jspTree
                    outputs.dir "${project.buildDir}/${project.jspCompile.classDir}"
                }
        )

        project.tasks.compileJspJava {
            dependsOn jspCompileTask
        }

        def Task jspJar = project.task('jspJar',
                group: "jsp",
                type: Jar,
                description: "produce jar file of jsps", {
            from "${project.buildDir}/${project.jspCompile.classDir}"
            exclude '**/*.java'
            //baseName "${project.name}_jsp"
            archiveName "${project.name}_jsp.jar" // TODO remove this in favor of a versioned jar file when other items have change
            destinationDir = project.libDir
        })
        jspJar.dependsOn(project.tasks.compileJspJava)

        project.artifacts {
            jspCompile jspJar
        }
    }
}

class JspCompileExtension
{
    def String tempDir = "jspTempDir"
    def String extraLibDir = "lib"
    def String classDir = "$tempDir/classes"
}
