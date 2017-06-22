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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DeleteSpec
import org.gradle.api.tasks.Delete
import org.labkey.gradle.task.InstallRLabKey
import org.labkey.gradle.task.InstallRPackage
import org.labkey.gradle.task.InstallRuminex
import org.labkey.gradle.util.GroupNames
/**
 * Created by susanh on 3/15/17.
 */
class RPackages implements Plugin<Project>
{
    @Override
    void apply(Project project)
    {
        addTasks(project)
    }

    private static void addTasks(Project project)
    {
        String rLibsUserPath = InstallRPackage.getRLibsUserPath(project)
        project.task("clean",
            type: Delete,
            group: GroupNames.DEPLOY,
            description: "Delete user directory containing R libraries (${rLibsUserPath})",
                {
                    DeleteSpec delete ->
                        if (rLibsUserPath != null)
                            delete.delete rLibsUserPath
                }
        )
        project.task("installRLabKey",
                type: InstallRLabKey,
                group: GroupNames.DEPLOY,
                description: "Install RLabKey"
        )
        project.task("installRuminex",
                type: InstallRuminex,
                group: GroupNames.DEPLOY,
                description: "Install Ruminex package")
                {InstallRPackage task ->
                    task.packageNames = ["Ruminex"]
                    task.installScript = "install-ruminex-dependencies.R"
                }

        project.task("installFlowWorkspace",
                type: InstallRPackage,
                group: GroupNames.DEPLOY,
                description: "Install flow workspace package")
                {
                    InstallRPackage task ->
                        task.packageNames = ["flowWorkspace"]
                        task.installScript = "install-flowWorkspace.R"
                }
        project.task("installFlowStats",
                type: InstallRPackage,
                group: GroupNames.DEPLOY,
                description: "Install flowStats package")
                {InstallRPackage task ->
                    task.packageNames = ["flowStats"]
                    task.installScript = "install-flowStats.R"
                    task.dependsOn(project.tasks.installFlowWorkspace)
                }

        project.task("installKnitr",
                type: InstallRPackage,
                group: GroupNames.DEPLOY,
                description: "Install knitr package",
                {InstallRPackage task ->
                    task.packageNames = ["knitr", "rmarkdown"]
                    task.installScript = "install-knitr.R"
                })

        project.task("installEhrDependencies",
                type: InstallRPackage,
                group: GroupNames.DEPLOY,
                description: "Install EHR Dependencies packages",
                {
                    InstallRPackage task ->
                        task.packageNames = ["kinship2", "pedigree"]
                        task.installScript = "install-ehr-dependencies.R"
                })

        project.task("installRSurvival",
                type: InstallRPackage,
                group: GroupNames.DEPLOY,
                description: "Install RSurvival package",
                {
                    InstallRPackage task ->
                        task.packageNames = ["survival"]
                        task.installScript = "install-survival.R"
                })

        project.task("install",
                group: GroupNames.DEPLOY,
                description: "Install R packages"
        ).dependsOn(project.tasks.installRLabKey,
                project.tasks.installRuminex,
                project.tasks.installFlowStats,
                project.tasks.installKnitr,
                project.tasks.installEhrDependencies,
                project.tasks.installRSurvival)
    }

}
