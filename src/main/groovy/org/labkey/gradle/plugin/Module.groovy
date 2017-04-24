package org.labkey.gradle.plugin

import org.gradle.api.Project
import org.labkey.gradle.util.BuildUtils

/**
 * This class is used for building a LabKey module (one that typically resides in a *modules
 * directory).  It defines tasks for building the jar files (<module>_api.jar, <module>_jsp.jar, <module>.jar, <module>_schemas.jar)
 * as well as tasks for copying resources to the module's build directory.  This differs from java module
 * in that it allows for a separate api jar and a schemas jar that the compileJava tasks depend on. (So "Simple"
 * means fewer dependencies.)
 */
class Module extends JavaModule
{
    @Override
    void apply(Project project)
    {
        super.apply(project)

        if (!AntBuild.isApplicable(project))
        {
            addDependencies(project)
        }
    }

    private void addDependencies(Project project)
    {
        project.dependencies
                {
                    // This is only required for :server:api to compile, but we exclude the declaration of dependencies from
                    // the :server:api pom file because we cannot specify a version, since we rely on the local tomcat version.
                    // Therefore, when relying on the api jar file not built from source, we require this extra definition;
                    // it will not find the tomcat jar files without this.
                    local project.fileTree(dir: "${project.ext.tomcatDir}/lib", includes: ['*.jar'], excludes: ['servlet-api.jar', 'mail.jar'])

                    BuildUtils.addLabKeyDependency(project: project, config: "compile", depProjectPath: ":server:internal")
                    BuildUtils.addLabKeyDependency(project: project, config: "compile", depProjectPath: ":remoteapi:java")
                    compile 'org.apache.tomcat:jsp-api'
                    compile 'org.apache.tomcat:jasper'
                    if (XmlBeans.isApplicable(project))
                        compile project.files(project.tasks.schemasJar)
                    if (Api.isApplicable(project))
                        compile project.files(project.tasks.apiJar)
                }
    }
}

