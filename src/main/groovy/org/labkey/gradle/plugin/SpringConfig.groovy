package org.labkey.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Jar

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Created by susanh on 4/20/16.
 */
class SpringConfig extends LabKey
{
    def Project _project;
    def String _configDir;
    def Map<String, String> _tokens = [
        'VcsRevision': "Unknown",
        'VcsURL': "URL",
    ]
    def Pattern PROPERTY_PATTERN = Pattern.compile("@@([^@]+)@@")

    @Override
    void apply(Project project)
    {
        _project = project;
        _configDir = "${_project.labkey.explodedModuleDir}/config"
        project.apply plugin: 'java-base'

        addConfiguration()
        addDependencies()
        addSourceSet()
        addTasks()
    }

    private void addConfiguration()
    {

    }

    private void addDependencies()
    {

    }

    private void addSourceSet()
    {
        _project.sourceSets
                {
                    spring {
                        resources {
                            srcDirs = ["webapp/WEB-INF/${_project.name}"]
                        }
                        output.resourcesDir = _configDir
                    }
                }
    }

    private void addTasks()
    {
        def Task modulesXmlTask = _project.task('modulesXml',
                group: "module",
                type: Copy,
                description: "create the module.xml file using module.properties",
                        {
                            println("copy from ${_project.project(":server").projectDir} to ${_configDir}")
                            from _project.project(":server").projectDir
                            include '*.template.xml'
                            rename {"module.xml"}
                            filter( { line ->
                                def Matcher matcher = PROPERTY_PATTERN.matcher(line);
                                def String newLine = line;
                                while (matcher.find())
                                {
                                    if (_tokens.containsKey(matcher.group(1)))
                                    {
                                        newLine = newLine.replace(matcher.group(), _tokens.get(matcher.group(1)))
                                    }
                                }
                                return newLine;

                            })
                            destinationDir = new File(_configDir)
                        }
        )

        def Task moduleFile = _project.task("moduleFile",
                group: "module",
                type: Jar,
                description: "create ", {
            from _project.labkey.explodedModuleDir
            exclude '**/*.uptodate'
            exclude "META-INF/${_project.name}/**"
            exclude 'gwt-unitCache/**'
            //baseName "${_project.name}"
            //extension 'module'
            archiveName "${_project.name}.module" // TODO remove this in favor of a versioned jar file when other items have change
            destinationDir = new File(_project.labkey.stagingModulesDir)
        }
        )
        moduleFile.dependsOn(modulesXmlTask, _project.tasks.assemble)
    }
}