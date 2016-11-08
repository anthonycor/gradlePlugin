package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Created by susanh on 8/5/16.
 */
class TeamCity implements Plugin<Project>
{
    @Override
    void apply(Project project)
    {

    }

    private void addTeamCityTabs()
    {
        // TODO
        /*
            <target name="teamcity_tabs">
        <mkdir dir="${dist.dir}/TeamCityTabs" />
        <copy todir="${dist.dir}/TeamCityTabs">
            <fileset file="${dist.dir}/client-api/javascript/*.zip" />
            <mapper type="merge" to="JavaScriptAPIDocs.zip" />
        </copy>
    </target>
         */
    }
}
