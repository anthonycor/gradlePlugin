package org.labkey.gradle.plugin

import org.gradle.api.Project
/**
 * TODO:
 *  - TeamCityTabs tasks
 *  - StartTomcat task needs some parameters
 *  - create pipeline and nlp configurations
 *  - unpack_dist
 *
 */
class TeamCity extends Tomcat
{
    @Override
    void apply(Project project)
    {
        // TODO set the parameters for the extension here
        if (project.file("${project.tomcatDir}/localhost.truststore").exists())
        {
            project.tomcat.trustStore = "-Djavax.net.ssl.trustStore=${project.tomcatDir}/localhost.truststore"
            project.tomcat.trustStorePassword = "-Djavax.net.ssl.trustStorePassword=changeit"
        }
        project.tomcat.catalinaOpts = "-Xdebug -Dproject.root=${project.rootDir} -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=${project.tomcat.debug} "

        addTasks(project)
    }

    private void addTasks(Project project)
    {
//        Task teamCityTabsTask = project.task("copyJavascriptDocs",
//            group: GroupNames.TEST_SERVER,
//                description: "create client-api docs file for presentation in TeamCity",
//                type: Copy
//                {
//                    from
//                    into
//                }
//        )
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
        project.tasks.stopTomcat.dependsOn(project.tasks.debugClasses)
        project.tasks.stopTomcat.doLast (
                {
                    ensureShutdown(project)
                }
        )
    }

    public static boolean isTeamCity(Project project)
    {
        return project.hasProperty('teamcity')
    }

    public static String getProperty(Project project, String name)
    {
        return isTeamCity(project) ? project.teamcity[name] : "";
    }

    private void ensureShutdown(Project project)
    {
        if (!getProperty(project, "tomcat.debug").isEmpty())
        {
            project.javaexec({
                main = "org.labkey.test.debug.ThreadDumpAndKill"
                classpath { [project.sourceSets.debug.output.classesDir, project.configurations.debugCompile] }
                args = [project.teamcity['tomcat.debug']]
            })
        }
    }
}
