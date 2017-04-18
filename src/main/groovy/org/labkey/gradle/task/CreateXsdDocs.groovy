package org.labkey.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
/**
 * Created by susanh on 4/17/17.
 */
class CreateXsdDocs extends DefaultTask
{
    @InputFiles
    List<File> getXsdFiles()
    {
        return project.xsdDoc.xsdFiles
    }

    @OutputDirectory
    File getOutputDirectory()
    {
        return new File("${project.rootProject.buildDir}/client-api/xml-schemas/docs")
    }

    @TaskAction
    void createDocs()
    {
        List<File> xsdFiles = getXsdFiles()
        project.javaexec {exec ->
            exec.main = "com.docflex.xml.Generator"

            exec.classpath project.configurations.xsdDoc

            exec.args = [
                    "-template", "${getDocFlexRoot()}/templates/XSDDoc/FramedDoc.tpl",
                    "-p:docTitle", "LabKey XML Schema Reference",
                    "-format", "HTML", // output format
                    "-d", getOutputDirectory(), // output directory
                    "-nodialog", // do not launch the generator GUI
                    "-launchviewer=false", //  do not launch the default viewer for the output file
            ]
            //     Specify one or many data source XML files to be processed
            //     by the specified template. (Both local pathnames and URLs
            //     are allowed.)
            xsdFiles.each {File file ->
                exec.args += file.path
            }
        }
    }


    //the location of the DocFlex/XML home directory
    String getDocFlexRoot()
    {
        return "${project.rootDir}/tools/docflex-xml-re-${project.docflexXmlReVersion}"
    }
}
