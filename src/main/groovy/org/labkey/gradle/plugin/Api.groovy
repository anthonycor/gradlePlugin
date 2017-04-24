package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.CopySpec
import org.gradle.api.tasks.bundling.Jar
import org.labkey.gradle.plugin.extension.LabKeyExtension
import org.labkey.gradle.util.BuildUtils
import org.labkey.gradle.util.GroupNames

/**
 * Add a sourceSet to create a module's api jar file
 */
class Api implements Plugin<Project>
{
    public static final String CLASSIFIER = "api"
    public static final String SOURCE_DIR = "api-src"
    public static final String ALT_SOURCE_DIR = "src/api-src"
    private static final String MODULES_API_DIR = "modules-api"

    static boolean isApplicable(Project project)
    {
        return project.file(SOURCE_DIR).exists() || project.file(ALT_SOURCE_DIR).exists()
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
                            srcDirs = [project.file(SOURCE_DIR).exists() ? SOURCE_DIR : ALT_SOURCE_DIR, 'intenral/gwtsrc']
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
                        project.project(":server:internal")
                    BuildUtils.addLabKeyDependency(project: project, config: "apiCompile", depProjectPath: ":remoteapi:java")
                }
    }

    private void addApiJarTask(Project project)
    {
        Task apiJar = project.task("apiJar",
                group: GroupNames.API,
                type: Jar,
                description: "produce jar file for api",
                {Jar jar ->
                    jar.classifier CLASSIFIER
                    jar.from project.sourceSets['api'].output.classesDir
                    jar.baseName "${project.name}_api"
                    jar.destinationDir = project.file(project.labkey.explodedModuleLibDir)
                })
        project.tasks.processApiResources.enabled = false
        apiJar.dependsOn(project.apiClasses)
        if (project.hasProperty('jsp2Java'))
            project.tasks.jsp2Java.dependsOn(apiJar)

        project.tasks.assemble.dependsOn(apiJar)

        if (LabKeyExtension.isDevMode(project))
        {
            // we put all API jar files into a special directory for the RecompilingJspClassLoader's classpath
            apiJar.doLast {
                project.copy { CopySpec copy ->
                    copy.from project.file(project.labkey.explodedModuleLibDir)
                    copy.into "${project.rootProject.buildDir}/${MODULES_API_DIR}"
                    copy.include "${project.name}_api*.jar"
                }
            }
        }
    }

    private void addArtifacts(Project project)
    {
        project.artifacts
                {
                    apiCompile project.tasks.apiJar
                }
    }
}
