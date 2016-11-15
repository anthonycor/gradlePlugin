package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.specs.AndSpec
import org.gradle.api.tasks.JavaExec
import org.labkey.gradle.util.GroupNames

/**
 * Created by susanh on 10/30/16.
 */
class XsdDoc implements Plugin<Project>
{
    @Override
    void apply(Project project)
    {
        project.extensions.create("xsdDoc", XsdDocExtension)
        project.xsdDoc.root = "${project.rootDir}/${project.xsdDoc.root}"
        project.xsdDoc.outputDir = "${project.rootProject.buildDir}/client-api/xml-schemas/docs"
        project.xsdDoc.xsdFiles = [
                project.project(":schemas").file("apiTest.xsd"),
                project.project(":server:modules:study").file("schemas/assayProvider.xsd"),
                project.project(":server:modules:pipeline").file("schemas/pipelineTasks.xsd"),
                project.project(":server:modules:study").file("schemas/studyPipelineTasks.xsd"),
                project.project(":server:modules:dataintegration").file("schemas/etl.xsd"),
                project.project(":schemas").file("cohorts.xsd"),
                project.project(":schemas").file("datasets.xsd"),
                project.project(":schemas").file("domainTemplate.xsd"),
                project.project(":schemas").file("folderType.xsd"),
                project.project(":schemas").file("freezerProExport.xsd"),
                project.project(":schemas").file("query.xsd"),
                project.project(":schemas").file("queryCustomView.xsd"),
                project.project(":schemas").file("redcapExport.xsd"),
                project.project(":schemas").file("report.xsd"),
                project.project(":schemas").file("study.xsd"),
                project.project(":schemas").file("studyDesign.xsd"),
                project.project(":schemas").file("tableInfo.xsd"),
                project.project(":schemas").file("view.xsd"),
                project.project(":schemas").file("visitMap.xsd"),
                project.project(":schemas").file("webpart.xsd")
        ]
        addTasks(project)
    }

    private void addTasks(Project project)
    {
       project.task(
                "xsddoc",
                group: GroupNames.DOCUMENTATION,
                type: JavaExec,
                description: 'Generating documentation for classes generated from XSD files',
                {
                    inputs.files project.xsdDoc.xsdFiles
                    outputs.dir project.xsdDoc.outputDir

                    // Workaround for incremental build (GRADLE-1483)
                    outputs.upToDateSpec = new AndSpec()

                    main "com.docflex.xml.Generator"
                    classpath {
                        [
                                "${project.xsdDoc.root}/lib/xml-apis.jar",
                                "${project.xsdDoc.root}/lib/xercesImpl.jar",
                                "${project.xsdDoc.root}/lib/docflex-xml-re.jar"
                        ]
                    }
                    args = [
                            "-template", "${project.xsdDoc.root}/templates/XSDDoc/FramedDoc.tpl",
                            "-p:docTitle", "LabKey XML Schema Reference",
                            "-format", "HTML", // output format
                            "-d", "${project.rootProject.buildDir}/client-api/xml-schemas/docs", // output directory
                            "-nodialog", // do not launch the generator GUI
                            "-launchviewer=false", //  do not launch the default viewer for the output file
                            //     Specify one or many data source XML files to be processed
                            //     by the specified template. (Both local pathnames and URLs
                            //     are allowed.)
                            project.project(":schemas").file("apiTest.xsd").path,
                            project.project(":server:modules:study").file("schemas/assayProvider.xsd").path,
                            project.project(":server:modules:pipeline").file("schemas/pipelineTasks.xsd").path,
                            project.project(":server:modules:study").file("schemas/studyPipelineTasks.xsd").path,
                            project.project(":server:modules:dataintegration").file("schemas/etl.xsd").path,
                            project.project(":schemas").file("cohorts.xsd").path,
                            project.project(":schemas").file("datasets.xsd").path,
                            project.project(":schemas").file("domainTemplate.xsd").path,
                            project.project(":schemas").file("folderType.xsd").path,
                            project.project(":schemas").file("freezerProExport.xsd").path,
                            project.project(":schemas").file("query.xsd").path,
                            project.project(":schemas").file("queryCustomView.xsd").path,
                            project.project(":schemas").file("redcapExport.xsd").path,
                            project.project(":schemas").file("report.xsd").path,
                            project.project(":schemas").file("study.xsd").path,
                            project.project(":schemas").file("studyDesign.xsd").path,
                            project.project(":schemas").file("tableInfo.xsd").path,
                            project.project(":schemas").file("view.xsd").path,
                            project.project(":schemas").file("visitMap.xsd").path,
                            project.project(":schemas").file("webpart.xsd").path
                    ]
                }
        )
    }
}

public class XsdDocExtension
{
    def String root = "/tools/docflex-xml-re-1.7.2" //the location of the DocFlex/XML home directory
    def File[] xsdFiles = []
    def outputDir
}
