/*
 * Copyright (c) 2017 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Jar
import org.labkey.gradle.plugin.extension.ModuleExtension
import org.labkey.gradle.task.CheckForVersionConflicts
import org.labkey.gradle.util.BuildUtils
import org.labkey.gradle.util.GroupNames
/**
 * This class is used for building a LabKey Java module (one that typically resides in a *modules
 * directory).  It defines tasks for building the jar files (<module>_jsp.jar, <module>.jar)
 * as well as tasks for copying resources to the build directory.
 *
 */
class JavaModule extends FileModule
{
    public static final DIR_NAME = "src";

    static boolean isApplicable(Project project)
    {
        return project.file(DIR_NAME).exists()
    }

    @Override
    protected void applyPlugins(Project project)
    {
        project.apply plugin: 'maven'
        project.apply plugin: 'maven-publish'

        if (AntBuild.isApplicable(project))
        {
            if (shouldDoBuild(project))
                project.apply plugin: 'org.labkey.antBuild'
        }
        else
        {
            setJavaBuildProperties(project)
            // We don't have an isApplicable method here because the directory we need to check is set in the extension
            // created by this plugin.  We could separate extension creation from plugin application, but it would be
            // different from the pattern used elsewhere.  Schema tasks will be skipped if there are no xsd files in
            // the designated directory
            project.apply plugin: 'org.labkey.xmlBeans'

            if (ModuleResources.isApplicable(project))
                project.apply plugin: 'org.labkey.moduleResources'
            if (Api.isApplicable(project))
                project.apply plugin: 'org.labkey.api'

            if (SpringConfig.isApplicable(project))
                project.apply plugin: 'org.labkey.springConfig'

            if (Webapp.isApplicable(project))
                project.apply plugin: 'org.labkey.webapp'

            if (ClientLibraries.isApplicable(project))
                project.apply plugin: 'org.labkey.clientLibraries'

            if (Jsp.isApplicable(project))
                project.apply plugin: 'org.labkey.jsp'

            if (Gwt.isApplicable(project))
                project.apply plugin: 'org.labkey.gwt'

            if (NpmRun.isApplicable(project))
            {
                // This brings in nodeSetup and npmInstall tasks.  See https://github.com/srs/gradle-node-plugin
                project.apply plugin: 'com.moowork.node'
                project.apply plugin: 'org.labkey.npmRun'
            }

            if (UiTest.isApplicable(project))
            {
                project.apply plugin: 'org.labkey.uiTest'
            }
        }
    }

    @Override
    protected void addConfigurations(Project project)
    {
        super.addConfigurations(project)
        project.configurations
                {
                    // We will use these when moving away from the deprecated compile configuration from Gradle
//                    local.extendsFrom(implementation)
//                    external.extendsFrom(api)
                    local
                    labkey // use this configuration for dependencies to labkey API jars that are needed for a module
                           // but don't need to show up in the dependencies.txt and jars.txt
                    compile.extendsFrom(external)
                    compile.extendsFrom(labkey)
                    compile.extendsFrom(local)
                }
    }

    protected void setJavaBuildProperties(Project project)
    {
        project.libsDirName = 'explodedModule/lib'

        addSourceSets(project)

        project.jar { Jar jar ->
            jar.baseName = project.name
        }
    }

    private void addSourceSets(Project project)
    {
        project.sourceSets {
            main {
                java {
                    srcDirs = ['src']
                }
            }
        }
    }

    static boolean isDatabaseSupported(Project project, String database)
    {
        ModuleExtension extension = project.extensions.getByType(ModuleExtension.class)
        String supported = extension.getPropertyValue("SupportedDatabases")
        return supported == null || supported.contains(database)
    }

