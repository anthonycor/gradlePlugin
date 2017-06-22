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
package org.labkey.gradle.util

import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.labkey.gradle.plugin.Api
import org.labkey.gradle.plugin.Jsp
import org.labkey.gradle.plugin.ServerBootstrap
import org.labkey.gradle.plugin.XmlBeans
import org.labkey.gradle.plugin.extension.TeamCityExtension

import java.nio.file.Files
import java.nio.file.Paths
import java.util.regex.Matcher
import java.util.regex.Pattern
/**
 * Static utility methods and constants for use in the build and settings scripts.
 */
class BuildUtils
{
    public static final String BUILD_FROM_SOURCE_PROP = "buildFromSource"
    public static final String BUILD_CLIENT_LIBS_FROM_SOURCE_PROP = "buildClientLibsFromSource"
    public static final String SERVER_MODULES_DIR = "server/modules"
    public static final String CUSTOM_MODULES_DIR = "server/customModules"
    public static final String OPTIONAL_MODULES_DIR = "server/optionalModules"
    public static final String EXTERNAL_MODULES_DIR = "externalModules"

    public static final String TEST_MODULE = ":server:test"
    public static final String TEST_MODULES_DIR = "server/test/modules"

    // the set of modules required for minimal LabKey server functionality
    public static final List<String> BASE_MODULES =
            [
                    ":server:bootstrap",
                    ":server:api",
                    ":schemas",
                    ":server:internal",
                    ':remoteapi:java',
                    ":server:modules:announcements",
                    ":server:modules:audit",
                    ":server:modules:core",
                    ":server:modules:experiment",
                    ":server:modules:filecontent",
                    ":server:modules:pipeline",
                    ":server:modules:query",
                    ":server:modules:wiki"
            ]

    public static final List<String> COMMUNITY_MODULES = BASE_MODULES +
            [
                    ':server:modules:bigiron',
                    ':server:modules:dataintegration',
                    ':server:modules:elisa',
                    ':server:modules:elispotassay',
                    ':server:customModules:fcsexpress',
                    ':server:modules:flow',
                    ':server:modules:issues',
                    ':server:modules:list',
                    ':server:modules:luminex',
                    ':server:modules:microarray',
                    ':server:modules:ms1',
                    ':server:modules:ms2',
                    ':server:modules:nab',
                    ':server:modules:search',
                    ':server:modules:study',
                    ':server:modules:survey',
                    ':server:customModules:targetedms',
                    ':server:modules:visualization'
            ]

    public static final List<String> EHR_MODULE_NAMES = [
            "EHR_ComplianceDB",
            "WNPRC_EHR",
            "cnprc_ehr",
            "snprc_ehr",
            "ehr",
            "onprc_ehr",
            "tnprc_ehr"
    ]

    // a set of directory paths in which to look for module directories
    public static final List<String> SERVER_MODULE_DIRS = [SERVER_MODULES_DIR,
                                                           CUSTOM_MODULES_DIR,
                                                           OPTIONAL_MODULES_DIR
    ]

    public static final List<String> EHR_EXTERNAL_MODULE_DIRS = [
            "externalModules/labModules",
            "externalModules/onprcEHRModules",
            "externalModules/cnprcEHRModules",
            "externalModules/snprcEHRModules",
            "externalModules/tnprcEHRModules",
            "externalModules/DISCVR"
    ]

    public static final List<String> EXTERNAL_MODULE_DIRS = ["externalModules/scharp"] + EHR_EXTERNAL_MODULE_DIRS

    /**
     * This includes modules that are required for any LabKey server build (e.g., bootstrap, api, internal)
     * @param settings the settings
     */
    static void includeBaseModules(Settings settings)
    {
        includeModules(settings, BASE_MODULES)
    }

    /**
     * This includes the :server:test project as well as the modules in the server/test/modules directory
     * @param settings
     * @param rootDir root directory of the project
     */
    static void includeTestModules(Settings settings, File rootDir)
    {
        settings.include ":sampledata:qc"
        settings.include TEST_MODULE
        includeModules(settings, rootDir, [TEST_MODULES_DIR], [])
        // TODO get rid of this when we decide whether to move dumbster
        File dumbsterDir = new File(rootDir, "server/modules/dumbster")
        if (dumbsterDir.exists())
            settings.include ":server:modules:dumbster"
        else
            settings.include ":server:test:modules:dumbster"
    }

    static void includeModules(Settings settings, List<String> modules)
    {
        settings.include modules.toArray(new String[0])
    }

