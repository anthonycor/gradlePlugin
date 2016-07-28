package org.labkey.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/**
 * Created by susanh on 7/27/16.
 */
class ClientLibsCompress extends DefaultTask
{
    @Input
    File libXmlFile

    @OutputDirectory
    File sourceDir = new File("${project.labkey.explodedModuleDir}/web")

    private static final String YUI_COMPRESSOR = "yuicompressor-2.4.8a.jar"


    @TaskAction
    def compress()
    {
        def libXml
        def sourceFile = new File(this.sourceDir, libXmlFile.getName())
        // TODO get rid of this in favor of porting the ClientLibraryBuilder class to Gradle/groovy
        ant.taskdef(
            name: "clientLibraryBuilder",
            classname: "labkey.ant.ClientLibraryBuilder",
            classpath: "${project.labkey.externalDir}/ant/labkeytasks/labkeytasks.jar"
        )
        ant.clientLibraryBuilder(
                srcfile: sourceFile.getAbsolutePath(),
                sourcedir: this.sourceDir,
                yuicompressorjar: "${project.labkey.externalLibDir}/build/${YUI_COMPRESSOR}"
        )

    }
}
