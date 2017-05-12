package org.labkey.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.CopySpec
import org.gradle.api.tasks.TaskAction

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Creates a new module based on the contents of trunk/server/moduleTemplate. Sources properties from commandline or
 * user input based on the prompt flag.
 */
class CreateModule extends DefaultTask
{
    boolean prompt = true

    @TaskAction
    void createModule() {
        String moduleName = null
        String moduleDestination = null
        boolean hasManagedSchema = true
        boolean createTestFiles = true
        boolean createApiFiles = true

        if (prompt) {
            project.ant.input(
                    message: "\nEnter the name for your new module: ",
                    addProperty: "new_moduleName"
            )
            moduleName = ant.new_moduleName

            project.ant.input(
                    message: "\nEnter the full path for where to put the new module: ",
                    addProperty: "new_moduleDestination"
            )
            moduleDestination = ant.new_moduleDestination

            project.ant.input(
                    message: "\nWill this module create and manage a database schema? (y/N)",
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
        else {
            if (project.hasProperty('moduleName')) {
                moduleName = project.moduleName
            }
            if (project.hasProperty('moduleDestination')) {
                moduleDestination = project.moduleDestination
            }
            if (project.hasProperty('hasManagedSchema')) {
                hasManagedSchema = !(project.hasManagedSchema.toLowerCase().equals("n"))
            }
            if (project.hasProperty('createTestFiles')) {
                createTestFiles = project.createTestFiles.toLowerCase().equals("y")
            }
            if (project.hasProperty('createApiFiles')) {
                createApiFiles = project.createApiFiles.toLowerCase().equals("y")
            }
        }

        if (moduleName == null) {
            project.logger.error("moduleName is not specified")
        }
        if (moduleDestination == null) {
            project.logger.error("moduleDestination is not specified")
        }

        boolean shouldCreate = true

        File existingModuleDir = new File(moduleDestination)
        if (existingModuleDir.exists()) {
            ant.input(
                    message: "\nModule directory already exists (${moduleDestination}). " +
                            "Create module anyway? (y/N)\"",
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
            project.logger.error("Error generated : ${e.message}")
            project.logger.error(e.stackTrace.toArrayString())
        }

        Map<String, String> substitutions = [
                'MODULE_DIR_NAME' : moduleName,
                'MODULE_LOWERCASE_NAME' : moduleName.toLowerCase(),
                'MODULE_NAME' : moduleName,
        ]

        project.copy({ CopySpec copy ->
            copy.from("${project.rootProject.projectDir}/server/moduleTemplate")
            copy.into(moduleDestination)
            if (hasManagedSchema) {
                copy.exclude("**/MODULE_NAMECodeOnlyModule.java")
            }
            else {
                copy.exclude("**/MODULE_NAMESchema.java")
                copy.exclude("**/MODULE_NAMEModule.java")
            }
            if (!createTestFiles) {
                copy.exclude("test/**")
            }
            if (!createApiFiles) {
                copy.exclude("api-src/**")
            }

            copy.exclude("**/*.iml")

            copy.filter({String line ->
                Matcher matcher = Pattern.compile("(@@([^@]+)@@)").matcher(line)
                while(matcher.find()) {
                    line = line.replace(matcher.group(1), substitutions.get(matcher.group(2)))
                }
                return line
            })
        })
        //copy.rename only looks at file names, rather than files and directories.
        renameCrawler(project.file(moduleDestination), substitutions)
        File codeOnlyModule = new File("${moduleDestination}/src/org/labkey/${moduleName}/MODULE_NAMECodeOnlyModule.java")
        if (codeOnlyModule.exists()) {
            codeOnlyModule.renameTo(new File("${moduleDestination}/src/org/labkey/${moduleName}/MODULE_NAMEModule.java"))
        }

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