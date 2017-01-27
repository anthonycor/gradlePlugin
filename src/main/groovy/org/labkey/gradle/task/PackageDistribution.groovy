package org.labkey.gradle.task

import org.apache.commons.lang3.SystemUtils
import org.gradle.api.DefaultTask
import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecSpec
import org.labkey.gradle.plugin.DistributionExtension
import org.labkey.gradle.util.BuildUtils
import org.labkey.gradle.util.PropertiesUtils

import java.nio.file.Files
import java.nio.file.Paths

class PackageDistribution extends DefaultTask
{
    public static final String ALL_DISTRIBUTIONS = "all"

    public static final String[] STANDARD_MODULES = [
            ':server:internal',
            ':server:api',
            ':server:modules:announcements',
            ':server:modules:audit',
            ':server:modules:bigiron',
            ':server:modules:core',
            ':server:modules:dataintegration',
            ':server:modules:elisa',
            ':server:modules:elispotassay',
            ':server:modules:experiment',
            ':server:customModules:fcsexpress',
            ':server:modules:filecontent',
            ':server:modules:flow',
            ':server:modules:issues',
            ':server:modules:list',
            ':server:modules:luminex',
            ':server:modules:microarray',
            ':server:modules:ms1',
            ':server:modules:ms2',
            ':server:modules:nab',
            ':server:modules:pipeline',
            ':server:modules:query',
            ':server:modules:search',
            ':server:modules:study',
            ':server:modules:survey',
            ':server:customModules:targetedms',
            ':server:modules:visualization',
            ':server:modules:wiki'
    ]

    String baseDir
    boolean buildInstallerExes

    // TODO would like to declare this as the output directory but need to supply a value on initialization.
    // Need to figure out order of initialization of extension and task.
    File distributionDir

    String vcsRevision
    String versionPrefix
    String binPrefix

    private void init()
    {
        vcsRevision = BuildUtils.getStandardVCSProperties(project).getProperty("VcsRevision")

        project.dist.labkeyInstallerVersion = "${project.version}-${vcsRevision}"
        versionPrefix = "Labkey${project.dist.labkeyInstallerVersion}${project.dist.extraFileIdentifier}"
        binPrefix = "${versionPrefix}-bin"
    }

    @TaskAction
    void action()
    {
        baseDir = "${project.rootProject.projectDir}/server/installer"
        buildInstallerExes = project.dist.skipWindowsInstaller != null ? project.dist.skipWindowsInstaller : true

        distributionDir = project.file("${project.dist.dir}/${project.dist.subDirName}")

        init()

        setUpModuleDistDirectories()

        // TODO create separate tasks/plugins for these that are included separately so we get rid of this checking
        // and can declare proper inputs and outputs
        if ("modules".equalsIgnoreCase(project.dist.type))
        {
            writeDistributionFile()
            gatherModules()
            packageRedistributables()
        }
        else if ("source".equalsIgnoreCase(project.dist.type))
        {
            packageSource()
        }
        else if ("pipelineConfigs".equalsIgnoreCase(project.dist.type))
        {
            packagePipelineConfigs()
        }
        else if ("clientApis".equalsIgnoreCase(project.dist.type))
        {
            packageClientApis()
        }
    }

    private void gatherModules()
    {
        ant.copy (
                toDir: project.dist.distModulesDir
        )
                {
                    project.configurations.distribution.each {
                        File moduleFile ->
                            file(name: moduleFile.getPath())
                    }
                }
    }

    private void packageRedistributables()
    {
        project.mkdir(project.file(distributionDir).getAbsolutePath())

        copyLibXml()
        packageInstallers()
        packageArchives()
    }

    private void copyLibXml()
    {
        Properties copyProps = new Properties()
        //The Windows installer only supports Postgres, which it also installs.
        copyProps.put("jdbcURL", "jdbc:postgresql://localhost/labkey")
        copyProps.put("jdbcDriverClassName", "org.postgresql.Driver")

        project.copy({ CopySpec copy ->
            copy.from("${project.rootProject.projectDir}/webapps")
            copy.include("labkey.xml")
            copy.into(project.buildDir)
            copy.filter({ String line ->
                return PropertiesUtils.replaceProps(line, copyProps);
            })
        })
    }

