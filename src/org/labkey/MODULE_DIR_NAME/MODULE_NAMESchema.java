/*
 * Copyright (c) 2013 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.@@MODULE_LOWERCASE_NAME@@;

import org.labkey.api.data.DbSchema;
import org.labkey.api.data.dialect.SqlDialect;

public class @@MODULE_NAME@@Schema
{
    private static final @@MODULE_NAME@@Schema _instance = new @@MODULE_NAME@@Schema();

    public static @@MODULE_NAME@@Schema getInstance()
    {
        return _instance;
    }

    private @@MODULE_NAME@@Schema()
    {
        // private constructor to prevent instantiation from
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
