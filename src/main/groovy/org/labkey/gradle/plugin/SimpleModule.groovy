package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.java.archives.Manifest
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Zip
import org.labkey.gradle.task.PomFile
import org.labkey.gradle.util.BuildUtils
import org.labkey.gradle.util.GroupNames
import org.labkey.gradle.util.PropertiesUtils

import java.text.SimpleDateFormat
import java.util.regex.Matcher
/**
 * This class is used for building a LabKey module (one that typically resides in a *modules
 * directory).  It defines tasks for building the jar files (<module>_jsp.jar, <module>.jar)
 * as well as tasks for copying resources to the build directory.
 *
 */
class SimpleModule implements Plugin<Project>
{
    // Deprecated: instead of creating the skipBuild.txt file,
    // set the skipBuild property to true in the module's build.gradle file
    //   ext.skipBuild = true
    private static final String SKIP_BUILD_FILE = "skipBuild.txt"

    def Project _project;

    @Override
    void apply(Project project)
    {
        _project = project;

        _project.apply plugin: 'java-base'

        _project.build.onlyIf ({
            return shouldDoBuild(project)
        })

        project.extensions.create("lkModule", ModuleExtension, project)
        setJavaBuildProperties()
        applyPlugins()
        addConfigurations()
        setJarManifestAttributes(_project.jar.manifest)
        addTasks()
        addDependencies()
        addArtifacts()
    }

