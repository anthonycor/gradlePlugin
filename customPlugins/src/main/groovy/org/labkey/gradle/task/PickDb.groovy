package org.labkey.gradle.task

/**
 * Created by susanh on 8/11/16.
 */
class PickDb extends DoThenSetup
{
    def String dbType;

    def Closure<Void> fn = {

        //ant pick_[pg|mssql|db]
        //copies the correct config file.
        project.copy({
            from "${project.projectDir}${File.separator}configs"
            into "${project.projectDir}"
            include "${dbType}.properties"
            rename { String fileName ->
                fileName.replace("${dbType}", "config")
            }
        })

        super.getFn().run();
    }
}
