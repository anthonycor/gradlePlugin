package org.labkey.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction

class PipelineConfigDistribution extends DefaultTask
{
    public static final String CLASSIFIER = "PipelineConfig"

    @OutputFiles
    List<File> getConfigFiles()
    {
        List<File> files = new ArrayList<>();
        files.add(getConfigFile("zip"))
        files.add(getConfigFile("tar.gz"))
        return files
    }

    private File getConfigFile(String extension)
    {
        return new File("${project.dist.dir}/LabKey${project.installerVersion}-${CLASSIFIER}.${extension}")
    }

    @TaskAction
    void doAction()
    {
        ant.zip(destfile: getConfigFile("zip")) {
            zipfileset(dir: "${project.rootProject.projectDir}/server/configs/config-remote",
                    prefix: "remote")
            zipfileset(dir: "${project.rootProject.projectDir}/server/configs/config-cluster",
                    prefix: "cluster")
            zipfileset(dir: "${project.rootProject.projectDir}/server/configs/config-webserver",
                    prefix: "webserver")
        }
        ant.tar(destfile: getConfigFile("tar.gz"),
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
