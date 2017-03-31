package org.labkey.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/**
 * Task to compile XSD schema files into Java class files using the ant XMLBean
 */
class SchemaCompile extends DefaultTask {


  @InputDirectory
  File getSchemasDir()
  {
    return project.file(project.xmlBeans.schemasDir)
  }

  @OutputDirectory
  File getSrcGenDir()
  {
    return new File("$project.labkey.srcGenDir/$project.xmlBeans.classDir")
  }

  @OutputDirectory
  File getClassesDir()
  {
    return new File("$project.buildDir/$project.xmlBeans.classDir")
  }

  @TaskAction
  void compile() {
    ant.taskdef(
            name: 'xmlbean',
            classname: 'org.apache.xmlbeans.impl.tool.XMLBean',
            classpath: project.configurations.xmlbeans.asPath
    )
    ant.xmlbean(
            schema: getSchemasDir(),
            javasource: project.sourceCompatibility,
            srcgendir: getSrcGenDir(),
            classgendir: getClassesDir(),
            classpath: project.configurations.xmlbeans.asPath,
            failonerror: true
    )
  }
}