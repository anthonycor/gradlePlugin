package org.labkey.gradle.util

import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes

/**
 * Finder for module directories that match a particular pattern.
 */
class ModuleFinder extends SimpleFileVisitor<Path>
{
    private final PathMatcher matcher
    private List<String> modulePaths = []
    private File rootDir
    private List<String> excluded = []

    private static List<String> NON_MODULE_DIRS = ["build", "buildSrc", "dist", "external", "gradle", "intellijBuild", "sampledata", "schemas", "test_secure", "tools", "vagrant", "webapps"]

    ModuleFinder(File rootDir, String pattern)
    {
        this.rootDir = rootDir
        matcher = FileSystems.getDefault().getPathMatcher("glob:"+ pattern)
    }

    ModuleFinder(File rootDir, String pattern, List<String> excluded)
    {
        this(rootDir, pattern)
        this.excluded = excluded
    }

    // Compares the glob pattern against the directory name.
    void find(Path directory) {
        if (directory != null && matcher.matches(directory)) {
            modulePaths += BuildUtils.convertDirToPath(rootDir, directory.toFile())
        }
    }


    @Override
    FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
        if (dir.getFileName().toString().startsWith(".") ||
                excluded.contains(dir.getFileName()) ||
                NON_MODULE_DIRS.contains(dir.getFileName().toString()))
        {
            FileVisitResult.SKIP_SUBTREE
        }
        else
        {
            find(dir)
            return FileVisitResult.CONTINUE
        }
    }
}
