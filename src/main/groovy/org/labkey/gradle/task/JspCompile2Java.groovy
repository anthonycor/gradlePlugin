package org.labkey.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Created by susanh on 4/5/16.
 */
class JspCompile2Java extends DefaultTask
{
    @TaskAction
    def compile() {
        ant.taskdef(
                name: 'jasper',
                classname: 'org.apache.jasper.JspC',
                classpath: project.configurations.jspCompile.asPath
        )
        ant.jasper(
                uriroot: "${project.buildDir}/${project.jspCompile.tempDir}/webapp",
                outputDir: "${project.buildDir}/${project.jspCompile.tempDir}/classes",
                package: "org.labkey.jsp.compiled",
                compilerTargetVM: project.labkey.targetCompatibility,
                compilerSourceVM: project.labkey.sourceCompatibility,
                trimSpaces: false,
                compile: false,
                listErrors: true
        )
    }
}
