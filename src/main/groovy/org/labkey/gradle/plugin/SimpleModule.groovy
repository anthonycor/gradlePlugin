package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.CopySpec
import org.gradle.api.java.archives.Manifest
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Delete
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

    Project _project

    @Override
    void apply(Project project)
    {
        _project = project

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

    static boolean shouldDoBuild(Project project)
    {
        List<String> indicators = new ArrayList<>()
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

            if (UiTest.isApplicable(_project))
            {
                _project.apply plugin: 'org.labkey.uiTest'
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

    static boolean isDatabaseSupported(Project project, String database)
    {
        ModuleExtension extension = project.extensions.getByType(ModuleExtension.class)
        String supported = extension.getPropertyValue("SupportedDatabases")
        return supported == null || supported.contains(database)
    }

    void setJarManifestAttributes(Manifest manifest)
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

        Task moduleXmlTask = _project.task('moduleXml',
                group: GroupNames.MODULE,
                type: Copy,
                description: "create the module.xml file using module.properties",
                {
                    from _project.project(":server").projectDir
                    include 'module.template.xml'
                    rename {"module.xml"}
                    filter( { String line ->
                        Matcher matcher = PropertiesUtils.PROPERTY_PATTERN.matcher(line)
                        String newLine = line
                        while (matcher.find())
                        {
                            newLine = newLine.replace(matcher.group(), (String) _project.lkModule.getPropertyValue(matcher.group(1), ""))
                        }
                        return newLine

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

        Task moduleFile = _project.task("module",
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

        Task copyExternalDependencies = _project.task("copyExternalLibs",
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
        if (_project.getPlugins().findPlugin(ClientLibraries.class) && !LabKeyExtension.isDevMode(_project))
            moduleFile.dependsOn(_project.tasks.compressClientLibs)
        _project.tasks.build.dependsOn(moduleFile)
        _project.tasks.clean.dependsOn(_project.tasks.cleanModule)


        if (hasClientLibraries(_project))
        {
            _project.task("zipWebDir",
                    group: GroupNames.MODULE,
                    description: "Create a zip file from the exploded module web directory",
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
                description: "copy a project's .module file to the local deploy directory")
                {Task task ->
                    task.inputs.file moduleFile
                    task.outputs.file "${ServerDeployExtension.getModulesDeployDirectory(_project)}/${moduleFile.outputs.getFiles()[0].getName()}"

                    task.doLast {
                        _project.copy { CopySpec copy->
                            copy.from moduleFile
                            copy.into _project.staging.modulesDir
                        }
                        _project.copy { CopySpec copy->
                            copy.from moduleFile
                            copy.into ServerDeployExtension.getModulesDeployDirectory(_project)
                        }
                        if (LabKeyExtension.isBootstrapModule(_project))
                        {
                            _project.copy
                                    { CopySpec copy ->
                                        copy.from _project.tasks.jar
                                        copy.into "${_project.rootProject.buildDir}/deploy/labkeyWebapp/WEB-INF/lib"
                                    }
                        }
                    }
                }

        _project.task('undeployModule',
                group: GroupNames.MODULE,
                description: "remove a project's .module file and the unjarred file from the deploy directory",
                type: Delete,
                { Delete delete ->
                    getModuleFilesAndDirectories(_project).forEach({
                        File file ->
                            if (file.isDirectory())
                                delete.inputs.dir file
                            else
                                delete.inputs.file file
                    })
                    delete.outputs.dir "${ServerDeployExtension.getServerDeployDirectory(_project)}/modules"
                    delete.doFirst {
                        undeployModule(_project)
                    }
                })

        _project.task("reallyClean",
                group: GroupNames.BUILD,
                description: "Deletes the build, staging, and deployment directories of this module",
        ).dependsOn(_project.tasks.clean, _project.tasks.undeployModule)

    }

    /**
     * Finds all module files and directories for a project included in the deployment directory and/or staging directory
     * @param project the project to find module files for
     * @param includeDeployed include .module files and directories in the build/deploy directory
     * @param includeStaging indlude .module files in the build/staging directory
     * @return list of files and directories for this module with the deploy .module files first, followed by the deploy directories
     *          followed by the staging .module files.
     */
    static List<File> getModuleFilesAndDirectories(Project project, Boolean includeDeployed = true, Boolean includeStaging=true)
    {
        String moduleFilePrefix = "${project.tasks.module.baseName}-"
        List<File> files = new ArrayList<>()
        if (includeDeployed)
        {
            File deployDir = new File(ServerDeployExtension.getModulesDeployDirectory(project))
            if (deployDir.isDirectory())
            {
                // first add the files because we want to delete these first.  If the directory goes away and the .module file is there
                // the directory might get recreated because of listeners.
                files.addAll(deployDir.listFiles(new FileFilter() {
                    @Override
                    boolean accept(final File file)
                    {
                        return file.isFile() && file.getName().startsWith(moduleFilePrefix)
                    }
                })
                )

                // then add the directories
                files.addAll(deployDir.listFiles(new FileFilter() {
                    @Override
                    boolean accept(final File file)
                    {
                        return file.isDirectory() && file.getName().startsWith(moduleFilePrefix)
                    }
                })
                )
            }
        }
        // staging has only the .modules files
        if (includeStaging)
        {
            File stagingDir = new File((String) project.staging.modulesDir)
            if (stagingDir.isDirectory())
            {
                files.addAll(stagingDir.listFiles(new FilenameFilter() {
                    @Override
                    boolean accept(final File dir,
                                   final String name)
                    {
                        return name.startsWith(moduleFilePrefix)
                    }
                })
                )
            }
        }
        return files
    }

    static undeployModule(Project project)
    {
        getModuleFilesAndDirectories(project).forEach({File file ->
            project.delete file
        })
    }

    private static boolean hasClientLibraries(Project project)
    {
        return ClientLibraries.isApplicable(project) || Gwt.isApplicable(project) || Webapp.isApplicable(project)
    }

    private void addDependencies()
    {
        BuildUtils.addLabKeyDependency(project: _project.project(":server"), config: 'modules', depProjectPath: _project.path, depProjectConfig: 'published', depExtension: 'module')
    }

    protected void addArtifacts()
    {
        if (!AntBuild.isApplicable(_project))
        {
            _project.afterEvaluate {
                Task pomFileTask = _project.task("pomFile",
                        group: GroupNames.PUBLISHING,
                        description: "create the pom file for this project",
                        type: PomFile,
                        {PomFile pomFile ->
                            pomFile.pomProperties = _project.lkModule.modProperties
                        }
                )
                _project.publishing {
                    publications {
                        libs(MavenPublication) {
                            _project.tasks.each {
                                if (it instanceof Jar &&
                                    (!it.name.equals("schemasJar") || XmlBeans.isApplicable(_project)))
                                        artifact it
                            }
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
                        publications('libs')
                    }

                }
            }
        }
    }
}

class ModuleExtension
{
    private static final String ENLISTMENT_PROPERTIES = "enlistment.properties"
    protected static final String MODULE_PROPERTIES_FILE = "module.properties"
    private Properties modProperties
    private Project project

    ModuleExtension(Project project)
    {
        this.project = project
        setModuleProperties(project)
    }

    Project getProject()
    {
        return project
    }

    Properties getModProperties()
    {
        return modProperties
    }

    String getPropertyValue(String propertyName, String defaultValue)
    {
        String value = modProperties.getProperty(propertyName)
        return value == null ? defaultValue : value

    }

    String getPropertyValue(String propertyName)
    {
        return getPropertyValue(propertyName, null)
    }

    Object get(String propertyName)
    {
        return modProperties.get(propertyName)
    }

    void setModuleProperties(Project project)
    {
        File propertiesFile = project.file(MODULE_PROPERTIES_FILE)
        this.modProperties = new Properties()
        PropertiesUtils.readProperties(propertiesFile, this.modProperties)

        if (modProperties.getProperty("Version") == null)
            // remove -SNAPSHOT and any feature branch prefix from the module version number
            // because the module loader does not expect or handle decorated version numbers
            modProperties.setProperty("Version", BuildUtils.getLabKeyModuleVersion(project))

        setBuildInfoProperties()
        setModuleInfoProperties()
        setVcsProperties()
        setEnlistmentId()

    }

    private void setVcsProperties()
    {
        modProperties.putAll(BuildUtils.getStandardVCSProperties(project))
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
        modProperties.setProperty("EnlistmentId", enlistmentProperties.getProperty("enlistment.id"))
    }

    private void setBuildInfoProperties()
    {
        modProperties.setProperty("RequiredServerVersion", "0.0")
        if (modProperties.getProperty("BuildType") == null)
            modProperties.setProperty("BuildType", project.labkey.getDeployModeName(project))
        modProperties.setProperty("BuildUser", System.getProperty("user.name"))
        modProperties.setProperty("BuildOS", System.getProperty("os.name"))
        modProperties.setProperty("BuildTime", SimpleDateFormat.getDateTimeInstance().format(new Date()))
        modProperties.setProperty("BuildPath", project.buildDir.getAbsolutePath() )
        modProperties.setProperty("SourcePath", project.projectDir.getAbsolutePath() )
        modProperties.setProperty("ResourcePath", "") // TODO  _project.getResources().... ???
        if (modProperties.getProperty("ConsolidateScripts") == null)
            modProperties.setProperty("ConsolidateScripts", "")
        if (modProperties.getProperty("ManageVersion") == null)
            modProperties.setProperty("ManageVersion", "")
    }

    private void setModuleInfoProperties()
    {
        if (modProperties.getProperty("Name") == null)
            modProperties.setProperty("Name", project.name)
        if (modProperties.getProperty("ModuleClass") == null)
            modProperties.setProperty("ModuleClass", "org.labkey.api.module.SimpleModule")
    }
}