package org.labkey.gradle.util

import org.gradle.api.initialization.Settings

/**
 * Static utility methods and constants for use in the build and settings scripts.
 */
class BuildUtils
{
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
                String relativePath = directory.absolutePath - rootDir.absolutePath
                String prefix = relativePath.replaceAll("[\\\\\\/]", ":")
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
}
