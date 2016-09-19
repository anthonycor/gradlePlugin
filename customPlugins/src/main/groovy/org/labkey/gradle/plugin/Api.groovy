package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.bundling.Jar
import org.labkey.gradle.util.GroupNames
/**
 * Add a sourceSet to create a module's api jar file
 */
class Api implements Plugin<Project>
{
    public static final String CLASSIFIER = "api"
    public static final String SOURCE_DIR = "api-src"

    public static boolean isApplicable(Project project)
    {
        return project.file(SOURCE_DIR).exists()
    }

    @Override
    void apply(Project project)
    {
        project.apply plugin: 'java-base'
        project.apply plugin: 'maven'
        project.apply plugin: 'maven-publish'
        addSourceSet(project)
        addDependencies(project)
        addApiJarTask(project)
        addArtifacts(project)
    }

    private void addSourceSet(Project project)
    {
        project.sourceSets
                {
                    api {
                        java {
                            srcDirs = [SOURCE_DIR, 'intenral/gwtsrc']
                        }
                        output.classesDir = "${project.buildDir}/api-classes"
                    }
                }
    }

    private void addDependencies(Project project)
    {
        project.dependencies
                {
                    apiCompile  project.project(":server:api"),
                        project.project(":server:internal"),
                        "org.labkey:labkey-client-api:${project.version}"
                }
    }

    private void addApiJarTask(Project project)
    {
        def Task apiJar = project.task("apiJar",
                group: GroupNames.API,
                type: Jar,
                description: "produce jar file for api",
                {
                    classifier CLASSIFIER
                    from project.sourceSets['api'].output.classesDir
                    baseName "${project.name}_api"
                    destinationDir = project.file(project.labkey.explodedModuleLibDir)
                })
        project.tasks.processApiResources.enabled = false
        apiJar.dependsOn(project.apiClasses)
        if (project.hasProperty('jsp2Java'))
            project.tasks.jsp2Java.dependsOn(apiJar)

        project.tasks.assemble.dependsOn(apiJar)

    }

    private void addArtifacts(Project project)
    {
        project.artifacts
                {
                    apiCompile project.tasks.apiJar
                }
    }
}
