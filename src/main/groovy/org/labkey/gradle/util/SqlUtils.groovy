package org.labkey.gradle.util

import groovy.sql.Sql
import org.gradle.api.GradleException
import org.gradle.api.Project

import java.sql.Driver
import java.sql.DriverManager

class SqlUtils
{
    //in ant no tasks ever depend on this, so it makes sense to be a method.
    //additionally, it is always called with inheritAll=false, so we should explicitly pass in parameters.
    static void execSql(Project project, Properties params, String sql)
    {
        // read in a clean (unsubstituted) set of properties from the file
        Properties configProperties = DatabaseProperties.readDatabaseProperties(project)
        configProperties.putAll(params) //overrides configProperties in case of duplicates

        String url = PropertiesUtils.parseCompositeProp(project, configProperties, configProperties.getProperty("jdbcURL"))
        String user = configProperties.getProperty("jdbcUser")
        String password = configProperties.getProperty("jdbcPassword")
        String driverClassName = configProperties.getProperty("jdbcDriverClassName")

        //see http://gradle.1045684.n5.nabble.com/using-jdbc-driver-in-a-task-fails-td1435189.html
        URLClassLoader loader = GroovyObject.class.classLoader

        project.configurations.driver.each {File file ->
            loader.addURL(file.toURI().toURL())
        }
        Class driverClass = loader.loadClass(driverClassName)

        Driver driverInstance = (Driver) driverClass.newInstance()
        DriverManager.registerDriver(driverInstance)

        Sql db = null
        try
        {
            db = Sql.newInstance(url, user, password)
            db.execute(sql)
        }
        catch (Exception e)
        {
            project.logger.error(e.toString())
        }
        finally
        {
            if (db != null)
                db.close()
        }

    }

    static void dropDatabase(Project project, Properties properties)
    {
        if (!properties.containsKey("jdbcDatabase") || properties.get("jdbcDatabase").equals("labkey"))
        {
            throw new GradleException("Must specify a database that is not 'labkey'")
        }
        else
        {
            Properties params = new Properties()
            // need to connect to the master database in order to drop the database
            params.setProperty("jdbcDatabase", (String) properties.get('databaseMaster'))
            params.setProperty("jdbcURLParameters", "")
            params.setProperty("jdbcHost", (String) properties.get('jdbcHost'))
            params.setProperty("jdbcPort", (String) properties.get("jdbcPort"))

            project.logger.info("Attempting to drop database ${properties.get("jdbcDatabase")}")
            execSql(project, params, "DROP DATABASE \"${properties.get('jdbcDatabase')}\";")
        }
    }
}