    /**
     * Can be used in a gradle settings file to include the projects in a particular directory.
     * @param rootDir - the root directory for the gradle build (project.rootDir)
     * @param moduleDirs - the list of directories that are parents of module directories to be included
     * @param excludedModules - a list of directory names that are to be excluded from the build configuration (e.g., movies)
     */
    static void includeModules(Settings settings, File rootDir, List<String> moduleDirs, List<String> excludedModules)
    {
        // find the directories in each of the moduleDirs that meet our selection criteria
        moduleDirs.each { String path ->
            if (path.contains("*"))
            {
                ModuleFinder finder = new ModuleFinder(rootDir, path, excludedModules)
                Files.walkFileTree(Paths.get(rootDir.getAbsolutePath()), finder)
                finder.modulePaths.each{String modulePath ->
                    settings.include modulePath
                }
            }
            else
            {
                File directory = new File(rootDir, path)
                if (directory.exists())
                {
                    String prefix = convertDirToPath(rootDir, directory)
                    settings.include directory.listFiles().findAll { File f ->
                        // exclude non-directories, explicitly excluded names, and directories beginning with a .
                        f.isDirectory() && !excludedModules.contains(f.getName()) &&  !(f.getName() =~ "^\\..*")
                    }.collect {
                        (String) "${prefix}:${it.getName()}"
                    }.toArray(new String[0])
                }
            }
        }

    }

    static String convertDirToPath(File rootDir, File directory)
    {
        String relativePath = directory.absolutePath - rootDir.absolutePath
        return  relativePath.replaceAll("[\\\\\\/]", ":")
    }

    static boolean shouldBuildFromSource(Project project)
    {
        return whyNotBuildFromSource(project, BUILD_FROM_SOURCE_PROP).isEmpty()
    }

    static List<String> whyNotBuildFromSource(Project project, String property)
    {
        List<String> reasons = []
        String propValue = project.hasProperty(property) ? project.property(property) : null
        String value = TeamCityExtension.getTeamCityProperty(project, property, propValue)
        if (value == null)
        {
            reasons.add("Project does not have ${property} property")
            if (isSvnModule(project))
                reasons.add("svn module without ${property} property set to true")
        }
        else if (!Boolean.valueOf((String) value))
            reasons.add("${property} property is false")

        return reasons
    }

    static boolean shouldBuildClientLibsFromSource(Project project)
    {
        return whyNotBuildFromSource(project, BUILD_CLIENT_LIBS_FROM_SOURCE_PROP).isEmpty()
    }

    static boolean isGitModule(Project project)
    {
        return project.file(".git").exists()
    }

    static boolean isSvnModule(Project project)
    {
        return !isGitModule(project)
    }

    static String getVersionNumber(Project project)
    {
        if (project.hasProperty("versioning"))
        {
            String branch = project.versioning.info.branchId
            if (["trunk", "master", "develop", "none"].contains(branch))
                return project.labkeyVersion
            else
            {
                String currentVersion = project.labkeyVersion
                return currentVersion.replace("-SNAPSHOT", "_${branch}-SNAPSHOT")
            }
        }
        return project.labkeyVersion
    }

    static String getLabKeyModuleVersion(Project project)
    {
        String version = project.version
        // matches to a.b.c.d_rfb_123-SNAPSHOT or a.b.c.d-SNAPSHOT
        Matcher matcher = Pattern.compile("([^_-]+)[_-].*").matcher(version)
        if (matcher.matches())
            version = matcher.group(1)
        return version
    }

    static Properties getStandardVCSProperties(project)
    {
        Properties ret = new Properties()
        if (project.plugins.hasPlugin("org.labkey.versioning"))
        {
            ret.setProperty("VcsURL", project.versioning.info.url)
            ret.setProperty("VcsRevision", project.versioning.info.commit)
            ret.setProperty("BuildNumber", (String) TeamCityExtension.getTeamCityProperty(project, "build.number", project.versioning.info.build))
        }
        else
        {
            ret.setProperty("VcsURL", "Unknown")
            ret.setProperty("VcsRevision", "Unknown")
            ret.setProperty("BuildNumber", "Unknown")
        }
        return ret
    }

    static void addLabKeyDependency(Map<String, Object> config)
    {
        if (config.get('transitive') != null)
        {
            addLabKeyDependency(
                    (Project) config.get("project"),
                    (String) config.get("config"),
                    (String) config.get("depProjectPath"),
                    (String) config.get("depProjectConfig"),
                    (String) config.get("depVersion"),
                    (String) config.get("depExtension"),
                    (Boolean) config.get('transitive'),
                    (Closure) config.get("specialParams")
            )
        }
        else
        {
            addLabKeyDependency(
                    (Project) config.get("project"),
                    (String) config.get("config"),
                    (String) config.get("depProjectPath"),
                    (String) config.get("depProjectConfig"),
                    (String) config.get("depVersion"),
                    (String) config.get("depExtension"),
                    (Closure) config.get("specialParams")
            )
        }
    }

    static void addModuleDistributionDependency(Project distributionProject, String depProjectPath, String config)
    {
        if (distributionProject.configurations.findByName(config) == null)
            distributionProject.configurations {
                config
            }
        distributionProject.logger.info("${distributionProject.path}: adding ${depProjectPath} as dependency for config ${config}")
        addLabKeyDependency(project: distributionProject, config: config, depProjectPath: depProjectPath, depProjectConfig: "published", depExtension: "module", depVersion: distributionProject.labkeyVersion)
    }

    static void addModuleDistributionDependency(Project distributionProject, String depProjectPath)
    {
        addLabKeyDependency(project: distributionProject, config: "distribution", depProjectPath: depProjectPath, depProjectConfig: "published", depExtension: "module", depVersion: distributionProject.labkeyVersion)
    }

