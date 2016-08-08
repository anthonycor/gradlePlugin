package org.labkey.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/**
 * Created by susanh on 8/8/16.
 */
class CatenateJS extends DefaultTask
{
    @InputDirectory
    def File scriptFragmentsDir = project.file("script-fragments")
    @OutputDirectory
    def File scriptsDir = project.file("resources/scripts")

    /*
    <property name="core-scripts.src" value="${basedir}/modules/core/script-fragments"/>
        <!--<property name="core-scripts.out" value="${build.modules.dir}/core/explodedModule/scripts">-->
        <property name="core-scripts.out" value="${basedir}/modules/core/resources/scripts"/>

        <mkdir dir="${core-scripts.out}/Ext"/>
        <mkdir dir="${core-scripts.out}/labkey/adapter"/>

        <!-- create a combined Ext.js usable by the core module's server-side scripts -->
        <concat destfile="${core-scripts.out}/Ext.js" force="true">
            <header file="${core-scripts.src}/Ext.header.js"/>
            <fileset file="${basedir}/api/webapp/${extjs.dirname}/src/Ext.js"/>
            <fileset file="${core-scripts.src}/Ext.middle.js"/>
            <fileset file="${basedir}/api/webapp/${extjs.dirname}/src/Observable.js"/>
            <fileset file="${basedir}/api/webapp/${extjs.dirname}/src/JSON.js"/>
            <fileset file="${basedir}/api/webapp/${extjs.dirname}/src/Connection.js"/>
            <fileset file="${basedir}/api/webapp/${extjs.dirname}/src/Format.js"/>
            <footer file="${core-scripts.src}/Ext.footer.js"/>
        </concat>

         <!--create a combined Ext.js usable by the core module's server-side scripts -->
        <concat destfile="${core-scripts.out}/Ext4.js" force="true">
            <header file="${core-scripts.src}/Ext4.header.js"/>
            <fileset file="${basedir}/api/webapp/${extjs42.dirname}/src/Ext.js"/>
            <fileset file="${basedir}/api/webapp/${extjs42.dirname}/src/lang/Array.js"/>
            <fileset file="${basedir}/api/webapp/${extjs42.dirname}/src/lang/Date.js"/>
            <fileset file="${basedir}/api/webapp/${extjs42.dirname}/src/lang/Number.js"/>
            <fileset file="${basedir}/api/webapp/${extjs42.dirname}/src/lang/Object.js"/>
            <fileset file="${basedir}/api/webapp/${extjs42.dirname}/src/lang/String.js"/>
            <fileset file="${basedir}/api/webapp/${extjs42.dirname}/src/lang/Error.js"/>
            <fileset file="${core-scripts.src}/Ext4.middle.js"/>
            <fileset file="${basedir}/api/webapp/${extjs42.dirname}/src/misc/JSON.js"/>
            <footer file="${core-scripts.src}/Ext4.footer.js"/>
        </concat>

        <!-- copy some of the clientapi into the core module's server-side scripts -->
        <concat destfile="${core-scripts.out}/labkey/ActionURL.js" force="true">
            <header file="${core-scripts.src}/labkey/ActionURL.header.js"/>
            <fileset file="${basedir}/api/webapp/clientapi/core/ActionURL.js"/>
            <footer file="${core-scripts.src}/labkey/ActionURL.footer.js"/>
        </concat>

        <concat destfile="${core-scripts.out}/labkey/Ajax.js" force="true">
            <header file="${core-scripts.src}/labkey/Ajax.header.js"/>
            <fileset file="${basedir}/api/webapp/clientapi/core/Ajax.js"/>
            <footer file="${core-scripts.src}/labkey/Ajax.footer.js"/>
        </concat>

        <concat destfile="${core-scripts.out}/labkey/Filter.js" force="true">
            <header file="${core-scripts.src}/labkey/Filter.header.js"/>
            <fileset file="${basedir}/api/webapp/clientapi/core/Filter.js"/>
            <footer file="${core-scripts.src}/labkey/Filter.footer.js"/>
        </concat>

        <concat destfile="${core-scripts.out}/labkey/Message.js" force="true">
            <header file="${core-scripts.src}/labkey/Message.header.js"/>
            <fileset file="${basedir}/api/webapp/clientapi/core/Message.js"/>
            <footer file="${core-scripts.src}/labkey/Message.footer.js"/>
        </concat>

        <concat destfile="${core-scripts.out}/labkey/Query.js" force="true">
            <header file="${core-scripts.src}/labkey/Query.header.js"/>
            <fileset file="${basedir}/api/webapp/clientapi/core/Query.js"/>
            <footer file="${core-scripts.src}/labkey/Query.footer.js"/>
        </concat>

        <concat destfile="${core-scripts.out}/labkey/Report.js" force="true">
            <header file="${core-scripts.src}/labkey/Report.header.js"/>
            <fileset file="${basedir}/api/webapp/clientapi/core/Report.js"/>
            <footer file="${core-scripts.src}/labkey/Report.footer.js"/>
        </concat>

        <concat destfile="${core-scripts.out}/labkey/Security.js" force="true">
            <header file="${core-scripts.src}/labkey/Security.header.js"/>
            <fileset file="${basedir}/api/webapp/clientapi/core/Security.js"/>
            <footer file="${core-scripts.src}/labkey/Security.footer.js"/>
        </concat>

        <concat destfile="${core-scripts.out}/labkey/FieldKey.js" force="true">
            <header file="${core-scripts.src}/labkey/FieldKey.header.js"/>
            <fileset file="${basedir}/api/webapp/clientapi/core/FieldKey.js"/>
            <footer file="${core-scripts.src}/labkey/FieldKey.footer.js"/>
        </concat>

        <!--TODO-->
        <!--<concat destfile="${core-scripts.out}/labkey/SecurityPolicy.js" force="true">-->
            <!--<header file="${core-scripts.src}/labkey/SecurityPolicy.header.js"/>-->
            <!--<fileset file="${basedir}/api/webapp/clientapi/ext3/SecurityPolicy.js"/>-->
            <!--<footer file="${core-scripts.src}/labkey/SecurityPolicy.footer.js"/>-->
        <!--</concat>-->

        <concat destfile="${core-scripts.out}/labkey/Utils.js" force="true">
            <header file="${core-scripts.src}/labkey/Utils.header.js"/>
            <fileset file="${basedir}/api/webapp/clientapi/core/Utils.js"/>
            <footer file="${core-scripts.src}/labkey/Utils.footer.js"/>
        </concat>
     */
    @TaskAction
    public void catenate()
    {

    }
}