    public static boolean shouldDoBuild(Project project)
    {
        def List<String> indicators = new ArrayList<>();
        if (project.file(SKIP_BUILD_FILE).exists())
            indicators.add(SKIP_BUILD_FILE + " exists")
        if (!project.file(ModuleExtension.MODULE_PROPERTIES_FILE).exists())
            indicators.add(ModuleExtension.MODULE_PROPERTIES_FILE + " does not exist")
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

    private void addConfigurations()
    {
        _project.configurations
                {
                    published
                    local
                    compile.extendsFrom(external)
                    compile.extendsFrom(local)
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
//            test {
//                java {
//                    srcDirs = ['test/src']
//                }
//                resources {
//                    srcDirs = ["test/resources"]
//                }
//            }
        }
    }

    public void setJarManifestAttributes(Manifest manifest)
    {
        manifest.attributes(
                "Implementation-Version": _project.version,
                "Implementation-Title": _project.lkModule.getPropertyValue("Label", _project.name),
                "Implementation-Vendor": "LabKey"
        )

    }

    protected void addTasks()
    {
//        FIXME: the following tasks will not work in general because of the cycle between ms2 and ms1.  Would be nice...
//        _project.task('javadocJar', description: "Generate jar file of javadoc files", type: Jar) {
//            from project.tasks.javadoc.destinationDir
//            group GroupNames.DISTRIBUTION
//            baseName "${project.name}_${LabKey.JAVADOC_CLASSIFIER}"
//            classifier LabKey.JAVADOC_CLASSIFIER
//            dependsOn project.tasks.javadoc
//        }
//
//        _project.task('sourcesJar', description: "Generate jar file of source files", type: Jar) {
//            from project.sourceSets.main.allJava
//
//            group GroupNames.DISTRIBUTION
//            baseName "${project.name}_${LabKey.SOURCES_CLASSIFIER}"
//            classifier LabKey.SOURCES_CLASSIFIER
//        }

        def Task moduleXmlTask = _project.task('moduleXml',
                group: GroupNames.MODULE,
                type: Copy,
                description: "create the module.xml file using module.properties",
                {
                    from _project.project(":server").projectDir
                    include 'module.template.xml'
                    rename {"module.xml"}
                    filter( { String line ->
                        def Matcher matcher = PropertiesUtils.PROPERTY_PATTERN.matcher(line);
                        def String newLine = line;
                        while (matcher.find())
                        {
                            newLine = newLine.replace(matcher.group(), (String) _project.lkModule.getPropertyValue(matcher.group(1), ""))
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
                            if (_project.file(ModuleExtension.MODULE_PROPERTIES_FILE).lastModified() > moduleXmlFile.lastModified() ||
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
                    from _project.labkey.explodedModuleDir
                    exclude '**/*.uptodate'
                    exclude "META-INF/${_project.name}/**"
                    exclude 'gwt-unitCache/**'
                    baseName _project.name
                    extension 'module'
                    destinationDir = _project.buildDir
                }
        )

        def Task copyExternalDependencies = _project.task("copyExternalLibs",
                group: GroupNames.MODULE,
                type: Copy,
                description: "copy the dependencies declared in the 'external' configuration into the lib directory of the built module",
                {
                    from _project.configurations.external
                    into "${_project.labkey.explodedModuleDir}/lib"
                    include "*.jar"
                    exclude "*-sources.jar"
                    exclude "*-javadoc.jar"
                }
        )
        moduleFile.dependsOn(copyExternalDependencies)
        setJarManifestAttributes((Manifest) moduleFile.manifest)
        moduleFile.dependsOn(moduleXmlTask, _project.tasks.jar)
        if (_project.hasProperty('apiJar'))
            moduleFile.dependsOn(_project.tasks.apiJar)
        if (_project.hasProperty('jspJar'))
            moduleFile.dependsOn(_project.tasks.jspJar)
        _project.tasks.build.dependsOn(moduleFile)
        _project.tasks.clean.dependsOn(_project.tasks.cleanModule)


        if (hasClientLibraries(_project))
        {
            _project.task("zipWebDir",
                    group: GroupNames.MODULE,
                    description: "Create a zip file form the exploded module web directory",
                    type: Zip,
                    {
                        baseName = _project.name
                        classifier = LabKey.CLIENT_LIBS_CLASSIFER
                        from _project.labkey.explodedModuleWebDir
                        destinationDir _project.file("${_project.buildDir}/${_project.libsDirName}")
                    }
            )
        }

        _project.artifacts
                {
                    published moduleFile
                }

        _project.task('deployModule',
            group: GroupNames.MODULE,
            type: Copy,
            description: "copy a project's .module file to the local deploy directory",
                {
                    from moduleFile
                    into "${ServerDeployExtension.getServerDeployDirectory(project)}/modules"
                    dependsOn(moduleFile)
                })
    }

    private boolean hasClientLibraries(Project project)
    {
        return ClientLibraries.isApplicable(project) || Gwt.isApplicable(project) || Webapp.isApplicable(project);
    }

    private void addDependencies()
    {
        BuildUtils.addLabKeyDependency(project: _project.project(":server"), config: 'modules', depProjectPath: _project.path, depProjectConfig: 'published', depExtension: 'module')
        if (_project.file("test").exists())
        {
            _project.dependencies {
                testCompile "org.jetbrains:annotations:${_project.annotationsVersion}",
                            "commons-beanutils:commons-beanutils:${_project.commonsBeanutilsVersion}",
                            "org.apache.commons:commons-lang3:${_project.commonsLang3Version}",
                            "org.apache.commons:commons-collections4:${_project.commonsCollections4Version}",
                            "com.googlecode.json-simple:json-simple:${_project.jsonSimpleVersion}",
                            "junit:junit:${_project.junitVersion}",
                            "org.apache.tika:tika-app:${_project.tikaAppVersion}",
                            "log4j:log4j:${_project.log4jVersion}",
                            "org.apache.xmlbeans:xbean:${_project.xmlbeansVersion}",
                            "com.fasterxml.jackson.core:jackson-annotations:${_project.jacksonAnnotationsVersion}",
                            "com.fasterxml.jackson.core:jackson-core:${_project.jacksonVersion}",
                            "com.fasterxml.jackson.core:jackson-databind:${_project.jacksonVersion}"
            }
            BuildUtils.addLabKeyDependency(project: _project, config: 'testCompile', depProjectPath: ":schemas")
            BuildUtils.addLabKeyDependency(project: _project, config: 'testCompile', depProjectPath: ":server:api")
            BuildUtils.addLabKeyDependency(project: _project, config: 'testCompile', depProjectPath: ":remoteapi:java")
        }
    }

    protected void addArtifacts()
    {
        if (!AntBuild.isApplicable(_project))
        {
            _project.afterEvaluate {
                Task pomFileTask = _project.task("pomFile",
                        group: GroupNames.PUBLISHING,
                        description: "create the pom file for this project",
                        type: PomFile
                )
                _project.publishing {
                    publications {
                        libs(MavenPublication) {
                            _project.tasks.each {
                                if (it instanceof Jar &&
                                    (!it.name.equals("schemasJar") || XmlBeans.isApplicable(_project)))
                                        artifact it
                            }
//                            if (_project.hasProperty("zipWebDir"))
//                                artifact _project.tasks.zipWebDir

                        }
                    }


                    _project.artifactoryPublish {
                        _project.tasks.each {
                            if (it instanceof Jar &&
                                    (!it.name.equals("schemasJar") || XmlBeans.isApplicable(_project)))
                            {
                                dependsOn it
                            }
                        }
                        dependsOn pomFileTask
//                        if (_project.hasProperty("zipWebDir"))
//                            dependsOn _project.tasks.zipWebDir
                        publications('libs')
                    }

                }
            }
        }
    }
}

public class ModuleExtension
{
    private static final String ENLISTMENT_PROPERTIES = "enlistment.properties"
    protected static final String MODULE_PROPERTIES_FILE = "module.properties"
    private Properties properties
    private Project project

    public ModuleExtension(Project project)
    {
        this.project = project
        setModuleProperties(project);
    }

    public Project getProject()
    {
        return project
    }

    public String getPropertyValue(String propertyName, String defaultValue)
    {
        String value = properties.getProperty(propertyName)
        return value == null ? defaultValue : value;

    }
    public String getPropertyValue(String propertyName)
    {
        return getPropertyValue(propertyName, null)
    }

    public Object get(String propertyName)
    {
        return properties.get(propertyName)
    }

    public void setModuleProperties(Project project)
    {
        File propertiesFile = project.file(MODULE_PROPERTIES_FILE)
        this.properties = new Properties()
        PropertiesUtils.readProperties(propertiesFile, this.properties)

        // remove -SNAPSHOT and any feature branch prefix from the module version number
        // because the module loader does not expect or handle decorated version numbers
        properties.setProperty("Version", BuildUtils.getLabKeyModuleVersion(project))

        setBuildInfoProperties()
        setModuleInfoProperties()
        setVcsProperties()
        setEnlistmentId()

    }

    private void setVcsProperties()
    {
        if (project.plugins.hasPlugin("org.labkey.versioning"))
        {
            properties.setProperty("VcsURL", project.versioning.info.url)
            properties.setProperty("VcsRevision", project.versioning.info.commit)
            properties.setProperty("BuildNumber",  System.hasProperty("build.number") ? System.getProperty("build.number") : project.versioning.info.build)
        }
        else
        {
            properties.setProperty("VcsURL", "Unknown")
            properties.setProperty("VcsRevision", "Unknown")
            properties.setProperty("BuildNumber", "Unknown")
        }
    }

    private setEnlistmentId()
    {
        File enlistmentFile = new File(project.getRootProject().getProjectDir(), ENLISTMENT_PROPERTIES)
        Properties enlistmentProperties = new Properties()
        if (!enlistmentFile.exists())
        {
            UUID id = UUID.randomUUID()
            enlistmentProperties.setProperty("enlistment.id", id.toString())
            enlistmentProperties.store(new FileWriter(enlistmentFile), SimpleDateFormat.getDateTimeInstance().format(new Date()))
        }
        else
        {
            PropertiesUtils.readProperties(enlistmentFile, enlistmentProperties)
        }
        properties.setProperty("EnlistmentId", enlistmentProperties.getProperty("enlistment.id"))
    }

    private void setBuildInfoProperties()
    {
        properties.setProperty("RequiredServerVersion", "0.0")
        if (properties.getProperty("BuildType") == null)
            properties.setProperty("BuildType", project.labkey.getDeployModeName(project))
        properties.setProperty("BuildUser", System.getProperty("user.name"))
        properties.setProperty("BuildOS", System.getProperty("os.name"))
        properties.setProperty("BuildTime", SimpleDateFormat.getDateTimeInstance().format(new Date()))
        properties.setProperty("BuildPath", project.buildDir.getAbsolutePath() )
        properties.setProperty("SourcePath", project.projectDir.getAbsolutePath() )
        properties.setProperty("ResourcePath", "") // TODO  _project.getResources().... ???
        if (properties.getProperty("ConsolidateScripts") == null)
            properties.setProperty("ConsolidateScripts", "")
        if (properties.getProperty("ManageVersion") == null)
            properties.setProperty("ManageVersion", "")
    }

    private void setModuleInfoProperties()
    {
        if (properties.getProperty("Name") == null)
            properties.setProperty("Name", project.name)
        if (properties.getProperty("ModuleClass") == null)
            properties.setProperty("ModuleClass", "org.labkey.api.module.SimpleModule")
    }
}