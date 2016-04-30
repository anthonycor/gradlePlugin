package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Created by susanh on 4/30/16.
 */
class Gwt implements Plugin<Project>
{
    @Override
    void apply(Project project)
    {
        project.apply plugin: 'java-base'
        project.sourceSets {
            main {
                java {
                    srcDir 'gwtsrc'
                }
            }
        }
    }
}
