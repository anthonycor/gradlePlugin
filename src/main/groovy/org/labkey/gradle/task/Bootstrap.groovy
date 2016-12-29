package org.labkey.gradle.task

import org.labkey.gradle.util.DatabaseProperties
import org.labkey.gradle.util.SqlUtils

class Bootstrap extends DoThenSetup
{
    Closure<Void> fn = {
        setDatabaseProperties()

        SqlUtils.dropDatabase(this, dbProperties.getConfigProperties());
    }

    @Override
    protected void setDatabaseProperties()
    {
        dbProperties = new DatabaseProperties(project, true)
    }

}
