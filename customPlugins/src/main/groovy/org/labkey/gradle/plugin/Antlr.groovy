package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileTree
import org.gradle.api.specs.AndSpec
import org.gradle.api.tasks.JavaExec
import org.labkey.gradle.util.GroupNames

/**
 * Used to compile antlr grammars into Java classes using the antlr executable.
 */
class Antlr implements Plugin<Project>
{
    private static final String ANTLR_JAR = "antlr-3.5.2.jar"
    private static final String EXTENSION = ".g"

    @Override
    void apply(Project project)
    {
        addSourceSets(project)
        addTasks(project)
    }

    public void addSourceSets(Project project)
    {
        // add the generated source directory as part of the compile source
        project.sourceSets {
            main {
                java {
                    srcDir "${project.labkey.srcGenDir}/antlr"
                }
            }
        }
    }

    public void addTasks(Project project)
    {
        FileTree antlrInput = project.fileTree(dir: project.projectDir, includes: ["**/*${EXTENSION}"])

        if (!antlrInput.isEmpty())
        {
            def outputDir = new File("${project.labkey.srcGenDir}/antlr/org/labkey/query/sql/antlr")
            List<Task> antlrTasks = new ArrayList<>();
            antlrInput.files.each() {
                def file ->
                    def Task antlrTask = project.task("antlr" + file.getName().substring(0, file.getName().indexOf(EXTENSION)),
                            type: JavaExec,
                            group: GroupNames.CODE_GENERATION,
                            description: "Generate Java classes from " + file.getName(),
                            {
                                inputs.source file
                                outputs.dir outputDir

                                // Workaround for incremental build (GRADLE-1483)
                                outputs.upToDateSpec = new AndSpec()

                                main = 'org.antlr.Tool'

                                classpath {
                                    [
                                        project.file("lib/${ANTLR_JAR}")
                                    ]
                                }

                                args =
                                    [
                                        '-o', outputDir,
                                        file.getPath()
                                    ]
                            }
                    )
                    antlrTasks.add(antlrTask);
            }
            def antlrTask = project.task("antlr",
                    group: GroupNames.CODE_GENERATION,
                    description: "generate Java classes from all ${EXTENSION} files",
                    dependsOn: antlrTasks
            )
            project.tasks.compileJava.dependsOn(antlrTask)
        }
    }
}
