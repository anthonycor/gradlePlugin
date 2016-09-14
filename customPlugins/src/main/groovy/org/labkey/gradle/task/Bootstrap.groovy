package org.labkey.gradle.task

import org.labkey.gradle.util.PropertiesUtils
import org.labkey.gradle.util.SqlUtils

class Bootstrap extends DoThenSetup
{
    def Closure<Void> fn = {
        initDatabaseProperties(project);

        Properties configProperties = PropertiesUtils.readConfigProperties(project);

        project.ext.jdbcDatabase = project.ext.databaseBootstrap;
        project.ext.jdbcHost = project.ext.databaseDefaultHost;
        project.ext.jdbcPort = project.ext.databaseDefaultPort;
        project.ext.jdbcURLParameters = "";

        project.ext.jdbcURL = PropertiesUtils.parseCompositeProp(project.ext.properties, configProperties.getProperty("jdbcURL"));

        SqlUtils.dropDatabase(this);
    }
}