    private void packageInstallers()
    {
        if (buildInstallerExes && SystemUtils.IS_OS_WINDOWS) {
            String scriptName = "labkey_installer.nsi"
            String scriptPath = "${baseDir}/${scriptName}"
            String nsisBasedir = "${baseDir}/nsis2.46"

            project.exec({ ExecSpec spec ->
                spec.commandLine "${nsisBasedir}/makensis.exe"
                spec.args = [
                        "/DPRODUCT_VERSION=\"${project.version}\"",
                        "/DPRODUCT_REVISION=\"${vcsRevision}\"",
                        "${scriptPath}"
                ]
            })

            project.copy({ CopySpec copy ->
                copy.from("${project.buildDir}/")
                copy.include("Setup_includeJRE.exe")
                copy.into("${distributionDir}/")
                copy.rename("Setup_includeJRE.exe", "${project.dist.versionPrefix}-Setup.exe")
            })
        }
    }

    private void packageArchives()
    {
        if (project.dist.skipTarGZDistribution == null || !project.dist.skipTarGZDistribution)
        {
            tarArchives()
        }
        if (project.dist.skipZipDistribution == null || !project.dist.skipZipDistribution)
        {
            zipArchives()
        }
    }

    private void tarArchives()
    {
        ant.tar(tarfile:"${distributionDir}/${binPrefix}.tar.gz",
                longfile: "gnu",
                compression: "gzip" ) {
            tarfileset(dir: "${project.rootProject.buildDir}/staging/labkeyWebapp",
                   prefix:"${binPrefix}/labkeywebapp") {
                exclude(name: "WEB-INF/classes/distribution")
            }
            tarfileset(dir: "${project.rootProject.buildDir}/distModules",
                    prefix: "${binPrefix}/modules") {
                include(name: "*.module")
            }
            tarfileset(dir: "${project.rootProject.buildDir}/distExtra",
                    prefix: "${binPrefix}/") {
                include(name:"**/*")
            }
            project.project(":server").configurations.tomcatJars.getFiles().collect({
                tomcatJar ->
                    tarfileset(file: tomcatJar.path,
                            prefix: "${binPrefix}/tomcat-lib")
            })
            if (project.dist.includeMassSpecBinaries) {
                tarfileset(dir: "${project.rootProject.projectDir}/external/windows/msinspect",
                        prefix: "${binPrefix}/bin") {
                    include(name: "**/*.jar")
                    exclude(name: "**/.svn")
                }
            }
            tarfileset(file: project.project(":server:bootstrap").tasks.jar.outputs.getFiles().asPath,
                    prefix: "${binPrefix}/tomcat-lib/")

            tarfileset(dir: "${project.rootProject.buildDir}/deploy/pipelineLib",
                    prefix: "${binPrefix}/pipeline-lib") {
            }

            tarfileset(dir: "${baseDir}/archivedata/",
                    prefix: "${binPrefix}") {
                include(name:"manual-upgrade.sh")
            }
            tarfileset(dir: "${baseDir}/archiveData",
                    prefix: "${binPrefix}") {
                include(name:"README.txt")
            }
            tarfileset(dir: "${project.buildDir}",
                    prefix: "${binPrefix}") {
                include(name:"VERSION")
            }
            tarfileset(dir: "${project.buildDir}",
                    prefix: "${binPrefix}") {
                include(name:"labkey.xml")
            }
        }
    }

