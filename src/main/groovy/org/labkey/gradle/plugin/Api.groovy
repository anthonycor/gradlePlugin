package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.bundling.Jar

/**
 * Created by susanh on 4/11/16.
 */
class Api implements Plugin<Project>
{
    @Override
    void apply(Project project)
    {
        project.apply plugin: 'java-base'
        addSourceSet(project)
        addDependencies(project)
        addApiJarTask(project)
    }

    private void addSourceSet(Project project)
    {
        project.sourceSets
                {
                    api {
                        java {
                            srcDirs = ['api-src', 'intenral/gwtsrc']
                        }
                        output.classesDir = 'api-classes'
                    }
                }
    }

    private void addDependencies(Project project)
    {
        project.dependencies
                {
                    apiCompile  project.project(":server:api"),
                        project.project(":server:internal"),
//                        project.project(":remoteapi:java"),
                        'org.labkey:labkey-client-api:DevBuild' // TODO bad version name
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
//                    baseName "${project.name}-api"
                    archiveName "${project.name}-api"
                    destinationDir = project.libDir
                })
        apiJar.dependsOn(project.apiClasses)
        project.artifacts
                {
                    apiCompile apiJar
                }
    }
}
