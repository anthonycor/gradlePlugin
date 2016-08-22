package org.labkey.gradle.task

import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.file.FileTree

/**
 * This action can be applied to a task as a doLast action if we need to create gzipped versions of the output files
 * produced by a task.
 */
class GzipAction implements Action<Task>
{
    @Override
    void execute(Task task)
    {
        println("${task.project.name}: GzipAction outputDir is ${task.outputs.files.files}")
        FileTree tree = task.outputs.files.getAsFileTree().matching {
            include("**/*.css")
            include("**/*.js");
            include("**/*.html");
            exclude("WEB-INF/**");
            exclude("**/src/**");
        }

        tree.each { File file ->
            task.project.ant.gzip(
                    src: file,
                    destfile: "${file.toString()}.gz"
            )
            task.project.logger.info("zipping file " + file + " to ${file.toString()}.gz")
        }
    }
}
