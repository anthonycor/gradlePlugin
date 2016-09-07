package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.java.archives.Manifest
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Jar
import org.labkey.gradle.util.GroupNames

import java.text.SimpleDateFormat
import java.util.regex.Matcher
import java.util.regex.Pattern
/**
 * This class is used for building a LabKey module (one that typically resides in a *modules
 * directory).  It defines tasks for building the jar files (<module>_api.jar, <module>_jsp.jar, <module>.jar, <module>_schemas.jar)
 * as well as tasks for copying resources to the build directory.
 *
 */
class SimpleModule implements Plugin<Project>
{
    // Deprecated: instead of creating this file,
    // set the skipBuild property to true in the module's build.gradle file
    //   ext.skipBuild = true
    private static final String SKIP_BUILD_FILE = "skipBuild.txt"
    private static final String MODULE_PROPERTIES_FILE = "module.properties"
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

        _project.build.onlyIf ({
            return shouldDoBuild(project)
        })

        applyPlugins()
        addConfiguration()
        setModuleProperties()
        addTasks()
        addArtifacts()
    }

    public static boolean shouldDoBuild(Project project)
    {
        def List<String> indicators = new ArrayList<>();
        if (project.file(SKIP_BUILD_FILE).exists())
            indicators.add(SKIP_BUILD_FILE + " exists")
        if (!project.file(MODULE_PROPERTIES_FILE).exists())
            indicators.add(MODULE_PROPERTIES_FILE + " does not exist")
        if (project.labkey.skipBuild)
            indicators.add("skipBuild property set for Gradle project")

        if (indicators.size() > 0)
        {
            project.logger.info("$project.name build skipped because: " + indicators.join("; "))
        }
        return indicators.isEmpty()
    }

    protected void applyPlugins()
    {
        _project.apply plugin: 'maven'
        _project.apply plugin: 'maven-publish'

        if (AntBuild.isApplicable(_project))
        {
            if (shouldDoBuild(_project))
                _project.apply plugin: 'org.labkey.antBuild'
        }
        else
        {
            // We don't have an isApplicable method here because the directory we need to check is set in the extension
            // created by this plugin.  We could separate extension creation from plugin application, but it would be
            // different from the pattern used elsewhere.  Schema tasks will be skipped if there are no xsd files in
            // the designated directory
            _project.apply plugin: 'org.labkey.xmlBeans'

            if (ModuleResources.isApplicable(_project))
                _project.apply plugin: 'org.labkey.moduleResources'
            if (Api.isApplicable(_project))
                _project.apply plugin: 'org.labkey.api'

            if (SpringConfig.isApplicable(_project))
                _project.apply plugin: 'org.labkey.springConfig'
            if (Webapp.isApplicable(_project))
                _project.apply plugin: 'org.labkey.webapp'

            if (LibResources.isApplicable(_project))
                _project.apply plugin: 'org.labkey.libResources'

            if (ClientLibraries.isApplicable(_project))
                _project.apply plugin: 'org.labkey.clientLibraries'

            if (Jsp.isApplicable(_project))
                _project.apply plugin: 'org.labkey.jsp'

            if (Gwt.isApplicable(_project))
                _project.apply plugin: 'org.labkey.gwt'

            if (Distribution.isApplicable(_project))
                _project.apply plugin: 'org.labkey.distribution'

            if (NpmRun.isApplicable(_project))
            {
                // This brings in nodeSetup and npmInstall tasks.  See https://github.com/srs/gradle-node-plugin
                _project.apply plugin: 'com.moowork.node'
                _project.apply plugin: 'org.labkey.npmRun'
            }
        }
    }

    private void addConfiguration()
    {
        _project.configurations
                {
                    published
                }
    }

    protected void setJavaBuildProperties()
    {
        _project.sourceCompatibility = _project.labkey.sourceCompatibility
        _project.targetCompatibility = _project.labkey.targetCompatibility

        _project.libsDirName = 'explodedModule/lib'

        addSourceSets()

        _project.jar {
            baseName project.name
        }
    }

    private void addSourceSets()
    {
        _project.sourceSets {
            main {
                java {
                    srcDirs = ['src']
                }
                resources {
                    srcDirs = ['src'] // src is included because it contains some sql scripts
                    exclude '**/*.java'
                    exclude '**/*.jsp'
                }
            }
        }
    }

    private void setVcsProperties()
    {
        if (_project.plugins.hasPlugin("org.labkey.versioning"))
        {
            _moduleProperties.setProperty("VcsURL", _project.versioning.info.url)
            _moduleProperties.setProperty("VcsRevision", _project.versioning.info.commit)
            _moduleProperties.setProperty("BuildNumber",  System.hasProperty("build.number") ? System.getProperty("build.number") : _project.versioning.info.build)
        }
        else
        {
            _moduleProperties.setProperty("VcsURL", "Not built from a source control working copy")
            _moduleProperties.setProperty("VcsRevision", "Not built from a source control working copy")
            _moduleProperties.setProperty("BuildNumber", "Not built from a source control working copy")
        }
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
        if (_moduleProperties.getProperty("BuildType") == null)
            _moduleProperties.setProperty("BuildType", _project.labkey.getDeployModeName(_project))
        _moduleProperties.setProperty("BuildUser", System.getProperty("user.name"))
        _moduleProperties.setProperty("BuildOS", System.getProperty("os.name"))
        _moduleProperties.setProperty("BuildTime", SimpleDateFormat.getDateTimeInstance().format(new Date()))
        _moduleProperties.setProperty("BuildPath", _project.buildDir.getAbsolutePath() )
        _moduleProperties.setProperty("SourcePath", _project.projectDir.getAbsolutePath() )
        _moduleProperties.setProperty("ResourcePath", "") // TODO  _project.getResources().... ???
        if (_moduleProperties.getProperty("ConsolidateScripts") == null)
            _moduleProperties.setProperty("ConsolidateScripts", "")
        if (_moduleProperties.getProperty("ManageVersion") == null)
            _moduleProperties.setProperty("ManageVersion", "")
    }

    private void setModuleInfoProperties()
    {
        if (_moduleProperties.getProperty("Name") == null)
            _moduleProperties.setProperty("Name", _project.name)
        if (_moduleProperties.getProperty("ModuleClass") == null)
            _moduleProperties.setProperty("ModuleClass", "org.labkey.api.module.SimpleModule")
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

    protected void setModuleProperties()
    {
        File propertiesFile = _project.file(MODULE_PROPERTIES_FILE)
        _moduleProperties = new Properties()
        readProperties(propertiesFile, _moduleProperties)

        // remove -SNAPSHOT because the module loader does not expect or handle decorated version numbers
        _moduleProperties.setProperty("Version", _project.version.toString().replace("-SNAPSHOT", ""))

        setBuildInfoProperties()
        setModuleInfoProperties()
        setVcsProperties()
        setEnlistmentId()
        setJarManifestAttributes(_project.jar.manifest)
    }

    public void setJarManifestAttributes(Manifest manifest)
    {
        // TODO set other attributes for manifest?
        manifest.attributes(
                "Implementation-Version": _project.version,
                "Implementation-Title": _moduleProperties.getProperty("Label", _project.name),
                "Implementation-Vendor": "LabKey"
        )

    }

    protected void addTasks()
    {
        def Task moduleXmlTask = _project.task('moduleXml',
                group: GroupNames.MODULE,
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
                    destinationDir = new File((String) _project.labkey.explodedModuleConfigDir)
                }
        )
        moduleXmlTask.outputs.upToDateWhen(
                {
                    Task task ->
                        File moduleXmlFile = new File((String) _project.labkey.explodedModuleConfigDir, "/module.xml")
                        if (!moduleXmlFile.exists())
                            return false
                        else
                        {
                            if (_project.file(MODULE_PROPERTIES_FILE).lastModified() > moduleXmlFile.lastModified() ||
                                _project.project(":server").file('module.template.xml').lastModified() > moduleXmlFile.lastModified())
                                return false
                        }
                        return true
                }
        )

        def Task moduleFile = _project.task("module",
                group: GroupNames.MODULE,
                type: Jar,
                description: "create the module file for this project",
                {
                    // this will collect all the dependencies of the module into its jar, but it collects too much
//                    from { _project.configurations.runtime.collect { it.isDirectory() ? it : _project.zipTree(it) } }
                    from _project.labkey.explodedModuleDir
                    exclude '**/*.uptodate'
                    exclude "META-INF/${_project.name}/**"
                    exclude 'gwt-unitCache/**'
                    baseName _project.name
                    extension 'module'
                    destinationDir = new File((String) _project.staging.modulesDir)
//                    doFirst {
//                        project.copy {
//                            //referring to the 'module' configuration
//                            from configurations.module
//                            into 'lib'
//                        }
//                    }
                }
        )
        setJarManifestAttributes(moduleFile.manifest)
        moduleFile.dependsOn(moduleXmlTask, _project.tasks.jar)
        if (_project.hasProperty('apiJar'))
            moduleFile.dependsOn(_project.tasks.apiJar)
        if (_project.hasProperty('jspJar'))
            moduleFile.dependsOn(_project.tasks.jspJar)
        _project.tasks.build.dependsOn(moduleFile)
        _project.tasks.clean.dependsOn(_project.tasks.cleanModule)
        _project.artifacts
                {
                    published moduleFile
                }
    }

    protected void addArtifacts()
    {
        _project.publishing {
            publications {
                libs(MavenPublication) {
                    artifact _project.tasks.module
                    if (_project.hasProperty('apiJar'))
                        artifact _project.tasks.apiJar
//                    if (_moduleProperties.hasProperty("Description"))
//                        pom.withXml {
//                            asNode().appendNode('description',
//                                    _moduleProperties.getProperty("Description"))
//                        }
                }
            }
        }

        _project.artifactoryPublish {
            dependsOn _project.tasks.module
            publications('libs')
        }
    }
}

