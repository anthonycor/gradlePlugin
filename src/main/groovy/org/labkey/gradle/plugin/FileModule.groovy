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
import org.labkey.gradle.plugin.extension.LabKeyExtension
import org.labkey.gradle.plugin.extension.ModuleExtension
import org.labkey.gradle.plugin.extension.ServerDeployExtension
import org.labkey.gradle.task.PomFile
import org.labkey.gradle.util.BuildUtils
import org.labkey.gradle.util.GroupNames
import org.labkey.gradle.util.PropertiesUtils

import java.util.regex.Matcher
/**
 * This class is used for building a LabKey file-based module, which contains only client-side code.
 * It also serves as a base class for the Java module classes.
 */
class FileModule implements Plugin<Project>
{
    // Deprecated: instead of creating the skipBuild.txt file,
    // set the skipBuild property to true in the module's build.gradle file
    //   ext.skipBuild = true
    private static final String SKIP_BUILD_FILE = "skipBuild.txt"

    @Override
    void apply(Project project)
    {
        project.apply plugin: 'java-base'

        project.build.onlyIf ({
            return shouldDoBuild(project)
        })

        project.extensions.create("lkModule", ModuleExtension, project)
        applyPlugins(project)
        addConfigurations(project)
        addTasks(project)
        addDependencies(project)
        addArtifacts(project)
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



    protected void applyPlugins(Project project)
    {
        project.apply plugin: 'maven'
        project.apply plugin: 'maven-publish'

        if (AntBuild.isApplicable(project))
        {
            if (shouldDoBuild(project))
                project.apply plugin: 'org.labkey.antBuild'
        }
        else
        {
            if (ModuleResources.isApplicable(project))
                project.apply plugin: 'org.labkey.moduleResources'
            if (SpringConfig.isApplicable(project))
                project.apply plugin: 'org.labkey.springConfig'

            if (Webapp.isApplicable(project))
                project.apply plugin: 'org.labkey.webapp'

            if (ClientLibraries.isApplicable(project))
                project.apply plugin: 'org.labkey.clientLibraries'

            if (NpmRun.isApplicable(project))
            {
                // This brings in nodeSetup and npmInstall tasks.  See https://github.com/srs/gradle-node-plugin
                project.apply plugin: 'com.moowork.node'
                project.apply plugin: 'org.labkey.npmRun'
            }
        }
    }

    protected void addConfigurations(Project project)
    {
        project.configurations
                {
                    published
                }
    }

    protected void addTasks(Project project)
    {
        Task moduleXmlTask = project.task('moduleXml',
                group: GroupNames.MODULE,
                type: Copy,
                description: "create the module.xml file using module.properties",
                { CopySpec copy ->
                    copy.from project.project(":server").projectDir
                    copy.include 'module.template.xml'
                    copy.rename { "module.xml" }
                    copy.filter({ String line ->
                        Matcher matcher = PropertiesUtils.PROPERTY_PATTERN.matcher(line)
                        String newLine = line
                        while (matcher.find())
                        {
                            newLine = newLine.replace(matcher.group(), (String) project.lkModule.getPropertyValue(matcher.group(1), ""))
                        }
                        return newLine

                    })
                    destinationDir = new File((String) project.labkey.explodedModuleConfigDir)
                }
        )
        moduleXmlTask.outputs.upToDateWhen(
                {
                    Task task ->
                        File moduleXmlFile = new File((String) project.labkey.explodedModuleConfigDir, "/module.xml")
                        if (!moduleXmlFile.exists())
                            return false
                        else
                        {
                            if (project.file(ModuleExtension.MODULE_PROPERTIES_FILE).lastModified() > moduleXmlFile.lastModified() ||
                                    project.project(":server").file('module.template.xml').lastModified() > moduleXmlFile.lastModified())
                                return false
                        }
                        return true
                }
        )

        if (!AntBuild.isApplicable(project))
        {
            Task moduleFile = project.task("module",
                    group: GroupNames.MODULE,
                    type: Jar,
                    description: "create the module file for this project",
                    {
                        from project.labkey.explodedModuleDir
                        exclude '**/*.uptodate'
                        exclude "META-INF/${project.name}/**"
                        exclude 'gwt-unitCache/**'
                        baseName project.name
                        extension 'module'
                        destinationDir = project.buildDir
                    }
            )

            if (ModuleResources.isApplicable(project))
                moduleFile.dependsOn(project.tasks.processModuleResources)
            if (SpringConfig.isApplicable(project))
                moduleFile.dependsOn(project.tasks.processResources)
            moduleFile.dependsOn(moduleXmlTask)
            setJarManifestAttributes(project, (Manifest) moduleFile.manifest)
            if (project.getPlugins().findPlugin(ClientLibraries.class) != null && !LabKeyExtension.isDevMode(project))
                moduleFile.dependsOn(project.tasks.compressClientLibs)
            project.tasks.build.dependsOn(moduleFile)
            project.tasks.clean.dependsOn(project.tasks.cleanModule)

            project.artifacts
                    {
                        published moduleFile
                    }

        project.task('deployModule',
                group: GroupNames.MODULE,
                description: "copy a project's .module file to the local deploy directory")
                { Task task ->
                    task.inputs.file moduleFile
                    task.outputs.file "${ServerDeployExtension.getModulesDeployDirectory(project)}/${moduleFile.outputs.getFiles()[0].getName()}"

                    task.doLast {
                        project.copy { CopySpec copy ->
                            copy.from moduleFile
                            copy.into project.staging.modulesDir
                        }
                        project.copy { CopySpec copy ->
                            copy.from moduleFile
                            copy.into ServerDeployExtension.getModulesDeployDirectory(project)
                        }
                        if (LabKeyExtension.isBootstrapModule(project))
                        {
                            project.copy
                                    { CopySpec copy ->
                                        copy.from project.tasks.jar
                                        copy.into "${project.rootProject.buildDir}/deploy/labkeyWebapp/WEB-INF/lib"
                                    }
                        }
                    }
                }

            project.task('undeployModule',
                    group: GroupNames.MODULE,
                    description: "remove a project's .module file and the unjarred file from the deploy directory",
                    type: Delete,
                    { Delete delete ->
                        getModuleFilesAndDirectories(project).forEach({
                            File file ->
                                if (file.isDirectory())
                                    delete.inputs.dir file
                                else
                                    delete.inputs.file file
                        })
                        delete.outputs.dir "${ServerDeployExtension.getServerDeployDirectory(project)}/modules"
                        delete.doFirst {
                            undeployModule(project)
                        }
                    })

            project.task("reallyClean",
                    group: GroupNames.BUILD,
                    description: "Deletes the build, staging, and deployment directories of this module",
            ).dependsOn(project.tasks.clean, project.tasks.undeployModule)
        }

        if (hasClientLibraries(project))
        {
            project.task("zipWebDir",
                    group: GroupNames.MODULE,
                    description: "Create a zip file from the exploded module web directory",
                    type: Zip,
                    {
                        baseName = project.name
                        classifier = LabKey.CLIENT_LIBS_CLASSIFER
                        from project.labkey.explodedModuleWebDir
                        destinationDir project.file("${project.buildDir}/${project.libsDirName}")
                    }
            )
        }
    }

    static void setJarManifestAttributes(Project project, Manifest manifest)
    {
        manifest.attributes(
                "Implementation-Version": project.version,
                "Implementation-Title": project.lkModule.getPropertyValue("Label", project.name),
                "Implementation-Vendor": "LabKey"
        )

    }

    static boolean hasClientLibraries(Project project)
    {
        return ClientLibraries.isApplicable(project) || Gwt.isApplicable(project) || Webapp.isApplicable(project)
    }

    static undeployModule(Project project)
    {
        getModuleFilesAndDirectories(project).forEach({File file ->
            project.delete file
        })
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

    protected void addArtifacts(Project project)
    {
        if (!AntBuild.isApplicable(project))
        {
            project.afterEvaluate {
                Task pomFileTask = project.task("pomFile",
                        group: GroupNames.PUBLISHING,
                        description: "create the pom file for this project",
                        type: PomFile,
                        {PomFile pomFile ->
                            pomFile.pomProperties = project.lkModule.modProperties
                        }
                )
                project.publishing {
                    publications {
                        libs(MavenPublication) { pub ->
                            project.tasks.each {
                                if (it instanceof Jar &&
                                        (!it.name.equals("schemasJar") || XmlBeans.isApplicable(project)))
                                {
                                    pub.artifact(it)
                                }
                            }
                        }
                    }

                    if (BuildUtils.shouldPublish(project))
                    {
                        project.artifactoryPublish {
                            project.tasks.each {
                                if (it instanceof Jar &&
                                        (!it.name.equals("schemasJar") || XmlBeans.isApplicable(project)))
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

    private void addDependencies(Project project)
    {
        BuildUtils.addLabKeyDependency(project: project.project(":server"), config: 'modules', depProjectPath: project.path, depProjectConfig: 'published', depExtension: 'module')
    }
}
