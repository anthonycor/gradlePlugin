package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project


/**
 * This class is used for building a LabKey module (one that typically resides in a *modules
 * directory).  It defines tasks for building the jar files (<module>_api.jar, <module>_jsp.jar, <module>.jar, <module>_schemas.jar)
 * as well as tasks for copying resources to the build directory.
 *
 * Created by susanh on 4/5/16.
 */
class Module extends LabKey
{
    // Deprecated: instead of creating this file,
    // set the skipBuild property to true in the module's build.gradle file
    //   ext.skipBuild = true
    def String _skipBuildFile = "skipBuild.txt"
    def String _modulePropertiesFile = "module.properties"
    def String _explodedModuleDir = "explodedModule"
    def Properties _moduleProperties;
    def Project _project;

    @Override
    void apply(Project project)
    {
        _project = project;

        _project.apply plugin: 'xmlBeans'
        _project.apply plugin: 'java-base'
        _project.apply plugin: 'labKeyDbSchema'
        _project.apply plugin: 'labKeyApi'
        _project.apply plugin: 'labKeyJsp'

        _project.build.onlyIf({
            def List<String> indicators = new ArrayList<>();
            if (project.file(_skipBuildFile).exists())
                indicators.add(_skipBuildFile + " exists")
            if (!project.file(_modulePropertiesFile).exists())
                indicators.add(_modulePropertiesFile + " does not exist")
            if (project.labkey.skipBuild)
                indicators.add("skipBuild property set for Gradle project")

            if (indicators.size() > 0)
            {
                project.logger.info("$project.name build skipped because: " + indicators.join("; "))
            }
            return indicators.isEmpty()
        })
        readModuleProperties()
        setVersion()
        addDependencies()
    }


    private void readModuleProperties()
    {
        File propertiesFile = _project.file(_modulePropertiesFile);
        if (propertiesFile.exists())
        {
            _moduleProperties = new Properties();
            FileInputStream is = new FileInputStream(_project.file(_modulePropertiesFile))
            _moduleProperties.load(is)
        }
        setVersion();
    }

    private void setVersion()
    {
        if (_moduleProperties != null && _moduleProperties.containsKey("Version"))
            _project.version = _moduleProperties.get("Version");
        _project.version = _project.rootProject.version;
    }

    private void showRepositories(String message)
    {
        println "=== ${_project.name} ==="
        if (message != null)
            println message
        _project.repositories.each( {
            repository ->
                for (File file : repository.getDirs())
                {
                    println(file.getAbsolutePath());
                }
        })
    }

    private void addDependencies()
    {
        _project.repositories
                {
                    flatDir dirs: "${_project.rootDir}/external/lib/server"
                    flatDir dirs: "${_project.rootDir}/external/lib/tomcat"
                    flatDir dirs: "${_project.rootDir}/external/lib/build"
                    // Where do we need the old version of servlet-api that comes from external/lib/build?
                    // Put the tomcat lib directory first to find out.
                    flatDir dirs: "${_project.tomcatDir}/lib"
                    flatDir dirs: _project.file("lib")
                    flatDir dirs: "${_project.buildDir}/$_explodedModuleDir/lib"
                    flatDir dirs: _project.modulesApiDir
                    flatDir dirs: _project.webappLibDir
                    flatDir dirs: _project.webappJspDir
                    flatDir dirs: "${_project.rootProject.buildDir}/client-api/java/jar/"
                    flatDir dirs: _project.project(":server:internal").buildDir
                }
        _project.dependencies
                {
                    compile _project.project(":server:api")
                    compile _project.project(":server:internal")
                    compile _project.project(":remoteapi:java")
                    compile _project.fileTree(dir: "${_project.explodedModuleDir}/lib", include: '*.jar')
                    compile _project.fileTree(dir: "${_project.modulesApiDir}", include: '*.jar')
                }
        _project.tasks.compileJava.dependsOn('schemasJar')
        _project.tasks.compileJava.dependsOn('apiJar')
    }

}

