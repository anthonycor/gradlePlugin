package org.labkey.gradle.plugin

import org.gradle.api.Project
/**
 * This class is used for building a LabKey module (one that typically resides in a *modules
 * directory).  It defines tasks for building the jar files (<module>_api.jar, <module>_jsp.jar, <module>.jar, <module>_schemas.jar)
 * as well as tasks for copying resources to the build directory.
 *
 * Created by susanh on 4/5/16.
 */
class Module extends SimpleModule
{
    @Override
    void apply(Project project)
    {
        super.apply(project)

        addDependencies()
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
        if (_project.hasProperty('schemasJar'))
            _project.tasks.compileJava.dependsOn('schemasJar')
        if (_project.hasProperty('apiJar'))
            _project.tasks.compileJava.dependsOn('apiJar')
    }
}

