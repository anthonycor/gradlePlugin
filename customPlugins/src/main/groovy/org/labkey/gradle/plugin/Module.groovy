package org.labkey.gradle.plugin

import org.gradle.api.Project
import org.labkey.gradle.util.BuildUtils

/**
 * This class is used for building a LabKey module (one that typically resides in a *modules
 * directory).  It defines tasks for building the jar files (<module>_api.jar, <module>_jsp.jar, <module>.jar, <module>_schemas.jar)
 * as well as tasks for copying resources to the module's build directory.  This differs from simple module
 * in that it allows for a separate api jar and a schemas jar that the compileJava tasks depend on. (So "Simple"
 * means fewer dependencies.)
 */
class Module extends SimpleModule
{
    @Override
    void apply(Project project)
    {
        super.apply(project)

        if (!AntBuild.isApplicable(project))
        {
            addDependencies()
        }
    }

    private void addDependencies()
    {
        _project.dependencies
                {
                    BuildUtils.addLabKeyDependency(project: _project, config: "compile", depProjectPath: ":server:api")
                    BuildUtils.addLabKeyDependency(project: _project, config: "compile", depProjectPath: ":server:internal")
                    compile "org.labkey:labkey-client-api:${_project.version}"
                    compile 'org.apache.tomcat:jsp-api'
                    compile 'org.apache.tomcat:jasper'
                    if (XmlBeans.isApplicable(_project))
                        compile _project.files(_project.tasks.schemasJar)
                    if (Api.isApplicable(_project))
                        compile _project.files(_project.tasks.apiJar)
                }
    }
}

