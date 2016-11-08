package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Jar
import org.labkey.gradle.task.JspCompile2Java
import org.labkey.gradle.util.BuildUtils
import org.labkey.gradle.util.GroupNames
/**
 * Used to generate the jsp jar file for a module.
 */
class Jsp implements Plugin<Project>
{
    public static final String CLASSIFIER = "jsp"
    public static final String BASE_NAME_EXTENSION = "_jsp"

    public static boolean isApplicable(Project project)
    {
        return !getJspFileTree(project).isEmpty()
    }

    private static FileTree getJspFileTree(Project project)
    {
        def FileTree jspTree = project.fileTree("src").matching
                {
                    include('**/*.jsp')
                }
        jspTree += project.fileTree("resources").matching
                {
                    include("**/*.jsp")
                }
        return jspTree;
    }

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
                force "javax.servlet:servlet-api:${project.servletApiVersion}" // the version number here is sort of irrelevant until we use a repository other than the file system; it just needs to be something other than the old 2.4 version in external/libs/build
            }
        }
    }

    private void addDependencies(Project project)
    {
        project.dependencies
                {
                    jspCompile  'org.apache.tomcat:jasper',
                        'org.apache.tomcat:jsp-api',
                        'org.apache.tomcat:tomcat-juli'
                    jspCompile project.fileTree(dir: "${project.tomcatDir}/lib", includes: ['*.jar'])
                    BuildUtils.addLabKeyDependency(project: project, config: "jspCompile", depProjectPath: ":server:api")
                    BuildUtils.addLabKeyDependency(project: project, config: "jspCompile", depProjectPath: ":server:internal")
                    jspCompile project.files(project.tasks.jar)
                    if (project.hasProperty('apiJar'))
                        jspCompile project.files(project.tasks.apiJar)

                    jsp     'org.apache.tomcat:jasper',
                            'org.apache.tomcat:bootstrap',
                            'org.apache.tomcat:tomcat-juli'
                }
    }

    private void addJspTasks(Project project)
    {
        def Task listJsps = project.task('listJsps', group: GroupNames.JSP)
        listJsps.doLast {
                    getJspFileTree(project).each ({
                        println it.absolutePath
                    })
                }

        def Task copyJsps = project.task('copyJsp', group: GroupNames.JSP, type: Copy, description: "Copy jsp files to jsp compile directory",
                {
                    from 'src'
                    into "${project.buildDir}/${project.jspCompile.tempDir}/webapp"
                    include '**/*.jsp'

                    from 'resources'
                    into "${project.buildDir}/${project.jspCompile.tempDir}/webapp/org/labkey/${project.name}"
                    include '**/*.jsp'

                })

        def Task copyTags = project.task('copyTagLibs', group: GroupNames.JSP, type: Copy, description: "Copy the tag library (.tld) files to jsp compile directory",
                {
                    from project.staging.webInfDir
                    into "${project.buildDir}/${project.jspCompile.tempDir}/webapp/WEB-INF"
                    include 'web.xml'
                    include '*.tld'
                    include 'tags/**'
                })

        def Task jspCompileTask = project.task('jsp2Java',
                group: GroupNames.JSP,
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
                group: GroupNames.JSP,
                type: Jar,
                description: "produce jar file of jsps",
                {
                    classifier CLASSIFIER
                    from "${project.buildDir}/${project.jspCompile.classDir}"
                    baseName "${project.name}${BASE_NAME_EXTENSION}"
                    destinationDir = project.file(project.labkey.explodedModuleLibDir)
                }
        )

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
