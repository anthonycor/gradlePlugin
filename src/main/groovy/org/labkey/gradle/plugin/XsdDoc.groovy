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
        // This a subset of xsds because dependencies get pulled in.
        project.xsdDoc.xsdFiles = [
                project.project(":schemas").file("clientLibrary.xsd"),
                project.project(":schemas").file("apiTest.xsd"),
                project.project(":schemas").file("cohorts.xsd"),
                project.project(":schemas").file("datasets.xsd"),
                project.project(":schemas").file("domainTemplate.xsd"),
                project.project(":schemas").file("externalSchema.xsd"),
                project.project(":schemas").file("folder.xsd"),
                project.project(":schemas").file("folderType.xsd"),
                project.project(":schemas").file("freezerProExport.xsd"),
                project.project(":schemas").file("module.xsd"),
                project.project(":schemas").file("pages.xsd"),
                project.project(":schemas").file("participantGroups.xsd"),
                project.project(":schemas").file("pipelineOptions.xsd"),
                project.project(":schemas").file("qcStates.xsd"),
                project.project(":schemas").file("query.xsd"),
                project.project(":schemas").file("redcapExport.xsd"),
                project.project(":schemas").file("report.xsd"),
                project.project(":schemas").file("reportProps.xsd"),
                project.project(":schemas").file("studyDesign.xsd"),
                project.project(":schemas").file("studyViews.xsd"),
                project.project(":schemas").file("view.xsd"),
                project.project(":schemas").file("viewCategory.xsd"),
                project.project(":schemas").file("visitMap.xsd")
        ]
        if (project.findProject(":server:modules:study") != null)
        {
            project.xsdDoc.xsdFiles += [project.project(":server:modules:study").file("schemas/assayProvider.xsd"),
                                        project.project(":server:modules:study").file("schemas/studyPipelineTasks.xsd")]
        }
        if (project.findProject(":server:modules:pipeline")  != null)
            project.xsdDoc.xsdFiles += project.project(":server:modules:pipeline").file("schemas/pipelineTasks.xsd")
        if (project.findProject(":server:modules:dataintegration")  != null)
            project.xsdDoc.xsdFiles += project.project(":server:modules:dataintegration").file("schemas/etl.xsd")
        addDependencies(project)
        addTasks(project)
    }

    private void addDependencies(Project project)
    {
        project.configurations {
            xsdDoc
        }
        project.dependencies {
            xsdDoc 'xml-apis:xml-apis:1.3.04'
            xsdDoc 'xerces:xercesImpl:2.9.1'
            xsdDoc 'docflex:docflex-xml-re:1.7.2'
        }
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

                    classpath project.configurations.xsdDoc

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

class XsdDocExtension
{
    String root = "/tools/docflex-xml-re-1.7.2" //the location of the DocFlex/XML home directory
    File[] xsdFiles = []
    String outputDir
}
