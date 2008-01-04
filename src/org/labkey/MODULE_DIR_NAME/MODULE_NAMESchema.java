package org.labkey.@@MODULE_LOWERCASE_NAME@@;

import org.labkey.api.data.DbSchema;
import org.labkey.api.data.SqlDialect;

public class @@MODULE_NAME@@Schema
{
    private static @@MODULE_NAME@@Schema _instance = null;

    public static @@MODULE_NAME@@Schema getInstance()
    {
        if (null == _instance)
            _instance = new @@MODULE_NAME@@Schema();

        return _instance;
    }

    private @@MODULE_NAME@@Schema()
    {
        // private contructor to prevent instantiation from
        // outside this class: this singleton should only be
        // accessed via org.labkey.@@MODULE_LOWERCASE_NAME@@.@@MODULE_NAME@@Schema.getInstance()
    }

    public DbSchema getSchema()
    {
        return DbSchema.get("@@MODULE_LOWERCASE_NAME@@");
    }

    public SqlDialect getSqlDialect()
    {
        return getSchema().getSqlDialect();
    }
}