    @Override
    protected void addTasks(Project project)
    {
        super.addTasks(project)
        setJarManifestAttributes(project, project.jar.manifest)
//        FIXME: the following tasks will not work in general because of the cycle between ms2 and ms1.  Would be nice...
//        _project.task('javadocJar', description: "Generate jar file of javadoc files", type: Jar) {
//            from project.tasks.javadoc.destinationDir
//            group GroupNames.DISTRIBUTION
//            baseName "${project.name}_${LabKey.JAVADOC_CLASSIFIER}"
//            classifier LabKey.JAVADOC_CLASSIFIER
//            dependsOn project.tasks.javadoc
//        }
//
//        _project.task('sourcesJar', description: "Generate jar file of source files", type: Jar) {
//            from project.sourceSets.main.allJava
//
//            group GroupNames.DISTRIBUTION
//            baseName "${project.name}_${LabKey.SOURCES_CLASSIFIER}"
//            classifier LabKey.SOURCES_CLASSIFIER
//        }


        FileCollection externalFiles = getTrimmedExternalFiles(project)
        FileCollection allJars = externalFiles

        Task copyExternalDependencies = project.task("copyExternalLibs",
                group: GroupNames.MODULE,
                type: Copy,
                description: "copy the dependencies declared in the 'external' configuration into the lib directory of the built module",
                { CopySpec copy ->
                    copy.from externalFiles
                    copy.into "${project.labkey.explodedModuleDir}/lib"
                    copy.include "*.jar"
                }
        )

        if (project.tasks.findByName("module") != null)
        {
            project.tasks.module.dependsOn(copyExternalDependencies)
            if (project.file("src").exists())
            {
                project.tasks.module.dependsOn(project.tasks.jar)
                allJars = allJars + project.tasks.jar.outputs.files
            }
            if (project.hasProperty('apiJar'))
            {
                project.tasks.module.dependsOn(project.tasks.apiJar)
                allJars = allJars + project.tasks.apiJar.outputs.files
            }
            if (project.hasProperty('jspJar'))
            {
                project.tasks.module.dependsOn(project.tasks.jspJar)
                allJars = allJars + project.tasks.jspJar.outputs.files
            }
            if (project.hasProperty('schemasJar'))
            {
                project.tasks.module.dependsOn(project.tasks.schemasJar)
                allJars = allJars + project.tasks.schemasJar.outputs.files
            }
        }
        Task checkJarVersions = project.task(
                "checkModuleJarVersions",
                group: GroupNames.MODULE,
                type: CheckForVersionConflicts,
                description: "Check for conflicts in version numbers of jar files to be included in the module and files already in the build directory ${project.labkey.explodedModuleDir}/lib." +
                        "Default action on detecting a conflict is to fail.  Use -PversionConflictAction=[delete|fail|warn] to change this behavior.  The value 'delete' will cause the " +
                        "conflicting version(s) in the ${project.labkey.explodedModuleDir}/lib directory to be removed.",
                { CheckForVersionConflicts task ->
                    task.directory = new File("${project.labkey.explodedModuleDir}/lib")
                    task.extension = "jar"
                    task.cleanTask = "${project.path}:clean"
                    task.collection = allJars
                }
        )

        copyExternalDependencies.dependsOn(checkJarVersions)
        project.project(":server").tasks.checkVersionConflicts.dependsOn(checkJarVersions)
    }

    /**
     * Returns the set of files included in the external configuration for a project
     * with the jar files already included in the base modules removed.  If the project
     * is a base module, only the jars included in the api module are removed.  If the
     * project is the api module, all jars in its external configuration are included.
     * @param project the project whose external dependencies are to be removed
     * @return the collection of files that contain the dependencies
     */
    static FileCollection getTrimmedExternalFiles(Project project)
    {
        FileCollection labkeyConfig = project.configurations.labkey
        FileCollection config = project.configurations.external

        if (config == null && labkeyConfig == null)
            return null

        config = labkeyConfig == null ? config : (config == null ? labkeyConfig : config + labkeyConfig)

        // trim nothing from api
        String apiProjectPath = BuildUtils.getProjectPath(project.gradle, "apiProjectPath", ":server:api")
        if (project.path.equals(apiProjectPath))
            return config
        // base modules should remove everything included by api
        else if (BuildUtils.getBaseModules(project.gradle).contains(project.path))
        {
            return config - project.project(BuildUtils.getProjectPath(project.gradle, "apiProjectPath", ":server:api")).configurations.external
        }
        else // all other modules should remove everything in the base modules
        {
            for (String path : BuildUtils.getBaseModules(project.gradle))
            {
                FileCollection otherExternal = project.project(path).configurations.external
                if (otherExternal != null)
                {
                    config = config - otherExternal
                }
            }
        }
        return config
    }
}