    private void zipArchives()
    {
        ant.zip(destfile: "${distributionDir}/${binPrefix}.zip") {
            zipfileset(dir:"${project.rootProject.buildDir}/staging/labkeyWebapp",
                    prefix: "${binPrefix}/labkeywebapp") {
                exclude(name:"WEB-INF/classes/distribution")
            }
            zipfileset(dir:"${project.rootProject.buildDir}/distModules",
                    prefix: "${binPrefix}/modules") {
                include(name:"*.module")
            }
            zipfileset(dir:"${project.rootProject.buildDir}/distExtra",
                    prefix: "${binPrefix}/") {
                include(name:"**/*")
            }
            project.project(":server").configurations.tomcatJars.getFiles().collect({
                tomcatJar ->
                    zipfileset(file: tomcatJar.path,
                            prefix: "${binPrefix}/tomcat-lib")
            })
            zipfileset(file: project.project(":server:bootstrap").tasks.jar.outputs.getFiles().asPath,
                    prefix: "${binPrefix}/tomcat-lib/")
            zipfileset(dir:"${project.rootProject.buildDir}/deploy/pipelineLib",
                    prefix: "${binPrefix}/pipeline-lib")
            zipfileset(dir:"${project.rootProject.projectDir}/external/windows/core",
                    prefix: "${binPrefix}/bin") {
                include(name:"**/*")
                exclude(name:"**/.svn")
            }

            if (project.dist.includeMassSpecBinaries) {
                zipfileset(dir:"${project.rootProject.projectDir}/external/windows/",
                        prefix: "${binPrefix}/bin") {
                    exclude(name:"**/.svn")
                    include(name:"tpp/**/*")
                    include(name:"comet/**/*")
                    include(name:"msinspect/**/*")
                    include(name:"labkey/**/*")
                    include(name:"pwiz/**/*")
                }
            }

            zipfileset(dir:"${baseDir}/archivedata/",
                    prefix: "${binPrefix}") {
                include(name: "README.txt")
            }
            zipfileset(dir:"${project.buildDir}/",
                    prefix: "${binPrefix}") {
                include(name:"VERSION")
            }
            zipfileset(dir:"${project.buildDir}/",
                    prefix: "${binPrefix}") {
                include(name:"labkey.xml")
            }
        }
    }

