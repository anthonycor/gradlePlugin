package org.labkey.gradle.task

import org.gradle.api.file.CopySpec

/**
 * Created by susanh on 8/11/16.
 */
class PickDb extends DoThenSetup
{
    String dbType
    File dir = project.project(":server").projectDir

    Closure<Void> fn = {

        //ant pick_[pg|mssql|db]
        //copies the correct config file.
        project.copy({ CopySpec copy ->
            copy.from "${dir}/configs"
            copy.into dir
            copy.include "${dbType}.properties"
            copy.rename { String fileName ->
                fileName.replace(dbType, "config")
            }
        })

        super.getFn().run();
    }
}
