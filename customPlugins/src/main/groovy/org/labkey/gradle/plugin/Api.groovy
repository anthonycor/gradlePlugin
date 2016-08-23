package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar

/**
 * Created by susanh on 4/11/16.
 */
class Api implements Plugin<Project>
{
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
                        project.project(":remoteapi:java")
                }
    }

    private void addApiJarTask(Project project)
    {
        def Task apiJar = project.task("apiJar",
                group: "api",
                type: Jar,
                description: "produce jar file for api",
                {
                    from project.sourceSets['api'].output.classesDir
                    baseName "${project.name}_api"
                    destinationDir = project.file(project.labkey.libDir)
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
        project.publishing {
            publications {
                api(MavenPublication) {
                    artifact project.tasks.apiJar
                }
            }
        }
    }
}
