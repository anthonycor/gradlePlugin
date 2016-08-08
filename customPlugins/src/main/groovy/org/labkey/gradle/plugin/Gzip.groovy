package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileTree

class Gzip implements Plugin<Project>
{
    @Override
    void apply(Project project)
    {
        project.extensions.create("gzipSettings", GzipPluginExtension)
        addGzipTask(project);
    }

    private void addGzipTask(Project project)
    {
        def Task gzipFiles = project.task("gzipFiles"
        ).doLast({
            FileTree tree = project.fileTree(dir: project.gzipSettings.dirToZip);
            tree.include("**/*.css");
            tree.include("**/*.js");
            tree.include("**/*.html");
            tree.exclude("WEB-INF/**");
            tree.exclude("**/src/**");

            tree.eachWithIndex{ File file, int idx ->
                project.ant.gzip(
                        src: file,
                        destfile: "${file.toString()}.gz"
                )
            }
        });

        project.tasks.assemble.dependsOn(gzipFiles);
        gzipFiles.dependsOn(project.tasks.processResources);
    }
}

class GzipPluginExtension
{
    String dirToZip
}