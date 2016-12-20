package org.labkey.gradle.util

/**
 * Created by susanh on 12/19/16.
 */
class DatabaseProperties
{
    String dbTypeAndVersion // e.g., postgres9.2
    String shortType // pg or mssql
    String version
    String jdbcURL
    String jdbcDatabase
    String jdbcPort
    String jdbcHost

    DatabaseProperties(String dbTypeAndVersion, String shortType, version)
    {
        this.dbTypeAndVersion = dbTypeAndVersion
        this.shortType = shortType
        this.version = version
    }
}