    static void addModuleDistributionDependencies(Project distributionProject, List<String> depProjectPaths)
    {
        addModuleDistributionDependencies(distributionProject, depProjectPaths, "distribution")
    }

    static void addModuleDistributionDependencies(Project distributionProject, List<String> depProjectPaths, String config)
    {
        depProjectPaths.each{
            String path -> addModuleDistributionDependency(distributionProject, path, config)
        }
    }

    static void addLabKeyDependency(Project parentProject,
                                           String parentProjectConfig,
                                           String depProjectPath,
                                           String depProjectConfig,
                                           String depVersion,
                                           String depExtension) {
        addLabKeyDependency(parentProject, parentProjectConfig, depProjectPath, depProjectConfig, depVersion, depExtension, null)
    }

    static void addLabKeyDependency(Project parentProject,
                                           String parentProjectConfig,
                                           String depProjectPath,
                                           String depProjectConfig,
                                           String depVersion,
                                           String depExtension,
                                           Closure specialParams)
    {
       addLabKeyDependency(parentProject, parentProjectConfig, depProjectPath, depProjectConfig, depVersion, depExtension,
              !"jars".equals(parentProjectConfig),  specialParams)
    }

    static void addLabKeyDependency(Project parentProject,
                                    String parentProjectConfig,
                                    String depProjectPath,
                                    String depProjectConfig,
                                    String depVersion,
                                    String depExtension,
                                    Boolean transitive,
                                    Closure specialParams
                                    )
    {
        Project depProject = parentProject.rootProject.findProject(depProjectPath)
        if (depProject != null && shouldBuildFromSource(depProject))
        {
            parentProject.logger.info("Found project ${depProjectPath}; building ${depProjectPath} from source")
            if (depProjectConfig != null)
                parentProject.dependencies.add(parentProjectConfig, parentProject.dependencies.project(path: depProjectPath, configuration: depProjectConfig, transitive: transitive))
            else
                parentProject.dependencies.add(parentProjectConfig, parentProject.dependencies.project(path: depProjectPath, transitive: transitive))
        }
        else
        {
            if (depProject == null)
            {
                parentProject.logger.info("${depProjectPath} project not found; assumed to be external.")
                if (depVersion == null)
                    depVersion = parentProject.version
            }
            else
            {
                parentProject.logger.info("${depProjectPath} project found but not building from source because: "
                        + whyNotBuildFromSource(parentProject, BUILD_FROM_SOURCE_PROP).join("; "))
                if (depVersion == null)
                    depVersion = depProject.version
            }
            if (specialParams != null)
            {
                parentProject.dependencies.add(parentProjectConfig, getLabKeyArtifactName(depProjectPath, depProjectConfig, depVersion, depExtension), specialParams)
            }
            else
            {
                parentProject.dependencies.add(parentProjectConfig, getLabKeyArtifactName(depProjectPath, depProjectConfig, depVersion, depExtension))
            }
        }
    }

    static String getLabKeyArtifactName(String projectPath, String projectConfig, String version, String extension)
    {
        String classifier = ''
        if (projectConfig != null)
        {
            if ('apiCompile'.equals(projectConfig))
                classifier = ":${Api.CLASSIFIER}"
            else if ('xmlSchema'.equals(projectConfig))
                classifier = ":${XmlBeans.CLASSIFIER}"
            else if ('jspCompile'.equals(projectConfig))
                classifier = ":${Jsp.CLASSIFIER}"
            else if ('transformCompile'.equals(projectConfig)) // special business for CNPRC's distribution so it can include the genetics transform jar file
                classifier = ":transform"
        }

        String moduleName
        if (projectPath.endsWith("remoteapi:java"))
        {
            moduleName = "labkey-client-api"
        }
        else if (projectPath.equals(":server:bootstrap"))
        {
            moduleName = ServerBootstrap.JAR_BASE_NAME
        }
        else
        {
            int index = projectPath.lastIndexOf(":")
            moduleName = projectPath
            if (index >= 0)
                moduleName = projectPath.substring(index + 1)
        }

        String versionString = version == null ? "" : ":$version"

        String extensionString = extension == null ? "" : "@$extension"

        return "org.labkey:${moduleName}${versionString}${classifier}${extensionString}"

    }

    static String getRepositoryKey(Project project)
    {
        String repoKey = shouldPublishDistribution(project) ? "dist" : "libs"
        repoKey += project.version.endsWith("-SNAPSHOT") ? "-snapshot" : "-release"
        repoKey += "-local"

        return repoKey
    }

    static Boolean shouldPublish(project)
    {
        return project.hasProperty("doPublishing")
    }

    static Boolean shouldPublishDistribution(project)
    {
        return project.hasProperty("doDistributionPublish")
    }

    static Boolean isIntellij()
    {
        return System.properties.'idea.active'
    }

    static Boolean isIntellijGradleRefresh(project)
    {
        return project.getStartParameter().getSystemPropertiesArgs().get("idea.version") != null
    }
}
