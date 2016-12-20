package org.labkey.gradle.util

import groovy.sql.Sql
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task

import java.sql.Driver
import java.sql.DriverManager

class SqlUtils
{
    //in ant no tasks ever depend on this, so it makes sense to be a method.
    //additionally, it is always called with inheritAll=false, so we should explicitly pass in parameters.
    static void execSql(Project project, Properties params, String sql)
    {
        Properties configProperties = PropertiesUtils.readDatabaseProperties(project);
        configProperties.putAll(params); //overrides configProperties in case of duplicates

        String url = PropertiesUtils.parseCompositeProp(configProperties, configProperties.getProperty("jdbcURL"));
        String user = configProperties.getProperty("jdbcUser");
        String password = configProperties.getProperty("jdbcPassword");
        String driverClassName = configProperties.getProperty("jdbcDriverClassName");

        //see http://gradle.1045684.n5.nabble.com/using-jdbc-driver-in-a-task-fails-td1435189.html
        URLClassLoader loader = GroovyObject.class.classLoader

        project.configurations.driver.each {File file ->
            loader.addURL(file.toURI().toURL())
        }
        Class driverClass = loader.loadClass(driverClassName)

        Driver driverInstance = (Driver) driverClass.newInstance()
        DriverManager.registerDriver(driverInstance)

        try
        {
            Sql db = Sql.newInstance(url, user, password);
            db.execute(sql);
        }
        catch (Exception e)
        {
            project.logger.error(e.toString());
        }

    }

    static void dropDatabase(Task task, Map<String, Object> properties)
    {
        Project project = task.project

        if (!properties.containsKey("jdbcDatabase") || properties.get("jdbcDatabase").equals("labkey"))
        {
            throw new GradleException("Must specify a database that is not 'labkey'")
        }
        else
        {
            project.logger.info("Attempting to drop database ${properties.get("jdbcDatabase")}");
            Properties params = new Properties();
            params.setProperty("tomcatHome", project.tomcatDir);
            params.setProperty("jdbcDatabase", (String) properties.get('databaseMaster'));
            params.setProperty("jdbcURLParameters", "");
            params.setProperty("jdbcHost", (String) properties.get('jdbcHost'));
            params.setProperty("jdbcPort", (String) properties.get("jdbcPort"));

            execSql(project, params, "DROP DATABASE \"${properties.get('jdbcDatabase')}\";");
        }
    }
}