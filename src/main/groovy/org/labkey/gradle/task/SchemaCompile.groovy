package org.labkey.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Task to compile XSD schema files into Java class files using the ant XMLBean
 */
class SchemaCompile extends DefaultTask {

  @TaskAction
  void compile() {
    ant.taskdef(
            name: 'xmlbean',
            classname: 'org.apache.xmlbeans.impl.tool.XMLBean',
            classpath: project.configurations.xmlbeans.asPath
    )
    ant.xmlbean(
            schema: project.file(project.xmlBeans.schemasDir),
            javasource: project.labkey.sourceCompatibility,
            srcgendir: "$project.labkey.srcGenDir/$project.xmlBeans.classDir",
            classgendir: "$project.buildDir/$project.xmlBeans.classDir",
            classpath: project.configurations.xmlbeans.asPath,
            failonerror: true
    )
  }
}