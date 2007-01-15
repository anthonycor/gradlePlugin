package org.labkey.@@MODULE_LOWERCASE_NAME@@;

import org.labkey.api.module.DefaultModule;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.Container;
import org.labkey.api.data.DbSchema;
import org.labkey.api.view.WebPartFactory;
import org.labkey.api.view.WebPartView;
import org.labkey.api.view.Portal;
import org.labkey.api.view.ViewContext;
import org.labkey.api.util.PageFlowUtil;
import org.apache.log4j.Logger;

import java.beans.PropertyChangeEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class @@MODULE_NAME@@Module extends DefaultModule implements ContainerManager.ContainerListener
{
    private static final Logger _log = Logger.getLogger(@@MODULE_NAME@@Module.class);
    public static final String NAME = "@@MODULE_NAME@@";

    private static final String[] RECOGNIZED_PAGEFLOWS = new String[]{"@@MODULE_LOWERCASE_NAME@@"};

    public @@MODULE_NAME@@Module()
    {
        super(NAME, 0.01, "/@@MODULE_LOWERCASE_NAME@@", new WebPartFactory[0]);
        addController("@@MODULE_LOWERCASE_NAME@@", @@MODULE_NAME@@Controller.class);
    }

    public void containerCreated(Container c)
    {
    }

    public void containerDeleted(Container c)
    {
    }

    public Collection<String> getSummary(Container c)
    {
        return Collections.emptyList();
    }

    public void propertyChange(PropertyChangeEvent evt)
    {
    }

    public void startup(ModuleContext moduleContext)
    {
        super.startup(moduleContext);
        // add a container listener so we'll know when our container is deleted:
        ContainerManager.addContainerListener(this);
    }

    public Set<DbSchema> getSchemasToTest()
    {
        return PageFlowUtil.set(@@MODULE_NAME@@Schema.getInstance().getSchema());
    }
}