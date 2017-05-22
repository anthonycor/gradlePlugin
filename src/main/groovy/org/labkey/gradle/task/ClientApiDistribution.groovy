package org.labkey.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction

class ClientApiDistribution extends DefaultTask
{
    public static final String CLIENT_API_JSDOC = "ClientAPI-JavaScript-Docs"
    public static final String JSDOC_CLASSIFIER = "jsdoc"
    public static final String XML_SCHEMA_DOC = "ClientAPI-XMLSchema-Docs"
    public static final String SCHEMA_DOC_CLASSIFIER = "schema-doc"

    File javaDir
    File javascriptDir
    File xmlDir

    String apiDocsBuildDir
    String xsdDocsBuildDir

    ClientApiDistribution()
    {
        javaDir = new File(project.dist.dir, "/client-api/java")
        javascriptDir = new File(project.dist.dir, "/client-api/javascript")
        xmlDir = new File(project.dist.dir, "/client-api/XML")

        apiDocsBuildDir = project.project(":server").tasks.jsdoc.outputs.getFiles().asPath
        xsdDocsBuildDir = project.project(":server").tasks.xsddoc.outputs.getFiles().asPath

    }

    @TaskAction
    void doAction()
    {
        // we do this already in the remoteapi/java project
        createJavaDocs()

        createClientApiDocs()

        createXsdDocs()

        createTeamCityArchives()
    }

    @OutputFile
    File getJavaClientApiFile()
    {
        return new File("${javaDir}/LabKey${project.labkeyVersion}-ClientAPI-Java.zip")
    }

    @OutputFile
    File getJavaClientApiSrcFile()
    {
        return new File("${javaDir}/LabKey${project.labkeyVersion}-ClientAPI-Java-src.zip")
    }

    private void createJavaDocs()
    {
        project.copy({CopySpec copy ->
            copy.from project.project(":remoteapi:java").tasks.fatJar
            copy.into javaDir
        })
        project.project(":remoteapi:java").configurations.compile.addToAntBuilder(ant, "zipfileset", FileCollection.AntType.FileSet)
        ant.zip(destfile: getJavaClientApiFile()) {
            zipfileset(dir: project.project(":remoteapi:java").tasks.javadoc.destinationDir,
                    prefix: "doc")
            zipfileset(file:"${project.project(":remoteapi:java").projectDir}/README.html")
            zipfileset(file:"${project.project(":remoteapi:java").tasks.fatJar.outputs.getFiles().asPath}")
        }

        ant.zip(destfile: getJavaClientApiSrcFile()) {
            zipfileset(dir: "${project.project(":remoteapi:java").projectDir}/src")
        }
    }

    @OutputFiles
   List<File> getJavascriptDocsFiles()
    {
        List<File> docsOutput = new ArrayList<>()
        docsOutput.add(getJavascriptDocsFile("zip"))
        docsOutput.add(getJavascriptDocsFile("tar.gz"))
        return docsOutput
    }

    private File getJavascriptDocsFile(String extension)
    {
        return new File("${javascriptDir}/${getJavascriptDocsPrefix()}.${extension}")
    }

    private String getJavascriptDocsPrefix()
    {
        return "LabKey${project.rootProject.installerVersion}-${CLIENT_API_JSDOC}"
    }

    private void createClientApiDocs()
    {
        String prefix = getJavascriptDocsPrefix()
        ant.zip(destfile: getJavascriptDocsFile("zip")){
            zipfileset(dir: apiDocsBuildDir, prefix: prefix)
        }

        ant.tar(destfile: getJavascriptDocsFile("tar.gz"),
                longfile:"gnu",
                compression: "gzip") {
            tarfileset(dir: apiDocsBuildDir, prefix: prefix)
        }
    }

    @OutputFiles
    List<File> getXsdDocsFiles()
    {
        List<File> docsOutput = new ArrayList<>()
        docsOutput.add(getXsdDocsFile("zip"))
        docsOutput.add(getXsdDocsFile("tar.gz"))
        return docsOutput
    }
    private File getXsdDocsFile(String extension)
    {
        return new File("${xmlDir}/${getXsdDocsPrefix()}.${extension}")
    }

    private String getXsdDocsPrefix()
    {
        return "LabKey${project.rootProject.installerVersion}-${XML_SCHEMA_DOC}"
    }


    private void createXsdDocs()
    {

        String prefix = getXsdDocsPrefix()
        ant.zip(destfile: getXsdDocsFile("zip")) {
            zipfileset(dir: xsdDocsBuildDir, prefix: prefix)
        }

        ant.tar(destfile: getXsdDocsFile("tar.gz"),
                longfile:"gnu",
                compression: "gzip") {
            tarfileset(dir: xsdDocsBuildDir, prefix: prefix)
        }
    }

    private void createTeamCityArchives()
    {
        //Create a stable file names so that TeamCity can serve it up directly through its own UI
        ant.zip(destfile:"${project.dist.dir}/TeamCity-ClientAPI-Java-Docs.zip") {
            zipfileset(dir: project.project(":remoteapi:java").tasks.javadoc.destinationDir)
        }

        ant.zip(destfile: "${project.dist.dir}/TeamCity-${CLIENT_API_JSDOC}.zip") {
            zipfileset(dir: apiDocsBuildDir)
        }

        ant.zip(destfile: "${project.dist.dir}/TeamCity-${XML_SCHEMA_DOC}.zip") {
            zipfileset(dir: xsdDocsBuildDir)
        }
    }
}
