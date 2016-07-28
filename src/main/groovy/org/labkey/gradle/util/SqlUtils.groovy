package org.labkey.gradle.util

import org.gradle.api.Project

import org.labkey.gradle.util.ParsingUtils

import groovy.sql.Sql

import java.sql.Driver
import java.sql.DriverManager

class SqlUtils
{
    //in ant no tasks ever depend on this, so it makes sense to be a method.
    //additionally, it is always called with inheritAll=false, so we should explicitly pass in parameters.
    public static void execSql(Project project, Properties params, String sql)
    {
        Properties configProperties = ParsingUtils.readConfigProperties(project);
        configProperties.putAll(params); //overrides configProperties in case of duplicates

        String url = ParsingUtils.parseCompositeProp(configProperties, configProperties.getProperty("jdbcURL"));
        String user = configProperties.getProperty("jdbcUser");
        String password = configProperties.getProperty("jdbcPassword");
        String driverClassName = configProperties.getProperty("jdbcDriverClassName");

        //see http://gradle.1045684.n5.nabble.com/using-jdbc-driver-in-a-task-fails-td1435189.html
        URLClassLoader loader = GroovyObject.class.classLoader

        project.configurations.driver.each {File file ->
            loader.addURL(file.toURL())
        }
        Class driverClass = loader.loadClass(driverClassName)

        Driver driverInstance = driverClass.newInstance()
        DriverManager.registerDriver(driverInstance)

        try
        {
            Sql db = Sql.newInstance(url, user, password);
            db.execute(sql);
        }
        catch (Exception e)
        {
            println(e.toString());
        }

    }

    public static void dropDatabase(Project project, boolean drop)
    {
        if (drop)
        {
            if (!project.ext.has("jdbcDatabase") || project.ext.jdbcDatabase.equals("labkey") || project.ext.jdbcDatabase.equals("cpas"))
            {
                println("Must specify a database that is not 'labkey' or 'cpas'");
            }
            else
            {
                println("Attempting to drop database ${project.ext.jdbcDatabase}");
                Properties params = new Properties();
                params.setProperty("tomcatHome", "${project.tomcatDir}");
                params.setProperty("jdbcDatabase", "${project.ext.databaseMaster}");
                params.setProperty("jdbcURLParameters", "");
                params.setProperty("jdbcHost", "${project.ext.jdbcHost}");
                params.setProperty("jdbcPort", "${project.ext.jdbcPort}");

                execSql(project, params, "DROP DATABASE \"${project.ext.jdbcDatabase}\";");
            }
        }
    }
}