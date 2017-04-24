package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.labkey.gradle.plugin.extension.NpmRunExtension
import org.labkey.gradle.util.GroupNames

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
    public static final String TYPINGS_DIR = "typings"

    private static final String EXTENSION_NAME = "npmRun"

    static boolean isApplicable(Project project)
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
                    group = GroupNames.NPM_RUN
                    description = "Runs 'npm run ${project.npmRun.clean}'"
                    dependsOn "npmInstall"
                    dependsOn "npm_run_${project.npmRun.clean}"
                    mustRunAfter "npmInstall"
                }

        project.task("npmRunSetup")
                {
                    group = GroupNames.NPM_RUN
                    description = "Runs 'npm run ${project.npmRun.setup}'"
                    dependsOn "npmInstall"
                    dependsOn "npm_run_${project.npmRun.setup}"
                    mustRunAfter "npmInstall"
                }
        project.tasks.npmRunSetup.inputs.file(project.file(TYPINGS_FILE))
        project.tasks.npmRunSetup.outputs.dir(project.file(TYPINGS_DIR))
        project.tasks.getByName("npm_run_${project.npmRun.setup}").inputs.file(project.file(TYPINGS_FILE))
        project.tasks.getByName("npm_run_${project.npmRun.setup}").outputs.dir(project.file(TYPINGS_DIR))

        project.task("npmRunBuildProd")
                {
                    group = GroupNames.NPM_RUN
                    description = "Runs 'npm run ${project.npmRun.buildProd}'"
                    dependsOn "npmSetup"
                    dependsOn "npmRunSetup"
                    dependsOn "npm_run_${project.npmRun.buildProd}"
                    mustRunAfter "npmSetup"
                    mustRunAfter "npmRunSetup"
                }
        addTaskInputOutput(project.tasks.npmRunBuildProd)
        addTaskInputOutput(project.tasks.getByName("npm_run_${project.npmRun.buildProd}"))


        project.task("npmRunBuild")
                {
                    group = GroupNames.NPM_RUN
                    description ="Runs 'npm run ${project.npmRun.buildDev}'"
                    dependsOn "npmSetup"
                    dependsOn "npmRunSetup"
                    dependsOn "npm_run_${project.npmRun.buildDev}"
                    mustRunAfter "npmSetup"
                    mustRunAfter "npmRunSetup"
                }
        addTaskInputOutput(project.tasks.npmRunBuild)
        addTaskInputOutput(project.tasks.getByName("npm_run_${project.npmRun.buildDev}"))

        if (project.hasProperty('npmDevMode'))
            project.tasks.module.dependsOn("npmRunBuild")
        else
            project.tasks.module.dependsOn("npmRunBuildProd")

        project.tasks.npmInstall {
            dependsOn "npm_prune"
            inputs.file project.file(NPM_PROJECT_FILE)
            outputs.files project.fileTree(dir: "node_modules", include: "**")
        }
    }


    private void addTaskInputOutput(Task task)
    {
        task.inputs.file task.project.file(NPM_PROJECT_FILE)
        task.inputs.file task.project.file(TYPESCRIPT_CONFIG_FILE)
        task.inputs.file task.project.file(TYPINGS_FILE)
        task.inputs.dir task.project.file(WEBPACK_DIR)
        task.inputs.files task.project.fileTree(dir: "src", includes: ["client/**/*", "theme/**/*"])
        task.outputs.files task.project.fileTree(dir: "resources", includes: ["web/**/gen/**/*"])
    }
}

