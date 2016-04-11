package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.bundling.Jar
import org.labkey.gradle.task.JspCompile


/**
 * Created by susanh on 4/5/16.
 */
class LabKeyModule implements Plugin<Project>
{
    // Deprecated: set the skipBuild property to true in the module's build.gradle file instead
    def String skipBuildFile = "skipBuild.txt"
    def String modulePropertiesFile = "module.properties"

    @Override
    void apply(Project project)
    {
        project.apply plugin: 'xmlBeans'
        project.apply plugin: 'java-base'

        project.build.onlyIf({
            def List<String> indicators = new ArrayList<>();
            if (project.file(skipBuildFile).exists())
                indicators.add(skipBuildFile + " exists")
            // TODO use a property to indicate if it's a module rather than rely on the path
            if (project.path.contains("modules:") && !project.file(modulePropertiesFile).exists())
                indicators.add(modulePropertiesFile + " does not exist")
            if (project.skipBuild)
                indicators.add("skipBuild property set for Gradle project")

            if (indicators.size() > 0)
            {
                project.logger.info("$project.name build skipped because: " + indicators.join("; "))
            }
            return indicators.isEmpty()
        })

        if (project.path.contains("modules:"))
        {
            addApiSource(project);
            addApiJarTask(project);
        }
//        addJspTasks(project);
    }

    private void addApiSource(Project project)
    {
        project.sourceSets {
            api {
                java {
                    srcDirs = ['api-src', 'intenral/gwtsrc']
                }
                output.classesDir = 'api-classes'
            }

            // TODO move to a different method
            schemas {
                resources {
                    srcDirs = ['resources']
                    exclude "schemas/**/obsolete/**"
                }
                output.resourcesDir = project.explodedModuleDir
            }
        }
        project.dependencies ({
            apiCompile project.project(":server:api"),
                        project.project(":server:internal"),
                        project.project(":remoteapi:java"),
                        'org.labkey:labkey-client-api:DevBuild' // TODO bad version name
        })
    }

    private void addApiJarTask(Project project)
    {
        def Task apiJar = project.task("apiJar",
                group: "api",
                type: Jar,
                description: "produce jar file for api", {
            from project.sourceSets['api'].output.classesDir
            baseName "${project.name}-api"
            destinationDir = project.libDir
        })
        apiJar.dependsOn(project.apiClasses)
        project.artifacts {
            apiCompile apiJar
        }
    }

    private void addJspTasks(Project project)
    {
        project.extensions.create("jspCompile", JspCompileExtension)
        def FileTree jspTree = project.fileTree("src").include('**/*.jsp');
        jspTree += project.fileTree("resources").include("**/*.jsp");

        project.task("listJsps") << {
            jspTree.each({ File file ->
                println file
            })
        }

        def Task jspCompile = project.task('jspCompile',
                group: "jsp",
                type: JspCompile,
                description: "compile jsp files into Java classes",
                {
                    inputs.files jspTree
                    outputs.dir "$project.buildDir/$project.jspCompile.classDir"
                }
        )

        def Task jspJar = project.task('jspJar',
                group: "jsp",
                type: Jar,
                description: "produce jar file of jsps", {
            from project.xmlBeans.classDir
            exclude '**/*.java'
            //baseName "${project.name}_jsp"
            archiveName "${project.name}_jsp.jar" // TODO remove this in favor of a versioned jar file when other items have change
            destinationDir = project.libDir
        })
        jspJar.dependsOn(jspCompile)

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
