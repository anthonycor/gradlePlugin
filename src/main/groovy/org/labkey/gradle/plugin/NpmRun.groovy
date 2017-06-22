/*
 * Copyright (c) 2016-2017 LabKey Corporation
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
    public static final String NODE_MODULES_DIR = "node_modules"
    public static final String WEBPACK_DIR = "webpack"

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

        String runCommand = project.hasProperty('npmDevMode') ? "npmRunBuild" : "npmRunBuildProd"
        if (project.tasks.findByName("module") != null)
            project.tasks.module.dependsOn(runCommand)
        if (project.tasks.findByName("processModuleResources") != null)
            project.tasks.processModuleResources.dependsOn(runCommand)

        project.tasks.npmInstall {
            dependsOn "npm_prune"
            inputs.file project.file(NPM_PROJECT_FILE)
            outputs.dir project.file(NODE_MODULES_DIR)
        }
    }


    private void addTaskInputOutput(Task task)
    {
        task.inputs.file task.project.file(NPM_PROJECT_FILE)
        task.inputs.file task.project.file(TYPESCRIPT_CONFIG_FILE)
        task.inputs.dir task.project.file(WEBPACK_DIR)

        // common input file pattern for client source
        task.inputs.files task.project.fileTree(dir: "src", includes: ["client/**/*", "theme/**/*"])

        // "core" theme building
        task.inputs.files task.project.fileTree(dir: "resources", includes: ["styles/**/*", "themes/**/*"])

        // common output file pattern for client artifacts
        task.outputs.files task.project.fileTree(dir: "resources", includes: ["web/**/*"])
    }
}

