package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.api.specs.AndSpec
import org.gradle.api.tasks.JavaExec

class Gwt implements Plugin<Project>
{
    private static final String GWT_VERSION = "2.4.0"
    private static final String GXT_VERSION = "2.2.5"
    private static final String GWT_DND_VERSION = "3.2.0"
    private static final String VALIDATION_VERSION = "1.0.0"

    private static final String GWT_EXTENSION = ".gwt.xml"

    @Override
    void apply(Project project)
    {
        project.apply plugin: 'java-base'
        project.extensions.create("gwt", GwtExtension)
        if (project.labkey.deployMode == LabKey.DeployMode.dev)
        {
            project.gwt.style = "PRETTY"
            project.gwt.draftCompile = true
            project.gwt.allBrowserCompile = false
        }

        addConfigurations(project)
        addDependencies(project)
        addSourceSet(project)
        addTasks(project)
    }

    private void addConfigurations(Project project)
    {

        project.configurations
                {
                    gwtCompile
                }
    }

    private void addDependencies(Project project)
    {

        project.dependencies {
            gwtCompile "com.google.gwt:gwt-user:${GWT_VERSION}",
                    "com.google.gwt:gwt-dev:${GWT_VERSION}",
                    "com.sencha.gxt:gxt:${GXT_VERSION}",
                    "com.allen-sauer.gwt.dnd:gwt-dnd:${GWT_DND_VERSION}",
                    "jcp.org:validation-api-${VALIDATION_VERSION}.GA:${VALIDATION_VERSION}"
        }

    }

    private void addSourceSet(Project project)
    {
        project.sourceSets {
            gwt {
                java {
                    srcDir project.gwt.srcDir
                }
            }
            main {
                java {
                    srcDir project.gwt.srcDir
                }
            }
        }
    }

    private void addTasks(Project project)
    {
        def Map<String, String> gwtModuleClasses = getGwtModuleClasses(project)
        gwtModuleClasses.entrySet().each {
            def gwtModuleClass ->

                def compileTask = project.task('compileGwt' + gwtModuleClass.getKey(), group: "gwt", type: JavaExec, description: "compile GWT source files for " + gwtModuleClass.getKey()  + " into JS",
                        {
                            def extrasDir = "${project.buildDir}/${project.gwt.extrasDir}"
                            def outputDir = "${project.labkey.explodedModuleDir}/web"

                            inputs.source project.sourceSets.gwt.java.srcDirs

                            outputs.dir outputDir

                            // Workaround for incremental build (GRADLE-1483)
                            outputs.upToDateSpec = new AndSpec()

                            doFirst {
                                project.file(extrasDir).mkdirs()
                                project.file(outputDir).mkdirs()
                            }

                            main = 'com.google.gwt.dev.Compiler'

                            // TODO remove repeated code here
                            if (project.gwt.allBrowserCompile)
                            {
                                classpath {
                                    [
                                            project.sourceSets.gwt.compileClasspath,       // Dep
                                            project.sourceSets.gwt.java.srcDirs,           // Java source
                                            project.project(":server:internal").file(project.gwt.srcDir),
                                    ]
                                }
                            }
                            else
                            {
                                classpath {
                                    [
                                            // TODO get value from  environment variable perhaps
                                            "${project.rootProject.rootDir}/external/lib/build/gwt-user-firefox",
                                            project.sourceSets.gwt.compileClasspath,       // Dep
                                            project.sourceSets.gwt.java.srcDirs,           // Java source
                                            project.project(":server:internal").file(project.gwt.srcDir),
                                    ]
                                }
                            }

                            args =
                                    [
                                            '-war', outputDir,
                                            '-style', project.gwt.style,
                                            '-logLevel', project.gwt.logLevel,
                                            '-extra', extrasDir,
                                            '-deploy', extrasDir,
                                            gwtModuleClass.getValue()
                                    ]
                            if (project.gwt.draftCompile)
                                args.add('-draftCompile')
                            jvmArgs =
                                    [
                                            '-Xss1024k',
                                            '-Djava.awt.headless=true'
                                    ]

                            maxHeapSize = '512m'
                        }
                )
                project.tasks.classes.dependsOn(compileTask)
        }
    }

    private static Map<String, String> getGwtModuleClasses(Project project)
    {
        File gwtSrc = project.file(project.gwt.srcDir)
        FileTree tree = project.fileTree(dir: gwtSrc, includes: ["**/*${GWT_EXTENSION}"]);
        Map<String, String> nameToClass = new HashMap<>();
        for (File file : tree.getFiles())
        {
            String className = file.getPath()
            className = className.substring(gwtSrc.getPath().length() + 1); // lop off the part of the path before the package structure
            className = className.replaceAll(System.getProperty("file.separator"), "."); // convert from path to class package
            className = className.substring(0, className.indexOf(GWT_EXTENSION)); // remove suffix
            nameToClass.put(file.getName().substring(0, file.getName().indexOf(GWT_EXTENSION)),className);
        }
        return nameToClass;
    }

}

class GwtExtension
{
    def String srcDir = "gwtSrc"
    def String style = "OBF"
    def String logLevel = "INFO"
    def String extrasDir = "gwtExtras"
    def Boolean draftCompile = false
    def Boolean allBrowserCompile = true
}
