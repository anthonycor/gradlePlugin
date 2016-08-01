package org.labkey.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Jar

import java.text.SimpleDateFormat
import java.util.regex.Matcher
import java.util.regex.Pattern
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
    private static final String ENLISTMENT_PROPERTIES = "enlistment.properties"
    def Properties _moduleProperties;
    def Project _project;
    def Pattern PROPERTY_PATTERN = Pattern.compile("@@([^@]+)@@")

    @Override
    void apply(Project project)
    {
        _project = project;

        _project.apply plugin: 'java-base'
        setJavaBuildProperties()

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

        _project.apply plugin: 'org.labkey.xmlBeans'
        _project.apply plugin: 'org.labkey.resources'
        _project.apply plugin: 'org.labkey.api'

        _project.apply plugin: 'org.labkey.springConfig'
        _project.apply plugin: 'org.labkey.webapp'
        _project.apply plugin: 'org.labkey.libResources'
        _project.apply plugin: 'org.labkey.clientLibraries'

        File gwtSrc = _project.file('gwtsrc')
        if (gwtSrc.exists())
            _project.apply plugin: 'org.labkey.gwt'

        setModuleProperties()
        addTasks()
        addDependencies()

        _project.apply plugin: 'org.labkey.jsp'
    }

    private void setJavaBuildProperties()
    {
        _project.sourceCompatibility = _project.labkey.sourceCompatibility
        _project.targetCompatibility = _project.labkey.targetCompatibility

        _project.libsDirName = 'explodedModule/lib'

        addSourceSets()

        _project.jar {
            manifest.attributes provider: 'LabKey'
            // TODO set other attributes for manifest?
            archiveName "${project.name}.jar"
        }
    }

    private void addSourceSets()
    {
        _project.sourceSets {
            main {
                java {
                    srcDirs = ['src']
                }
            }
        }
    }

    private void setVcsProperties()
    {
        _moduleProperties.setProperty("VcsURL", "http://some/url") // TODO: best plugin available assumes old SVN layout that has .svn in each directory
        _moduleProperties.setProperty("VcsRevision", "123") // TODO
        _moduleProperties.setProperty("BuildNumber", "123") // TODO where does this come from in TeamCity?
    }

    private setEnlistmentId()
    {
        File enlistmentFile = new File(_project.getRootProject().getProjectDir(), ENLISTMENT_PROPERTIES)
        Properties enlistmentProperties = new Properties()
        if (!enlistmentFile.exists())
        {
            UUID id = UUID.randomUUID()
            enlistmentProperties.setProperty("enlistment.id", id.toString())
            enlistmentProperties.store(new FileWriter(enlistmentFile), SimpleDateFormat.getDateTimeInstance().format(new Date()))
        }
        else
        {
            readProperties(enlistmentFile, enlistmentProperties)
        }
        _moduleProperties.setProperty("EnlistmentId", enlistmentProperties.getProperty("enlistment.id"))
    }

    private void setBuildInfoProperties()
    {
        _moduleProperties.setProperty("RequiredServerVersion", "0.0")
        _moduleProperties.setProperty("BuildType", _project.labkey.deployMode.toString())
        _moduleProperties.setProperty("BuildUser", System.getProperty("user.name"))
        _moduleProperties.setProperty("BuildOS", System.getProperty("os.name"))
        _moduleProperties.setProperty("BuildTime", SimpleDateFormat.getDateTimeInstance().format(new Date()))
        _moduleProperties.setProperty("BuildPath", _project.buildDir.getAbsolutePath() )
        _moduleProperties.setProperty("SourcePath", _project.projectDir.getAbsolutePath() )
        _moduleProperties.setProperty("ResourcePath", "") // TODO  _project.getResources().... ???
        _moduleProperties.setProperty("ConsolidateScripts", "")
        _moduleProperties.setProperty("ManageVersion", "")
    }

    private void addDependencies()
    {
        _project.dependencies
                {
                    compile _project.project(":server:api")
                    compile _project.project(":server:internal")
                    compile _project.project(":remoteapi:java")
                    compile _project.fileTree(dir: "${_project.labkey.explodedModuleDir}/lib", include: '*.jar') // TODO this seems like it should be a project(...) dependency
                }
        _project.tasks.compileJava.dependsOn('schemasJar')
        _project.tasks.compileJava.dependsOn('apiJar')
    }

    private static void readProperties(File propertiesFile, Properties properties)
    {
        if (propertiesFile.exists())
        {
            FileInputStream is;
            try
            {
                is = new FileInputStream(propertiesFile)
                properties.load(is)
            }
            finally
            {
                if (is != null)
                    is.close()
            }
        }
    }

    private void setModuleProperties()
    {
        File propertiesFile = _project.file(_modulePropertiesFile)
        _moduleProperties = new Properties()
        readProperties(propertiesFile, _moduleProperties)

        _moduleProperties.setProperty("Version", _project.version.toString())
        setBuildInfoProperties()
        setVcsProperties()
        setEnlistmentId()
    }

    private void addTasks()
    {
        def Task modulesXmlTask = _project.task('modulesXml',
                group: "module",
                type: Copy,
                description: "create the module.xml file using module.properties",
                {
                    from _project.project(":server").projectDir
                    include 'module.template.xml'
                    rename {"module.xml"}
                    filter( { String line ->
                        //Todo: migrate to ParsingUtils
                        def Matcher matcher = PROPERTY_PATTERN.matcher(line);
                        def String newLine = line;
                        while (matcher.find())
                        {
                            if (_moduleProperties.containsKey(matcher.group(1)))
                            {
                                newLine = newLine.replace(matcher.group(), (String) _moduleProperties.get(matcher.group(1)))
                            }
                            else
                            {
                                newLine = newLine.replace(matcher.group(), "")
                            }
                        }
                        return newLine;

                    })
                    destinationDir = new File((String) _project.sourceSets.spring.output.resourcesDir)
                }
        )
        modulesXmlTask.outputs.upToDateWhen(
                {
                    Task task ->
                        File moduleXmlFile = new File((String) _project.sourceSets.spring.output.resourcesDir, "/module.xml")
                        if (!moduleXmlFile.exists())
                            return false
                        else
                        {
                            if (_project.file(_modulePropertiesFile).lastModified() > moduleXmlFile.lastModified() ||
                                _project.project(":server").file('module.template.xml').lastModified() > moduleXmlFile.lastModified())
                                return false
                        }
                        return true
                }
        )

        def Task moduleFile = _project.task("module",
                group: "module",
                type: Jar,
                description: "create the module file for this project",
                {
                    from _project.labkey.explodedModuleDir
                    exclude '**/*.uptodate'
                    exclude "META-INF/${_project.name}/**"
                    exclude 'gwt-unitCache/**'
                    //baseName "${_project.name}"
                    //extension 'module'
                    archiveName "${_project.name}.module" // TODO remove this in favor of a versioned jar file when other items have change
                    destinationDir = new File((String) _project.labkey.stagingModulesDir)
                }
        )
        moduleFile.dependsOn(modulesXmlTask, _project.tasks.assemble)
        _project.tasks.build.dependsOn(moduleFile)
    }
}

