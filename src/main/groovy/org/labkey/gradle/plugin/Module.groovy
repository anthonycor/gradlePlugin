package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Created by susanh on 4/5/16.
 */
class Module implements Plugin<Project>
{
    // Deprecated: set the skipBuild property to true in the module's build.gradle file instead
    //   ext.skipBuild = true
    def String skipBuildFile = "skipBuild.txt"
    def String modulePropertiesFile = "module.properties"

    @Override
    void apply(Project project)
    {
        project.apply plugin: 'xmlBeans'
        project.apply plugin: 'java-base'
        project.apply plugin: 'labKeyDbSchema'
        project.apply plugin: 'labKeyApi'
        project.apply plugin: 'labKeyJsp'

        project.build.onlyIf({
            def List<String> indicators = new ArrayList<>();
            if (project.file(skipBuildFile).exists())
                indicators.add(skipBuildFile + " exists")
            if (isModule(project) && !project.file(modulePropertiesFile).exists())
                indicators.add(modulePropertiesFile + " does not exist")
            if (project.skipBuild)
                indicators.add("skipBuild property set for Gradle project")

            if (indicators.size() > 0)
            {
                project.logger.info("$project.name build skipped because: " + indicators.join("; "))
            }
            return indicators.isEmpty()
        })
    }

    // TODO we should be able to get rid of this since only modules should apply this plugin
    public static boolean isModule(Project project)
    {
        return project.hasProperty("isLabKeyModule") || project.path.contains("modules:");
    }
}

