package org.labkey.gradle.task

import org.labkey.gradle.util.ParsingUtils
import org.labkey.gradle.util.SqlUtils

/**
 * Created by susanh on 8/11/16.
 */
class Bootstrap extends DoThenSetup
{
    def Closure<Void> fn = {
        initDatabaseProperties(project);

        Properties configProperties = ParsingUtils.readConfigProperties(project);

        project.ext.jdbcDatabase = project.ext.databaseBootstrap;
        project.ext.jdbcHost = project.ext.databaseDefaultHost;
        project.ext.jdbcPort = project.ext.databaseDefaultPort;
        project.ext.jdbcURLParameters = "";

        project.ext.jdbcURL = ParsingUtils.parseCompositeProp(project.ext.properties, configProperties.getProperty("jdbcURL"));

        SqlUtils.dropDatabase(project, true);
    }
}
