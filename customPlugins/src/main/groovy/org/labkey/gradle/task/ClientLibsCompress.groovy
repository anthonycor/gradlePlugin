package org.labkey.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.TaskAction

class ClientLibsCompress extends DefaultTask
{
    private static final String LIB_XML_EXTENSION = ".lib.xml"

    private static final String YUI_COMPRESSOR = "yuicompressor-2.4.8a.jar"

    public ClientLibsCompress()
    {
        // TODO get rid of this in favor of porting the ClientLibraryBuilder class to Gradle
        ant.taskdef(
                name: "clientLibraryBuilder",
                classname: "labkey.ant.ClientLibraryBuilder",
                classpath: "${project.labkey.externalDir}/ant/labkeytasks/labkeytasks.jar"
        )
    }

    @TaskAction
    def compress()
    {
        def FileTree libXmlFiles = project.fileTree(dir: project.clientLibs.libXmlParentDirectory,
                includes: ["**/*${LIB_XML_EXTENSION}"]
        )
        libXmlFiles.files.each() {
            def file ->
                def inputDirPrefix = project.clientLibs.libXmlParentDirectory
                def pathSuffix = file.getPath().substring(inputDirPrefix.getPath().length())
                def sourceFile = new File(project.clientLibs.outputDir, pathSuffix)
                ant.clientLibraryBuilder(
                        srcFile: sourceFile.getAbsolutePath(),
                        sourcedir: project.clientLibs.outputDir,
                        yuicompressorjar: "${project.labkey.externalLibDir}/build/${YUI_COMPRESSOR}"
                )
        }
    }
}
