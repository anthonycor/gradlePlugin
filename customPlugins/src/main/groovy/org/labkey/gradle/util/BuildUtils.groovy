package org.labkey.gradle.util

import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.labkey.gradle.plugin.Api
import org.labkey.gradle.plugin.XmlBeans

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Static utility methods and constants for use in the build and settings scripts.
 */
class BuildUtils
{
    public static final String BUILD_FROM_SOURCE_PROP = "buildFromSource"
    public static final String SERVER_MODULES_DIR = "server/modules"
    public static final String CUSTOM_MODULES_DIR = "server/customModules"
    public static final String OPTIONAL_MODULES_DIR = "server/optionalModules"
    public static final String EXTERNAL_MODULES_DIR = "externalModules"

    public static final List<String> BASE_MODULES = ["server:bootstrap",
                                                     "server:api",
                                                     "schemas",
                                                     "server:internal",
                                                     'remoteapi:java'
    ]


    // a set of directory paths in which to look for module directories
    public static final List<String> SERVER_MODULE_DIRS = [SERVER_MODULES_DIR,
                                                           CUSTOM_MODULES_DIR,
                                                           OPTIONAL_MODULES_DIR]

    public static final List<String> EXTERNAL_MODULE_DIRS = [EXTERNAL_MODULES_DIR,
                                                             "externalModules/scharp",
                                                             "externalModules/labModules",
                                                             "externalModules/onprcEHRModules",
                                                             "externalModules/snprcEHRModules",
                                                             "externalModules/DISCVR"]

    /**
     * This includes modules that are required for any LabKey server build (e.g., bootstrap, api, internal)
     * @param settings the settings
     */
    public static void includeBaseModules(Settings settings)
    {
        settings.include BASE_MODULES.toArray(new String[0])
    }

    public static void includeAllModules(Settings settings)
    {
        // TODO, need a way to include modules not based on directory contents
    }

    /**
     * Can be used in a gradle settings file to include the projects in a particular directory.
     * @param rootDir - the root directory for the gradle build (project.rootDir)
     * @param moduleDirs - the list of directories that are parents of module directories to be included
     * @param excludedModules - a list of directory names that are to be excluded from the build configuration (e.g., movies)
     */
    public static void includeModules(Settings settings, File rootDir, List<String> moduleDirs, List<String> excludedModules)
    {
        // find the directories in each of the moduleDirs that meet our selection criteria
        moduleDirs.each { String path ->
            File directory = new File(rootDir, path);
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
        if (moduleDirs.contains(SERVER_MODULES_DIR) && !excludedModules.contains("enginesrc"))
        {
            settings.include 'server:modules:flow:enginesrc'
            // this is included separately since there's no good way to detect it programmatically
        }
    }

    public static String convertDirToPath(File rootDir, File directory)
    {
        String relativePath = directory.absolutePath - rootDir.absolutePath
        return  relativePath.replaceAll("[\\\\\\/]", ":")
    }

    public static boolean shouldBuildFromSource(Project project)
    {
        return whyNotBuildFromSource(project).isEmpty()
    }

    public static List<String> whyNotBuildFromSource(Project project)
    {
        List<String> reasons = [];
        if (!project.hasProperty(BUILD_FROM_SOURCE_PROP))
        {
            reasons.add("Project does not have buildFromSource property")
            if (isSvnModule(project))
                reasons.add("svn module without buildFromSource property set to true")
        }
        else if (!Boolean.valueOf((String) project.property(BUILD_FROM_SOURCE_PROP)))
            reasons.add("buildFromSource property is false")

        return reasons;
    }

    public static boolean isGitModule(Project project)
    {
        return project.file(".git").exists();
    }

    public static boolean isSvnModule(Project project)
    {
        return !isGitModule(project);
    }

    public static String getVersionNumber(Project project)
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

    public static String getLabKeyModuleVersion(Project project)
    {
        String version = project.version
        // matches to a.b.c.d_rfb_123-SNAPSHOT or a.b.c.d-SNAPSHOT
        Matcher matcher = Pattern.compile("([^_-]+)[_-].*").matcher(version)
        if (matcher.matches())
            version = matcher.group(1)
        return version
    }

    public static void addLabKeyDependency(Map<String, Object> config)
    {
        addLabKeyDependency(
                (Project) config.get("project"),
                (String) config.get("config"),
                (String) config.get("depProjectPath"),
                (String) config.get("depProjectConfig"),
                (String) config.get("depVersion")
        )
    }

    public static void addLabKeyDependency(Project parentProject, String parentProjectConfig, String depProjectPath, String depProjectConfig, String depVersion)
    {
        Project depProject = parentProject.project(depProjectPath)
        if (depProject != null && shouldBuildFromSource(depProject))
        {
            parentProject.logger.info("Found project ${depProjectPath}; building ${depProjectPath} from source")
            parentProject.dependencies.add(parentProjectConfig, parentProject.dependencies.project(path: depProjectPath, configuration: depProjectConfig))
        }
        else
        {
            if (depProject == null)
            {
                parentProject.logger.info("Did not find project for dependency ${depProjectPath}.  Assumed to be external.")
                if (depVersion == null)
                    depVersion = parentProject.version
            }
            else
            {
                parentProject.logger.info("Found project ${depProjectPath} but not building from source because: "
                        + whyNotBuildFromSource(parentProject).join("; "))
                if (depVersion == null)
                    depVersion = depProject.version
            }
            parentProject.dependencies.add(parentProjectConfig, getLabKeyArtifactName(depProjectPath, depProjectConfig, depVersion))
        }
    }

    public static String getLabKeyArtifactName(String projectPath, String projectConfig, String version)
    {
        String classifier = ''
        if (projectConfig != null)
        {
            if ('apiCompile'.equals(projectConfig))
                classifier = ":${Api.CLASSIFIER}"
            else if ('xmlSchema'.equals(projectConfig))
                classifier = ":${XmlBeans.CLASSIFIER}"
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

        return "org.labkey:${moduleName}${versionString}${classifier}"

    }
}
