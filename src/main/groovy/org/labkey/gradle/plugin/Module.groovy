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

        _project.apply plugin: 'xmlBeans'
        _project.apply plugin: 'java-base'
        _project.apply plugin: 'labKeyDbSchema'
        _project.apply plugin: 'labKeyApi'
        _project.apply plugin: 'labKeyJsp'
        _project.apply plugin: 'labKeySpringConfig'

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
        setModuleProperties()
        addTasks()
        addDependencies()
    }

    private void setVcsProperties()
    {
        _moduleProperties.setProperty("VcsURL", "???") // TODO
        _moduleProperties.setProperty("VcsRevision", "???")
        _moduleProperties.setProperty("BuildNumber", "???") // TODO where does this come from in TeamCity?
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
                    compile _project.fileTree(dir: "${_project.labkey.explodedModuleDir}/lib", include: '*.jar')
                }
        _project.tasks.compileJava.dependsOn('schemasJar')
        _project.tasks.compileJava.dependsOn('apiJar')
        _project.tasks.jsp2Java.dependsOn('apiJar')
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

        def Task moduleFile = _project.task("moduleFile",
                group: "module",
                type: Jar,
                description: "create ",
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
        moduleFile.dependsOn(modulesXmlTask, _project.tasks.assemble) // TODO is this the right dependency?
    }
}

