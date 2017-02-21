package org.labkey.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.OutputDirectory

abstract class DistributionTask extends DefaultTask
{
    @OutputDirectory
    File dir

    DistributionTask()
    {
        dir = project.rootProject.file("dist")
    }

}
