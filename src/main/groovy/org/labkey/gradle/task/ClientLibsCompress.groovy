package org.labkey.gradle.task

import org.apache.commons.io.FileUtils
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class ClientLibsCompress extends DefaultTask
{
    public static final String LIB_XML_EXTENSION = ".lib.xml"

    private static final String YUI_COMPRESSOR = "yuicompressor-2.4.8a.jar"

    File workingDir = project.clientLibs.workingDir

    @OutputFile
    File upToDateFile = new File(workingDir, ".clientlibrary.uptodate")

    ClientLibsCompress()
    {
        // TODO get rid of this in favor of porting to Gradle.
        ant.taskdef(
                name: "compressClientLibs",
                classname: "labkey.ant.ClientLibraryBuilder",
                classpath: "${project.labkey.externalDir}/ant/labkeytasks/labkeytasks.jar"
        )
    }

    @TaskAction
    def compress()
    {
        def FileTree libXmlFiles = project.fileTree(dir: workingDir,
                includes: ["**/*${LIB_XML_EXTENSION}"]
        )
        libXmlFiles.files.each() {
            def file ->
                def inputDirPrefix = workingDir
                def pathSuffix = file.getPath().substring(inputDirPrefix.getPath().length())
                def sourceFile = new File(workingDir, pathSuffix)
                ant.compressClientLibs(
                        srcFile: sourceFile.getAbsolutePath(),
                        sourcedir: workingDir,
                        yuicompressorjar: "${project.labkey.externalLibDir}/build/${YUI_COMPRESSOR}"
                )
        }

        // this file is used by the ClientLibraryBuilder to determine if things are up to date
        FileUtils.touch(upToDateFile)
    }

}
