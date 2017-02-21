package org.labkey.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.TaskAction

class SourceDistribution extends DefaultTask
{
    @TaskAction
    void doAction()
    {
        FileTree srcFileTree = project.fileTree("${project.rootProject.projectDir}") {
            exclude "**/.svn/**"
            exclude "**/**.old"
            exclude "buildSrc/**"
            exclude "**/.idea/modules/**"
            exclude "build/**"
            exclude "remoteAPI/axis-1_4/**"
            exclude "**/dist/**"
            exclude "**/.gwt-cache/**"
            exclude "**/intellijBuild/**"
            exclude "archive/**"
            exclude "docs/**"
            exclude "external/lib/**/*.zip"
            exclude "external/lib/**/junit-src.*.jar"
            exclude "external/lib/client/**"
            exclude "server/installer/3rdparty/**"
            exclude "server/installer/nsis*/**"
            exclude "sampledata/**"
            exclude "server/test/lib/**.zip"
            exclude "server/test/selenium.log"
            exclude "server/test/selenium.log.lck"
            exclude "server/test/remainingTests.txt"
            exclude "server/config.properties"
            exclude "server/LabKey.iws"
            exclude "server/gradlew" // include separately to ensure permissions set correctly.
            exclude "webapps/CPL/**"
            exclude "server/api/webapp/ext-3.4.1/src/**"
            exclude "**/.gradle/**"
            exclude ".gradle/**"
        }
        ant.zip(destfile: "${project.dist.dir}/LabKey${project.installerVersion}-src.zip") {
            srcFileTree.addToAntBuilder(ant, 'zipfileset', FileCollection.AntType.FileSet)
            zipfileset(file: "${project.rootProject.projectDir}/server/gradlew", prefix: "server", filemode: 755)
        }
        ant.tar(destfile: "${project.dist.dir}/LabKey${project.installerVersion}-src.tar.gz",
                longfile:"gnu",
                compression: "gzip") {
            srcFileTree.addToAntBuilder(ant, 'tarfileset', FileCollection.AntType.FileSet)
            tarfileset(file: "${project.rootProject.projectDir}/server/gradlew", prefix: "server", filemode: 755)
        }
    }
}
