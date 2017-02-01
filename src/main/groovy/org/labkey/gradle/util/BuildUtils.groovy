package org.labkey.gradle.util

import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.labkey.gradle.plugin.Api
import org.labkey.gradle.plugin.Jsp
import org.labkey.gradle.plugin.TeamCityExtension
import org.labkey.gradle.plugin.XmlBeans

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
    public static final List<String> BASE_MODULES = [
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

    public static final List<String> EHR_MODULE_NAMES = [
            "EHR_ComplianceDB",
            "WNPRC_EHR",
            "cnprc_ehr",
            "snprc_ehr",
            "ehr",
            "onprc_ehr"
    ]

    // TODO add other convenience lists here (e.g., "core" modules)

    // a set of directory paths in which to look for module directories
    public static final List<String> SERVER_MODULE_DIRS = [SERVER_MODULES_DIR,
                                                           CUSTOM_MODULES_DIR,
                                                           OPTIONAL_MODULES_DIR
    ]

    public static final List<String> EXTERNAL_MODULE_DIRS = [EXTERNAL_MODULES_DIR,
                                                             "externalModules/scharp",
                                                             "externalModules/labModules",
                                                             "externalModules/onprcEHRModules",
                                                             "externalModules/cnprcEHRModules",
                                                             "externalModules/snprcEHRModules",
                                                             "externalModules/DISCVR"]

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
        settings.include TEST_MODULE
        includeModules(settings, rootDir, [TEST_MODULES_DIR], [])
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
            File directory = new File(rootDir, path)
            if (directory.exists())
            {
                String prefix = convertDirToPath(rootDir, directory)
                settings.include directory.listFiles().findAll { File f ->
                    // exclude non-directories, explicitly excluded names, and directories beginning with a .
                    f.isDirectory() && !excludedModules.contains(f.getName()) && !(f =~ ".*/\\..*") && !(f =~ "^\\..*")
                }.collect {
                    (String) "${prefix}:${it.getName()}"
                }.toArray(new String[0])
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
        if (!project.hasProperty(property))
        {
            reasons.add("Project does not have ${property} property")
            if (isSvnModule(project))
                reasons.add("svn module without ${property} property set to true")
        }
        else if (!Boolean.valueOf((String) project.property(property)))
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
            if (["trunk", "master", "develop"].contains(branch))
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
              !"jars".equals(parentProjectConfig) && !"jspJars".equals(parentProjectConfig),  specialParams)
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
        Project depProject = parentProject.rootProject.project(depProjectPath)
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
        }

        String moduleName
        if (projectPath.endsWith("remoteapi:java"))
        {
            moduleName = "labkey-client-api"
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
}
