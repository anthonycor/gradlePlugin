package org.labkey.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Jar
import org.labkey.gradle.task.JspCompile2Java

/**
 * Created by susanh on 4/11/16.
 */
class Jsp extends LabKey
{
    @Override
    void apply(Project project)
    {
        project.apply plugin: 'java-base'
        project.extensions.create("jspCompile", JspCompileExtension)

        addConfiguration(project)
        addDependencies(project)
        addSourceSet(project)
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
                    jsp
                }
        project.configurations.getByName('jspCompile') {
            resolutionStrategy {
                force 'javax.servlet:servlet-api:3.1' // the version number here is sort of irrelevant until we use a repository other than the file system; it just needs to be something other than the old 2.4 version in external/libs/build
            }
        }
    }

    private void addDependencies(Project project)
    {
        project.dependencies
                {
                    // TODO there may be a need to add other items to the classpath here to get rid of these warnings:
//                    The XML schema [web-app_3_1.xsd] could not be found. This is very likely to break XML validation if XML validation is enabled.
//                    The XML schema [web-fragment_3_1.xsd] could not be found. This is very likely to break XML validation if XML validation is enabled.
//                    The XML schema [web-common_3_1.xsd] could not be found. This is very likely to break XML validation if XML validation is enabled.
//                    The XML schema [javaee_7.xsd] could not be found. This is very likely to break XML validation if XML validation is enabled.
//                    The XML schema [jsp_2_3.xsd] could not be found. This is very likely to break XML validation if XML validation is enabled.
//                    The XML schema [javaee_web_services_1_4.xsd] could not be found. This is very likely to break XML validation if XML validation is enabled.
//                    The XML schema [javaee_web_services_client_1_4.xsd] could not be found. This is very likely to break XML validation if XML validation is enabled.
                        jspCompile  'org.apache.tomcat:jasper',
                        'org.apache.tomcat:jsp-api',
                        'javax.servlet:servlet-api:3.1',
                        'org.apache.tomcat:tomcat-juli'
                    jspCompile project.fileTree(dir: "${project.tomcatDir}/lib", includes: ['*.jar'], excludes: ['servlet-api.jar'])
                    jspCompile project.project(":server:api")
                    jspCompile project.project(":server:internal")
                    jspCompile project.files("${project.labkey.explodedModuleDir}/lib/${project.name}.jar") {
                        builtBy 'jar'
                    }
                    if (project.hasProperty('apiJar'))
                        jspCompile project.files("${project.labkey.explodedModuleDir}/lib/${project.name}_api.jar") {
                            builtBy 'apiJar'
                        }

                    jsp     'org.apache.tomcat:jasper',
                            'org.apache.tomcat:bootstrap',
                            'org.apache.tomcat:tomcat-juli'
                }
    }

    private void addJspTasks(Project project)
    {
        def FileTree jspTree = project.fileTree("src").include('**/*.jsp');
        jspTree += project.fileTree("resources").include("**/*.jsp");

        def Task listJsps = project.task('listJsps', group: "jsp")
        listJsps.doLast {
                    jspTree.each ({
                        println it.absolutePath
                    })
                }

        def Task copyJsps = project.task('copyJsp', group: "jsp", type: Copy, description: "Copy jsp files to jsp compile directory",
                {
                    from 'src'
                    into "${project.buildDir}/${project.jspCompile.tempDir}/webapp"
                    include '**/*.jsp'

                    from 'resources'
                    into "${project.buildDir}/${project.jspCompile.tempDir}/webapp/org/labkey/${project.name}"
                    include '**/*.jsp'

                })

        def Task copyTags = project.task('copyTagLibs', group: "jsp", type: Copy, description: "Copy the tag library (.tld) files to jsp compile directory",
                {
                    from project.labkey.stagingWebInfDir
                    into "${project.buildDir}/${project.jspCompile.tempDir}/webapp/WEB-INF"
                    include 'web.xml'
                    include '*.tld'
                    include 'tags/**'
                })

        def Task jspCompileTask = project.task('jsp2Java',
                group: "jsp",
                type: JspCompile2Java,
                description: "compile jsp files into Java classes",
                {
                    inputs.file copyJsps
                    inputs.file copyTags
                    outputs.dir "${project.buildDir}/${project.jspCompile.classDir}"
                }
        )
        if (project.hasProperty('apiJar'))
            jspCompileTask.dependsOn('apiJar')
        jspCompileTask.dependsOn('jar')

        project.tasks.compileJspJava {
            dependsOn jspCompileTask
        }

        def Task jspJar = project.task('jspJar',
                group: "jsp",
                type: Jar,
                description: "produce jar file of jsps", {
            from "${project.buildDir}/${project.jspCompile.classDir}"
            //baseName "${project.name}_jsp"
            archiveName "${project.name}_jsp.jar" // TODO remove this in favor of a versioned jar file when other items have change
            destinationDir = project.file(project.labkey.libDir)
        })

        jspJar.dependsOn(project.tasks.compileJspJava)

        project.artifacts {
            jspCompile jspJar
        }
        project.tasks.assemble.dependsOn(jspJar)
    }
}

class JspCompileExtension
{
    def String tempDir = "jspTempDir"
    def String classDir = "$tempDir/classes"
}
