package org.labkey.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Created by susanh on 4/5/16.
 */
class JspCompile extends DefaultTask
{
    @TaskAction
    def compile() {
        ant.taskdef(
                name: 'jasper',
                classname: 'org.apache.jasper.JspC',
                classpath: project.configurations.jsp.asPath
        )
        ant.jasper(
                uriroot: "$project.configurations.jsp.tempDir/webapp",
                outputDir: "$project.configurations.jsp.tempDir/classes",
                package: "org.labkey.jsp.compiled",
                compilerTargetVM: project.sourceCompatibility,
                compilerSourceVM: project.sourceCompatibility,
                trimSpaces: false,
                compile: false,
                listErrors: true
        )
    }
}
