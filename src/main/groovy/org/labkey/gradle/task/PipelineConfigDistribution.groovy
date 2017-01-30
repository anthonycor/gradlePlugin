package org.labkey.gradle.task

import org.gradle.api.tasks.TaskAction

class PipelineConfigDistribution extends DistributionTask
{
    @TaskAction
    void doAction()
    {
        ant.zip(destfile: "${dir}/LabKey${project.installerVersion}-PipelineConfig.zip") {
            zipfileset(dir: "${project.rootProject.projectDir}/server/configs/config-remote",
                    prefix: "remote")
            zipfileset(dir: "${project.rootProject.projectDir}/server/configs/config-cluster",
                    prefix: "cluster")
            zipfileset(dir: "${project.rootProject.projectDir}/server/configs/config-webserver",
                    prefix: "webserver")
        }
        ant.tar(destfile: "${dir}/LabKey${project.installerVersion}-PipelineConfig.tar.gz",
                longfile:"gnu",
                compression: "gzip") {
            tarfileset(dir: "${project.rootProject.projectDir}/server/configs/config-remote",
                    prefix: "remote") {
                exclude(name: "**/*.bat")
                exclude(name: "**/*.exe")
            }
            tarfileset(dir: "${project.rootProject.projectDir}/server/configs/config-cluster",
                    prefix: "cluster")
            tarfileset(dir: "${project.rootProject.projectDir}/server/configs/config-webserver",
                    prefix: "webserver")
        }
    }

}
