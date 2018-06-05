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
package org.labkey.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.TaskAction

import java.nio.file.Paths
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Creates a new module based on the contents of trunk/server/moduleTemplate. Sources properties from commandline or
 * user input based on the prompt flag.
 */
class CreateModule extends DefaultTask
{
    @TaskAction
    void createModule() {
        String moduleName
        String moduleDestination
        boolean hasManagedSchema
        boolean createTestFiles
        boolean createApiFiles

        if (project.hasProperty('moduleName')) {
            moduleName =  project.moduleName
        }
        else {
            project.ant.input(
                    message: "\nEnter the name for your new module: ",
                    addProperty: "new_moduleName"
            )
            moduleName = ant.new_moduleName
        }

        if (project.hasProperty('moduleDestination')) {
            moduleDestination =  project.moduleDestination
        }
        else {
            project.ant.input(
                    message: "\nEnter the full path for where to put the new module: ",
                    addProperty: "new_moduleDestination"
            )
            moduleDestination = ant.new_moduleDestination
        }

        if (project.hasProperty('createFiles')) {
            hasManagedSchema = ((String)project.createFiles).contains('schema')
            createTestFiles = ((String)project.createFiles).contains('test')
            createApiFiles = ((String)project.createFiles).contains('api')
        }
        else {
            project.ant.input(
                    message: "\nWill this module create and manage a database schema? (Y/n)",
                    addProperty: "new_hasManagedSchema"
            )
            hasManagedSchema = !(ant.new_hasManagedSchema.toLowerCase().equals("n"))

            project.ant.input(
                    message: "\nCreate test stubs (y/N)",
                    addProperty: "new_createTestFiles"
            )
            createTestFiles = ant.new_createTestFiles.toLowerCase().equals("y")

            project.ant.input(
                    message: "\nCreate API stubs (y/N)",
                    addProperty: "new_createApiFiles"
            )
            createApiFiles = ant.new_createApiFiles.toLowerCase().equals("y")
        }

        if (moduleName == null || moduleName == "") {
            throw new GradleException("moduleName is not specified")
        }
        if (moduleDestination == null || moduleDestination == "") {
            throw new GradleException("moduleDestination is not specified")
        }

        boolean shouldCreate = true

        File existingModuleDir = new File(moduleDestination)
        if (existingModuleDir.exists()) {
            ant.input(
                    message: "\nModule directory already exists (${moduleDestination}). " +
                            "Delete directory and create module anyway? (y/N)\"",
                    addProperty: "shouldCreateNewModule"
            )
            shouldCreate = ant.shouldCreateNewModule.toLowerCase().equals("y")
            if (shouldCreate) {
                project.logger.info("Attempting to delete existing module directory... " + existingModuleDir.deleteDir() ? "Succeeded" : "Failed")
            }
        }

        if (shouldCreate) {
            createNewModule(moduleName,
                    moduleDestination,
                    hasManagedSchema,
                    createTestFiles,
                    createApiFiles)
        }
    }

    void createNewModule(String moduleName, String moduleDestination, boolean hasManagedSchema, boolean createTestFiles, boolean createApiFiles)
    {
        try {
            project.mkdir(moduleDestination)
        }
        catch (Exception e) {
            project.logger.error("Failed to create new module directory at ${(new File(moduleDestination)).getAbsolutePath()}")
            throw new GradleException(e.getMessage())
        }

        Map<String, String> substitutions = [
                'MODULE_DIR_NAME' : moduleName,
                'MODULE_LOWERCASE_NAME' : moduleName.toLowerCase(),
                'MODULE_NAME' : moduleName,
        ]

        project.copy({ CopySpec copy ->
            String serverModuleTemplateDir = "${project.rootProject.projectDir}/server/moduleTemplate"
            File templateDir = new File(serverModuleTemplateDir)
            if (templateDir.exists())
                copy.from(serverModuleTemplateDir)
            else
            {
                // This seems a very convoluted way to get to the zip file in the jar file.  Using the classLoader did not
                // work as expected, however.  Following the example from here:
                // https://discuss.gradle.org/t/gradle-plugin-copy-directory-tree-with-files-from-resources/12767/7
                FileTree jarTree = project.zipTree(getClass().getProtectionDomain().getCodeSource().getLocation().toExternalForm())
                File zipFile = jarTree.matching({
                    include "moduleTemplate.zip"
                }).singleFile
                FileTree zipTree = project.zipTree(zipFile);

                copy.from(zipTree)
            }
            copy.into(moduleDestination)
            if (hasManagedSchema)
            {
                copy.exclude("**/MODULE_NAMECodeOnlyModule.java")
            }
            else
            {
                copy.exclude("**/MODULE_NAMESchema.java")
                copy.exclude("**/MODULE_NAMEModule.java")
                copy.exclude("resources/schemas/**")
            }
            if (!createTestFiles)
            {
                copy.exclude("test/**")
            }
            if (!createApiFiles)
            {
                copy.exclude("api-src/**")
            }

            copy.filter({String line ->
                Matcher matcher = Pattern.compile("(@@([^@]+)@@)").matcher(line)
                while(matcher.find()) {
                    line = line.replace(matcher.group(1), substitutions.get(matcher.group(2)))
                }
                return line
            })
        })
        if (createApiFiles)
        {
            File apiSrcDir = new File(moduleDestination, "api-src")
            if (!apiSrcDir.exists())
            {
                File moduleApiPackageDir = Paths.get(moduleDestination, "api-src/org/labkey/api/MODULE_DIR_NAME/").toFile();
                moduleApiPackageDir.mkdirs()
            }
        }
        File codeOnlyModule = new File("${moduleDestination}/src/org/labkey/MODULE_DIR_NAME/MODULE_NAMECodeOnlyModule.java")
        if (codeOnlyModule.exists()) {
            codeOnlyModule.renameTo(new File("${moduleDestination}/src/org/labkey/MODULE_DIR_NAME/MODULE_NAMEModule.java"))
        }
        //copy.rename only looks at file names, rather than files and directories.
        renameCrawler(project.file(moduleDestination), substitutions)

        project.logger.info("Module created in ${moduleDestination}")
        project.logger.info("Refresh the Gradle window to add this module to your IntelliJ project to start editing the code.")
    }

    void renameCrawler(File currFile, Map<String, String> substitutions) {
        for (File f : currFile.listFiles()) {
            renameCrawler(f, substitutions)
            substitutions.each({curr, updated ->
                f.renameTo(f.getPath().replace(curr, updated))
            })
        }
    }
}