    private void packageSource()
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
        ant.zip(destfile: "${project.dist.dir}/LabKey${project.dist.labkeyInstallerVersion}-src.zip") {
            srcFileTree.addToAntBuilder(ant, 'zipfileset', FileCollection.AntType.FileSet)
            zipfileset(file: "${project.rootProject.projectDir}/server/gradlew", prefix: "server", filemode: 755)
        }
        ant.tar(destfile: "${project.dist.dir}/LabKey${project.dist.labkeyInstallerVersion}-src.tar.gz",
                longfile:"gnu",
                compression: "gzip") {
            srcFileTree.addToAntBuilder(ant, 'tarfileset', FileCollection.AntType.FileSet)
            tarfileset(file: "${project.rootProject.projectDir}/server/gradlew", prefix: "server", filemode: 755)
        }
    }

    private void packagePipelineConfigs()
    {
        ant.zip(destfile: "${project.dist.dir}/LabKey${project.dist.labkeyInstallerVersion}-PipelineConfig.zip") {
            zipfileset(dir: "${project.rootProject.projectDir}/server/configs/config-remote",
                    prefix: "remote")
            zipfileset(dir: "${project.rootProject.projectDir}/server/configs/config-cluster",
                    prefix: "cluster")
            zipfileset(dir: "${project.rootProject.projectDir}/server/configs/config-webserver",
                    prefix: "webserver")
        }
        ant.tar(destfile: "${project.dist.dir}/LabKey${project.dist.labkeyInstallerVersion}-PipelineConfig.tar.gz",
                longfile:"gnu",
                compression: "gzip") {
            tarfileset(dir: "${project.rootProject.projectDir}/server/configs/config-remote",
                    prefix: "remote") {
                exclude(name: "**/*.bat")
                exclude(name: "**/*.exe")
            }
            tarfileset(dir: "${project.rootProject.projectDir}/server/configs/config-cluster",
                    prefix: "cluster")
            tarfileset(dir: "${project.rootProject.projectDir}/server/configs/config-webserver",
                    prefix: "webserver")
        }
    }

    private void packageClientApis()
    {
        String javaDir = "${project.dist.dir}/client-api/java"
        String javascriptDir = "${project.dist.dir}/client-api/javascript"
        String xmlDir = "${project.dist.dir}/client-api/XML"

        project.mkdir(project.file(javaDir))
        project.copy({CopySpec copy ->
            copy.from "${project.project(":remoteapi:java").tasks.fatJar.outputs.getFiles().asPath}"
            copy.into javaDir
        })
        ant.zip(destfile: "${javaDir}/LabKey${project.labkeyVersion}-ClientAPI-Java.zip") {
            zipfileset(dir: project.project(":remoteapi:java").tasks.javadoc.destinationDir,
                    prefix: "doc")
            zipfileset(dir: "${project.project(":remoteapi:java").projectDir}/lib",
                    prefix: "lib/"){
                include(name:"*.jar")
            }
            zipfileset(file:"${project.project(":remoteapi:java").projectDir}/README.html")
            zipfileset(file:"${project.project(":remoteapi:java").tasks.fatJar.outputs.getFiles().asPath}")
        }

        ant.zip(destfile: "${javaDir}/LabKey${project.labkeyVersion}-ClientAPI-Java-src.zip") {
            zipfileset(dir: "${project.project(":remoteapi:java").projectDir}/src")
        }

        ant.zip(destfile:"${project.dist.dir}/TeamCity-ClientAPI-Java-Docs.zip") {
            zipfileset(dir: project.project(":remoteapi:java").tasks.javadoc.destinationDir)
        }

        String apidocsBuildDir = project.project(":server").tasks.jsdoc.outputs.getFiles().asPath
        String xsddocsBuildDir = project.project(":server").tasks.xsddoc.outputs.getFiles().asPath

        project.mkdir(project.file(javascriptDir))
        ant.zip(destfile: "${javascriptDir}/LabKey${project.dist.labkeyInstallerVersion}-ClientAPI-JavaScript-Docs.zip"){
            zipfileset(dir: apidocsBuildDir,
                    prefix: "LabKey${project.dist.labkeyInstallerVersion}-ClientAPI-JavaScript-Docs")
        }

        project.mkdir(project.file(xmlDir))
        ant.zip(destfile: "${xmlDir}/LabKey${project.dist.labkeyInstallerVersion}-ClientAPI-XMLSchema-Docs.zip") {
            zipfileset(dir: xsddocsBuildDir,
                    prefix: "LabKey${project.dist.labkeyInstallerVersion}-ClientAPI-XMLSchema-Docs")
        }

        //Create a stable file name so that TeamCity can serve it up directly through its own UI
        ant.zip(destfile: "${project.dist.dir}/TeamCity-ClientAPI-JavaScript-Docs.zip") {
            zipfileset(dir: apidocsBuildDir)
        }

        //Create a stable file name so that TeamCity can serve it up directly through its own UI
        ant.zip(destfile: "${project.dist.dir}/TeamCity-ClientAPI-XMLSchema-Docs.zip") {
            zipfileset(dir: xsddocsBuildDir)
        }

        ant.tar(destfile: "${javascriptDir}/LabKey${project.dist.labkeyInstallerVersion}-ClientAPI-JavaScript-Docs.tar.gz",
                longfile:"gnu",
                compression: "gzip") {
            tarfileset(dir: apidocsBuildDir,
                    prefix: "LabKey${project.dist.labkeyInstallerVersion}-ClientAPI-JavaScript-Docs")
        }

        ant.tar(destfile: "${xmlDir}/LabKey${project.dist.labkeyInstallerVersion}-ClientAPI-XMLSchema-Docs.tar.gz",
                longfile:"gnu",
                compression: "gzip") {
            tarfileset(dir: xsddocsBuildDir,
                    prefix: "LabKey${project.dist.labkeyInstallerVersion}-ClientAPI-XMLSchema-Docs")
        }
    }

    private void setUpModuleDistDirectories()
    {
        File distDir = new File((String) project.dist.distModulesDir)
        distDir.deleteDir()
        distDir.mkdirs()
    }

    private void writeDistributionFile()
    {
        File distExtraDir = new File(project.rootProject.buildDir, DistributionExtension.DIST_FILE_DIR)
        if (!distExtraDir.exists())
            distExtraDir.mkdirs()
        Files.write(Paths.get(distExtraDir.absolutePath, DistributionExtension.DIST_FILE_NAME), project.name.getBytes())
    }
}
