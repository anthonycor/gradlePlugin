package org.labkey.gradle.plugin.extension

import org.gradle.api.Project
import org.labkey.gradle.util.DatabaseProperties

/**
 * Created by susanh on 4/23/17.
 */
class TeamCityExtension
{
    String databaseName
    Boolean dropDatabase = false
    List<DatabaseProperties> databaseTypes = new ArrayList<>()
    List<String> validationMessages = new ArrayList<>()
    Project project
    static final String CUSTOM_DB_PROPS = "custom"

    private static final Map<String, DatabaseProperties> SUPPORTED_DATABASES = new HashMap<>()
    static
    {
        SUPPORTED_DATABASES.put("postgres9.2", new DatabaseProperties("postgres9.2", "pg", "9.2"))
        SUPPORTED_DATABASES.put("postgres9.3", new DatabaseProperties("postgres9.3", "pg", "9.3"))
        SUPPORTED_DATABASES.put("postgres9.4", new DatabaseProperties("postgres9.4", "pg", "9.4"))
        SUPPORTED_DATABASES.put("postgres9.5", new DatabaseProperties("postgres9.5", "pg", "9.5"))
        SUPPORTED_DATABASES.put("postgres9.6", new DatabaseProperties("postgres9.6", "pg", "9.6"))
        SUPPORTED_DATABASES.put("sqlserver2012", new DatabaseProperties("sqlserver2012", "mssql", "2012"))
        SUPPORTED_DATABASES.put("sqlserver2014", new DatabaseProperties("sqlserver2014", "mssql", "2014"))
        SUPPORTED_DATABASES.put("sqlserver2016", new DatabaseProperties("sqlserver2016", "mssql", "2016"))
    }

    TeamCityExtension(Project project)
    {
        this.project = project
        setDatabaseProperties()
        setValidationMessages()
    }

    static Boolean isDatabaseSupported(String database)
    {
        return SUPPORTED_DATABASES.containsKey(database)
    }

    Boolean isValidForTestRun()
    {
        return validationMessages.isEmpty()
    }

    void setValidationMessages()
    {
        if (getTeamCityProperty("suite").isEmpty())
            validationMessages.add("'suite' property not specified")

        if (getTeamCityProperty("tomcat.home").isEmpty())
            validationMessages.add("'tomcat.home' property not specified")
        if (getTeamCityProperty("tomcat.port").isEmpty())
            validationMessages.add("'tomcat.port' property not specified")
        if (this.databaseTypes.isEmpty())
            validationMessages.add("'database.types' property not specified or does not specify a supported database.  Must be one of: ${SUPPORTED_DATABASES.keySet().join(", ")}.")
        if (getTeamCityProperty('agent.name').isEmpty())
            validationMessages.add("'agent.name' property not specified")
        if (getTeamCityProperty('teamcity.projectName').isEmpty())
            validationMessages.add("'teamcity.projectName' property not specified")
        if (getTeamCityProperty('tomcat.debug').isEmpty())
            validationMessages.add("'tomcat.debug' property (for debug port) not specified")
    }

    private void setDatabaseProperties()
    {
        if ((Boolean) getTeamCityProperty("build.is.personal", false))
        {
            this.databaseName = "LabKey_PersonalBuild"
            this.dropDatabase = true
        }
        else
        {
            String name = getTeamCityProperty("teamcity.buildType.id")
            if (!(Boolean) getTeamCityProperty("teamcity.build.branch.is_default", true))
                name = "${getTeamCityProperty('teamcity.build.branch')}_${name}"
            this.databaseName = name.replaceAll("[/\\.\\s-]", "_")
            String dbProperty = getTeamCityProperty('drop.database')
            this.dropDatabase = dbProperty.equals("1") || dbProperty.equalsIgnoreCase("true")
        }
        String databaseTypesProp = getTeamCityProperty("database.types")
        Boolean databaseAvailable = false
        if (!databaseTypesProp.isEmpty())
        {
            for (String type : databaseTypesProp.split(","))
            {
                if (SUPPORTED_DATABASES.containsKey(type))
                {
                    if (CUSTOM_DB_PROPS.equals(type) || (Boolean) getTeamCityProperty("database.${type}", false))
                    {
                        DatabaseProperties props = SUPPORTED_DATABASES.get(type)
                        if (props == null)
                        {
                            props = new DatabaseProperties(project, false);
                        }
                        else
                        {
                            props.setProject(project)
                        }
                        props.jdbcDatabase = getDatabaseName()
                        if (!getTeamCityProperty("database.${type}.jdbcURL").isEmpty())
                        {
                            props.setJdbcURL(getTeamCityProperty("database.${type}.jdbcURL"))
                            if (getTeamCityProperty("database.${type}.port").isEmpty() && this.dropDatabase)
                                validationMessages.add("'database.${type}.port' not specified. Unable to drop database.")
                        }
                        else if (getTeamCityProperty("database.${type}.port").isEmpty())
                            validationMessages.add("database.${type}.jdbcURL and database.${type}.port not specified. Connection not possible.")
                        if (!getTeamCityProperty("database.${type}.port").isEmpty())
                            props.setJdbcPort(getTeamCityProperty("database.${type}.port"))
                        this.databaseTypes.add(props)
                        databaseAvailable = true
                    }
                }
            }
            if (!databaseAvailable)
            {
                validationMessages.add("None of the selected databases (${databaseTypesProp}) is supported on this server.")
            }
        }
    }

    static boolean isOnTeamCity(Project project)
    {
        return project.hasProperty('teamcity')
    }

    String getTeamCityProperty(String name)
    {
        return getTeamCityProperty(name, "")
    }

    Object getTeamCityProperty(String name, Object defaultValue)
    {
        getTeamCityProperty(project, name, defaultValue)
    }

    static Object getTeamCityProperty(Project project, String name, Object defaultValue)
    {
        if (isOnTeamCity(project))
            return project.teamcity[name] != null ? project.teamcity[name] : defaultValue
        else if (project.hasProperty(name))
            return project.property(name)
        else
            return defaultValue
    }
}
