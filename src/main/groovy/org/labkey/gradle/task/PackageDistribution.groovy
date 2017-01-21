package org.labkey.gradle.task

import org.apache.commons.lang3.SystemUtils
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.file.CopySpec
import org.gradle.process.ExecSpec
import org.gradle.api.tasks.TaskAction

import org.labkey.gradle.plugin.DistributionExtension
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

    def String basedir
    def boolean buildInstallerExes
    def String installerBuildDir
    def String installerDir
    def String productVersion
    def String vcsRevision
    def String versionPrefix

    private void init()
    {
        if (System.hasProperty("teamcity.product.version")) {
            productVersion = System.getProperty("teamcity.product.version")
        }
        else {
            productVersion = "${project.labkeyVersion}"
        }


        if (project.hasProperty('teamcity') && (project.teamcity['env.BUILD_NUMBER'])) {
            vcsRevision = project.teamcity['env.BUILD_NUMBER']
        }
        else if (project.hasProperty("includeVcs")) {
            vcsRevision = project.versioning.info.commit
        }
        else {
            vcsRevision = "NotFromSVN"
        }

        String extraFileIdentifier = project.dist.extraFileIdentifier || ""
        project.dist.labkeyInstallerVersion = "${productVersion}-${vcsRevision}"
        versionPrefix = "Labkey${project.dist.labkeyInstallerVersion}${extraFileIdentifier}"
    }

    @TaskAction
    void action()
    {
        project.dist.dir = "${project.rootProject.projectDir}/dist"
        basedir = "${project.rootProject.projectDir}/server/installer"
        buildInstallerExes = project.dist.skipWindowsInstaller != null ? project.dist.skipWindowsInstaller : true
        installerBuildDir = "${project.rootProject.projectDir}/build/installer"
        installerDir = "${project.dist.dir}/${project.dist.subDirName}"

        init()

        setUpModuleDistDirectories()

        // TODO enum would be better for these types
        if ("modules".equalsIgnoreCase(project.dist.type))
        {
            writeDistributionFile()
            gatherBootstrapJar()
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

    private void gatherBootstrapJar()
    {
        // TODO when converted to Gradle, we should be able to eliminate this copy
        project.copy { CopySpec copy ->
            copy.from project.project(":server:bootstrap").tasks.jar
            copy.into project.rootProject.buildDir
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

    private void setAntPropertiesForInstaller()
    {
        // TODO should we log errors if the subDirName or extraFileIdentifier is missing?
        if (project.dist.subDirName != null)
            ant.properties['dist_sub.dir'] = project.dist.subDirName
        if (project.dist.extraFileIdentifier != null)
            ant.properties['extraFileIdentifier'] = project.dist.extraFileIdentifier
        ant.properties['project.root'] = project.rootDir
        if (project.dist.skipWindowsInstaller != null)
            ant.properties['skip.windowsInstaller'] = project.dist.skipWindowsInstaller
        if (project.dist.skipZipDistribution != null)
            ant.properties['skip.zipDistribution'] = project.dist.skipZipDistribution
        if (project.dist.skipTarGZDistribution != null)
            ant.properties['skip.tarGZDistribution'] = project.dist.skipTarGZDistribution
        if (project.dist.versionPrefix != null)
            ant.properties['versionPrefix'] = project.dist.versionPrefix
        if (project.dist.includeMassSpecBinaries != null)
            ant.properties['includeMassSpecBinaries'] = project.dist.includeMassSpecBinaries
    }

    private void prepare()
    {
        Properties copyProps = new Properties()
        copyProps.put("jdbcURL", "jdbc:postgresql://localhost/labkey")
        copyProps.put("jdbcDriverClassName", "org.postgresql.Driver")

        project.mkdir(project.file(installerDir).getAbsolutePath())
        project.mkdir(project.file(installerBuildDir).getAbsolutePath())

        project.copy({ CopySpec copy ->
            copy.from("${project.rootProject.projectDir}/webapps")
            copy.include("labkey.xml")
            copy.into("${project.buildDir}")
            copy.filter({ String line ->
                return PropertiesUtils.replaceProps(line, copyProps);
            })
        })


    }

    private void packageRedistributables()
    {
        prepare()
        packageInstallers()
        packageArchives()
    }

    private void packageInstallers()
    {
        if (buildInstallerExes) {
            String scriptName = "labkey_installer.nsi"
            String scriptPath = "${basedir}/${scriptName}"
            String nsisBasedir = "${basedir}/nsis2.46"

            if (SystemUtils.IS_OS_WINDOWS) {
                project.exec({ ExecSpec spec ->
                    spec.commandLine "${nsisBasedir}/makensis.exe"
                    spec.args = [
                            "/DPRODUCT_VERSION=\"${productVersion}\"",
                            "/DPRODUCT_REVISION=\"${vcsRevision}\"",
                            "${scriptPath}"
                    ]
                })
            }
            if (SystemUtils.IS_OS_UNIX) {
                project.exec({ ExecSpec spec ->
                    spec.commandLine "${nsisBasedir}/makensis.exe"
                    spec.args = [
                            "-DPRODUCT_VERSION=\"${productVersion}\"",
                            "-DPRODUCT_REVISION=\"${vcsRevision}\"",
                            "${scriptPath}"
                    ]
                })
            }

            project.copy({ CopySpec copy ->
                copy.from("${installerBuildDir}/")
                copy.include("Setup_includeJRE.exe")
                copy.into("${installerDir}/")
                copy.rename("Setup_includeJRE.exe", "${project.dist.versionPrefix}-Setup.exe")
            })
        }
    }

    private void packageArchives()
    {
        //copy_manual_upgrade_script
        project.copy({CopySpec copy ->
            copy.from("${basedir}/archivedata/")
            copy.include "manual-update.sh"
            into "${installerBuildDir}"
        })
        if(project.dist.skipTarGZDistribution == null || !project.dist.skipTarGZDistribution) {
            tarArchives()
        }
        zipArchives()
    }

    private void tarArchives()
    {
        ant.tar(tarfile:"${installerDir}/${versionPrefix}-bin.tar.gz",
                longfile: "gnu",
                compression: "gzip" ) {
            tarfileset(dir: "${project.rootProject.buildDir}/staging/labkeyWebapp",
                   prefix:"${versionPrefix}-bin/labkeywebapp") {
                exclude(name: "WEB-INF/classes/distribution")
            }
            tarfileset(dir: "${project.rootProject.buildDir}/distModules",
                    prefix: "${versionPrefix}-bin/modules") {
                include(name: "*.module")
            }
            tarfileset(dir: "${project.rootProject.buildDir}/distExtra",
                    prefix: "${versionPrefix}-bin/") {
                include(name:"**/*")
            }
            tarfileset(dir: "${project.rootProject.projectDir}/external/lib/tomcat",
                    prefix: "${versionPrefix}-bin/tomcat-lib") {
                include(name:"*.jar")
            }
            if (project.dist.includeMassSpecBinaries) {
                tarfileset(dir: "${project.rootProject.projectDir}/external/windows/msinspect",
                        prefix: "${versionPrefix}-bin/bin") {
                    include(name: "**/*.jar")
                    exclude(name: "**/.svn")
                }
            }
            tarfileset(dir: "${project.rootProject.buildDir}/",
                    prefix: "${versionPrefix}-bin/tomcat-lib") {
                include(name:"labkeyBootstrap.jar")
            }
            tarfileset(dir: "${project.rootProject.buildDir}/deploy/pipelineLib",
                    prefix: "${versionPrefix}-bin/pipeline-lib") {
            }
            tarfileset(dir: "${project.buildDir}",
                    prefix: "${versionPrefix}-bin") {
                include(name:"manual-upgrade.sh")
            }
            tarfileset(dir: "${basedir}/archiveData",
                    prefix: "${versionPrefix}-bin") {
                include(name:"README.txt")
            }
            tarfileset(dir: "${installerBuildDir}",
                    prefix: "${versionPrefix}-bin") {
                include(name:"VERSION")
            }
            tarfileset(dir: "${installerBuildDir}",
                    prefix: "${versionPrefix}-bin") {
                include(name:"labkey.xml")
            }
        }
    }

    private void zipArchives()
    {
        ant.zip(destfile: "${installerDir}/${versionPrefix}-bin.zip") {
            zipfileset(dir:"${project.rootProject.buildDir}/staging/labkeyWebapp",
                    prefix: "${versionPrefix}-bin/labkeywebapp") {
                exclude(name:"WEB-INF/classes/distribution")
            }
            zipfileset(dir:"${project.rootProject.buildDir}/distModules",
                    prefix: "${versionPrefix}-bin/modules") {
                include(name:"*.module")
            }
            zipfileset(dir:"${project.rootProject.buildDir}/distExtra",
                    prefix: "${versionPrefix}-bin/") {
                include(name:"**/*")
            }
            zipfileset(dir:"${project.rootProject.projectDir}/external/lib/tomcat",
                    prefix: "${versionPrefix}-bin/tomcat-lib") {
                include(name:"*.jar")
            }
            zipfileset(dir:"${project.rootProject.buildDir}/",
                    prefix: "${versionPrefix}-bin/tomcat-lib") {
                include(name:"labkeyBootstrap.jar")
            }
            zipfileset(dir:"${project.rootProject.buildDir}/deploy/pipelineLib",
                    prefix: "${versionPrefix}-bin/pipeline-lib")
            zipfileset(dir:"${project.rootProject.projectDir}/external/windows/core",
                    prefix: "${versionPrefix}-bin/bin") {
                include(name:"**/*")
                exclude(name:"**/.svn")
            }

            if (project.dist.includeMassSpecBinaries) {
                zipfileset(dir:"${project.rootProject.projectDir}/external/windows/tpp",
                        prefix: "${versionPrefix}-bin/bin") {
                    exclude(name:"**/.svn")
                    include(name:"**/*")
                }
                zipfileset(dir:"${project.rootProject.projectDir}/external/windows/comet",
                        prefix: "${versionPrefix}-bin/bin") {
                    exclude(name:"**/.svn")
                    include(name:"**/*")
                }
                zipfileset(dir:"${project.rootProject.projectDir}/external/windows/msinspect",
                        prefix: "${versionPrefix}-bin/bin") {
                    exclude(name:"**/.svn")
                    include(name:"**/*")
                }
                zipfileset(dir:"${project.rootProject.projectDir}/external/windows/labkey",
                        prefix: "${versionPrefix}-bin/bin") {
                    exclude(name:"**/.svn")
                    include(name:"**/*")
                }
                zipfileset(dir:"${project.rootProject.projectDir}/external/windows/pwiz",
                        prefix: "${versionPrefix}-bin/bin") {
                    exclude(name:"**/.svn")
                    include(name:"**/*")
                }
            }

            zipfileset(dir:"${basedir}/archivedata/",
                    prefix: "${versionPrefix}-bin") {
                include(name: "README.txt")
            }
            zipfileset(dir:"${installerBuildDir}/",
                    prefix: "${versionPrefix}-bin") {
                include(name:"VERSION")
            }
            zipfileset(dir:"${installerBuildDir}/",
                    prefix: "${versionPrefix}-bin") {
                include(name:"labkey.xml")
            }
        }
    }

    private void packageSource()
    {
        prepare()
        setAntPropertiesForInstaller()
        FileTree srcFileTree = project.fileTree("${project.rootProject.projectDir}") {
            exclude "**/.svn/**"
            exclude "**/**.old"
            exclude "build/**"
            exclude "remoteAPI/axis-1_4/**"
            exclude "dist/**"
            exclude "**/.gwt-cache/**"
            exclude "intellijBuild/**"
            exclude "archive/**"
            exclude "docs/**"
            exclude "external/lib/**/*.zip"
            exclude "external/lib/**/junit-src.*.jar"
            exclude "external/lib/client/**"
            exclude "server/installer/3rdparty/**"
            exclude "server/installer/nsis/**"
            exclude "sampledata/**"
            exclude "server/test/lib/**.zip"
            exclude "server/test/selenium.log"
            exclude "server/test/selenium.log.lck"
            exclude "server/test/remainingTests.txt"
            exclude "server/config.properties"
            exclude "server/LabKey.iws"
            exclude "webapps/CPL/**"
            exclude "server/api/webapp/ext-3.4.1/src/**"
            exclude ".gradle/**"
        }
        ant.zip(destfile: "${project.dist.dir}/LabKey${project.dist.labkeyInstallerVersion}-src.zip") {
            srcFileTree.addToAntBuilder(ant, 'zipfileset', FileCollection.AntType.FileSet)
        }
        ant.tar(destfile: "${project.dist.dir}/LabKey${project.dist.labkeyInstallerVersion}-src.tar.gz",
                longfile:"gnu",
                compression: "gzip") {
            srcFileTree.addToAntBuilder(ant, 'zipfileset', FileCollection.AntType.FileSet)
        }
    }

    private void packagePipelineConfigs()
    {
        prepare()
        setAntPropertiesForInstaller()
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
                exclude("**/*.bat")
                exclude("**/*.exe")
            }
            tarfileset(dir: "${project.rootProject.projectDir}/server/configs/config-cluster",
                    prefix: "cluster")
            tarfileset(dir: "${project.rootProject.projectDir}/server/configs/config-webserver",
                    prefix: "webserver")
        }
    }

    private void packageClientApis()
    {
        prepare()

        setAntPropertiesForInstaller()

        println("Packaging up Client APIs")

        GString apidocsBuildDir = "${project.rootProject.projectDir}/build/client-api/javascript/docs"
        GString xsddocsBuildDir = "${project.rootProject.projectDir}/build/client-api/xml-schemas/docs"

        // zipClientApi
        project.mkdir(project.file("${project.dist.dir}/client-api/javascript"))
        ant.zip(destfile: "${project.dist.dir}/client-api/javascript/LabKey$project.dist.labkeyInstallerVersion}-ClientAPI-JavaScript-Docs.zip"){
            zipfileset(dir: apidocsBuildDir,
                    prefix: "LabKey${project.dist.labkeyInstallerVersion}-ClientAPI-JavaScript-Docs")
        }

        // zipXsdDoc
        project.mkdir(project.file("${project.dist.dir}/client-api/XML"))
        ant.zip(destfile: "${project.dist.dir}/client-api/XML/LabKey${project.dist.labkeyInstallerVersion}-ClientAPI-XMLSchema-Docs.zip") {
            zipfileset(dir: xsddocsBuildDir,
                    prefix: "LabKey${project.dist.labkeyInstallerVersion}-ClientAPI-XMLSchema-Docs")
        }

        //zipTeamCityClientApi
        ant.zip(destfile: "${project.dist.dir}/TeamCity-ClientAPI-JavaScript-Docs.zip") {
            zipfileset(dir: apidocsBuildDir)
        }

        //zipTeamCityXsdDocs
        ant.zip(destfile: "${project.dist.dir}/TeamCity-ClientAPI-XMLSchema-Docs.zip") {
            zipfileset(dir: xsddocsBuildDir)
        }

        //tarTeamCityClientApi
        ant.tar(destfile: "${project.dist.dir}/client-api/javascript/LabKey${project.dist.labkeyInstallerVersion}-ClientAPI-JavaScript-Docs.tar.gz",
                longfile:"gnu",
                compression: "gzip") {
            tarfileset(dir: apidocsBuildDir,
                    prefix: "LabKey${project.dist.labkeyInstallerVersion}-ClientAPI-JavaScript-Docs")
        }

        //tarTeamCityXsdDocs
        ant.tar(destfile: "${project.dist.dir}/client-api/XML/LabKey${project.dist.labkeyInstallerVersion}-ClientAPI-XMLSchema-Docs.tar.gz",
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
