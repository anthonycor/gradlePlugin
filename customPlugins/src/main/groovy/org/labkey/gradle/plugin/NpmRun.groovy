package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

/**
 * Used to add tasks for running npm commands for a module.
 *
 * Borrowed heavily from https://plugins.gradle.org/plugin/com.palantir.npm-run
 */
class NpmRun implements Plugin<Project>
{
    public static final String NPM_PROJECT_FILE = "package.json"
    public static final String TYPESCRIPT_CONFIG_FILE = "tsconfig.json"
    public static final String TYPINGS_FILE = "typings.json"
    public static final String WEBPACK_DIR = "webpack"

    private static final String EXTENSION_NAME = "npmRun"
    private static final String GROUP_NAME = "NPM Run"

    public static boolean isApplicable(Project project)
    {
        return project.file(NPM_PROJECT_FILE).exists()
    }

    @Override
    void apply(Project project)
    {
        project.extensions.create(EXTENSION_NAME, NpmRunExtension)

        project.afterEvaluate {
            addTasks(project)
        }
    }

    private void addTasks(Project project)
    {
        project.task("npmRunClean")
                {
                    group = GROUP_NAME
                    description = "Runs 'npm run ${project.npmRun.clean}'"
                    dependsOn "npmInstall"
                    dependsOn "npm_run_${project.npmRun.clean}"
                    mustRunAfter "npmInstall"
                }


        project.task("npmRunSetup")
                {
                    group = GROUP_NAME
                    description = "Runs 'npm run ${project.npmRun.setup}'"
                    dependsOn "npmInstall"
                    dependsOn "npm_run_${project.npmRun.setup}"
                    mustRunAfter "npmInstall"
                }

        project.task("npmRunBuildProd")
                {
                    group = GROUP_NAME
                    description = "Runs 'npm run ${project.npmRun.buildProd}'"
                    dependsOn "npmSetup"
                    dependsOn "npm_run_${project.npmRun.buildProd}"
                    mustRunAfter "npmSetup"
                }
        addTaskInputOutput(project.tasks.npmRunBuildProd)
        addTaskInputOutput(project.tasks.getByName("npm_run_${project.npmRun.buildProd}"))


        project.task("npmRunBuild")
                {
                    group = GROUP_NAME
                    description ="Runs 'npm run ${project.npmRun.buildDev}'"
                    dependsOn "npmSetup"
                    dependsOn "npm_run_${project.npmRun.buildDev}"
                    mustRunAfter "npmSetup"
                }
        addTaskInputOutput(project.tasks.npmRunBuild)
        addTaskInputOutput(project.tasks.getByName("npm_run_${project.npmRun.buildDev}"))

        if (project.hasProperty('npmDevMode'))
            project.tasks.build.dependsOn("npmRunBuild")
        else
            project.tasks.build.dependsOn("npmRunBuildProd")
    }

    private void addTaskInputOutput(Task task)
    {
        task.inputs.file task.project.file(NPM_PROJECT_FILE)
        task.inputs.file task.project.file(TYPESCRIPT_CONFIG_FILE)
        task.inputs.file task.project.file(TYPINGS_FILE)
        task.inputs.dir task.project.file(WEBPACK_DIR)
        task.inputs.files task.project.fileTree(dir: "src", includes: ["client/**/*", "theme/**/*"])
        task.outputs.dir  task.project.fileTree(dir: "resources", includes: ["**/gen"])
    }
}

class NpmRunExtension
{
    String clean = "clean"
    String setup = "setup"
    String buildProd = "build-prod"
    String buildDev = "build"

}
