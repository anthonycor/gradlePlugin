/*
 * Copyright (c) 2016-2017 LabKey Corporation
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