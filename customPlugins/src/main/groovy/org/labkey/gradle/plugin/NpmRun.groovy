package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

// borrowed heavily from https://plugins.gradle.org/plugin/com.palantir.npm-run
class NpmRun implements Plugin<Project>
{
    public static final String NPM_PROJECT_FILE = "package.json"
    private static final String EXTENSION_NAME = "npmRun"
    private static final String GROUP_NAME = "NPM Run"

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


        project.task("npmRunBuild")
                {
                    group = GROUP_NAME
                    description ="Runs 'npm run ${project.npmRun.build}'"
                    dependsOn "npmSetup"
                    dependsOn "npm_run_${project.npmRun.build}"
                    mustRunAfter "npmSetup"
                }

        if (project.hasProperty('npmDevMode'))
            project.tasks.build.dependsOn("npmRunBuild")
        else
            project.tasks.build.dependsOn("npmRunBuildProd")
    }
}

class NpmRunExtension
{
    String clean = "clean"
    String setup = "setup"
    String buildProd = "build-prod"
    String build = "build"

}
