package org.labkey.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/**
 * Created by susanh on 8/8/16.
 */
class ServerSideJS extends DefaultTask
{
    @InputDirectory
    File scriptFragmentsDir = project.file("script-fragments")

    @OutputDirectory
    File scriptsDir = project.file("resources/scripts")

    @TaskAction
    void action()
    {
        concatenateExt3JsFiles()
        concatenateExt4JsFiles()
        // copy some of the clientapi into the core module's server-side scripts
        concatenateLabKeyJsFile("ActionURL")
        concatenateLabKeyJsFile("Ajax")
        concatenateLabKeyJsFile("Filter")
        concatenateLabKeyJsFile("Message")
        concatenateLabKeyJsFile("Query")
        concatenateLabKeyJsFile("Report")
        concatenateLabKeyJsFile("Security")
        concatenateLabKeyJsFile("FieldKey")
        concatenateLabKeyJsFile("Utils")
    }

    // create combined Ext.js usable by the core module's server-side scripts
    private void concatenateExt3JsFiles()
    {

        File ext3SrcDir = project.project(':server:api').file("webapp/${project.labkey.ext3Dir}/src")
        if (ext3SrcDir.exists())
        {
            ant.concat(destFile: "${scriptsDir}/Ext.js", force: true)
                    {
                        header(file: "${scriptFragmentsDir}/Ext.header.js")
                        fileset(file: new File(ext3SrcDir, "Ext.js"))
                        fileset(file: "${scriptFragmentsDir}/Ext.middle.js")
                        fileset(file: new File(ext3SrcDir, "Observable.js"))
                        fileset(file: new File(ext3SrcDir, "JSON.js"))
                        fileset(file: new File(ext3SrcDir, "Connection.js"))
                        fileset(file: new File(ext3SrcDir, "Format.js"))
                        footer(file: "${scriptFragmentsDir}/Ext.footer.js")
                    }
        }
    }

    // create a combined Ext4.js usable by the core module's server-side scripts
    private void concatenateExt4JsFiles()
    {
        File ext4SrcDir = project.project(':server:api').file("webapp/${project.labkey.ext4Dir}/src")
        if (ext4SrcDir.exists())
        {
            ant.concat(destFile: "${scriptsDir}/Ext4.js", force: true)
                    {
                        header(file: "${scriptFragmentsDir}/Ext4.header.js")
                        fileset(file: new File(ext4SrcDir, "Ext.js"))
                        fileset(file: new File(ext4SrcDir, "lang/Array.js"))
                        fileset(file: new File(ext4SrcDir, "lang/Date.js"))
                        fileset(file: new File(ext4SrcDir, "lang/Number.js"))
                        fileset(file: new File(ext4SrcDir, "lang/Object.js"))
                        fileset(file: new File(ext4SrcDir, "lang/String.js"))
                        fileset(file: new File(ext4SrcDir, "lang/Error.js"))
                        fileset(file: "${scriptFragmentsDir}/Ext4.middle.js")
                        fileset(file: new File(ext4SrcDir, "misc/JSON.js"))
                        footer(file: "${scriptFragmentsDir}/Ext4.footer.js")
                    }
        }

    }

    private void concatenateLabKeyJsFile(String baseName)
    {
        ant.concat(destFile: "${scriptsDir}/labkey/${baseName}.js", force: true)
                {
                    header(file: "${scriptFragmentsDir}/labkey/${baseName}.header.js")
                    fileset(file: project.project(':server:api').file("webapp/clientapi/core/${baseName}.js"))
                    footer(file: "${scriptFragmentsDir}/labkey/${baseName}.footer.js")
                }
    }
}
