package org.labkey.gradle.task

import org.gradle.api.file.CopySpec
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class ClientApiDistribution extends DistributionTask
{
    @OutputDirectory
    File javaDir

    @OutputDirectory
    File javascriptDir

    @OutputDirectory
    File xmlDir

    String apiDocsBuildDir
    String xsdDocsBuildDir

    ClientApiDistribution()
    {

        javaDir = new File(dir, "/client-api/java")
        javascriptDir = new File(dir, "/client-api/javascript")
        xmlDir = new File(dir, "/client-api/XML")

        apiDocsBuildDir = project.project(":server").tasks.jsdoc.outputs.getFiles().asPath
        xsdDocsBuildDir = project.project(":server").tasks.xsddoc.outputs.getFiles().asPath

    }

    @TaskAction
    void doAction()
    {
        createJavaDocs()

        createClientApiDocs()

        createXsdDocs()

        createTeamCityArchives()
    }

    private void createJavaDocs()
    {
        project.copy({CopySpec copy ->
            copy.from project.project(":remoteapi:java").tasks.fatJar
            copy.into javaDir
        })
        ant.zip(destfile: "${javaDir}/LabKey${project.labkeyVersion}-ClientAPI-Java.zip") {
            zipfileset(dir: project.project(":remoteapi:java").tasks.javadoc.destinationDir,
                    prefix: "doc")
            zipfileset(dir: "${project.project(":remoteapi:java").projectDir}/lib",
                    prefix: "lib/"){
                include(name:"*.jar")
            }
            zipfileset(file:"${project.project(":remoteapi:java").projectDir}/README.html")
            zipfileset(file:"${project.project(":remoteapi:java").tasks.fatJar.outputs.getFiles().asPath}")
        }

        ant.zip(destfile: "${javaDir}/LabKey${project.labkeyVersion}-ClientAPI-Java-src.zip") {
            zipfileset(dir: "${project.project(":remoteapi:java").projectDir}/src")
        }
    }

    private void createClientApiDocs()
    {
        ant.zip(destfile: "${javascriptDir}/LabKey${project.installerVersion}-ClientAPI-JavaScript-Docs.zip"){
            zipfileset(dir: apiDocsBuildDir,
                    prefix: "LabKey${project.installerVersion}-ClientAPI-JavaScript-Docs")
        }

        ant.tar(destfile: "${javascriptDir}/LabKey${project.installerVersion}-ClientAPI-JavaScript-Docs.tar.gz",
                longfile:"gnu",
                compression: "gzip") {
            tarfileset(dir: apiDocsBuildDir,
                    prefix: "LabKey${project.installerVersion}-ClientAPI-JavaScript-Docs")
        }
    }

    private void createXsdDocs()
    {

        ant.zip(destfile: "${xmlDir}/LabKey${project.installerVersion}-ClientAPI-XMLSchema-Docs.zip") {
            zipfileset(dir: xsdDocsBuildDir,
                    prefix: "LabKey${project.installerVersion}-ClientAPI-XMLSchema-Docs")
        }

        ant.tar(destfile: "${xmlDir}/LabKey${project.installerVersion}-ClientAPI-XMLSchema-Docs.tar.gz",
                longfile:"gnu",
                compression: "gzip") {
            tarfileset(dir: xsdDocsBuildDir,
                    prefix: "LabKey${project.installerVersion}-ClientAPI-XMLSchema-Docs")
        }
    }

    private void createTeamCityArchives()
    {
        //Create a stable file names so that TeamCity can serve it up directly through its own UI
        ant.zip(destfile:"${dir}/TeamCity-ClientAPI-Java-Docs.zip") {
            zipfileset(dir: project.project(":remoteapi:java").tasks.javadoc.destinationDir)
        }

        ant.zip(destfile: "${dir}/TeamCity-ClientAPI-JavaScript-Docs.zip") {
            zipfileset(dir: apiDocsBuildDir)
        }

        ant.zip(destfile: "${dir}/TeamCity-ClientAPI-XMLSchema-Docs.zip") {
            zipfileset(dir: xsdDocsBuildDir)
        }
    }
}
