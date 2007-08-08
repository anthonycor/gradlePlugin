package org.labkey.@@MODULE_LOWERCASE_NAME@@;

import org.apache.log4j.Logger;
import org.labkey.api.security.ACL;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.view.*;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.validation.BindException;

public class @@MODULE_NAME@@Controller extends SpringActionController
{
    static DefaultActionResolver _actionResolver = new DefaultActionResolver(@@MODULE_NAME@@Controller.class);

    public @@MODULE_NAME@@Controller() throws Exception
    {
        super();
        setActionResolver(_actionResolver.getInstance(this));
    }

    @RequiresPermission(ACL.PERM_READ)
    public class BeginAction extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            return new JspView("/org/labkey/@@MODULE_LOWERCASE_NAME@@/view/hello.jsp");
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root;
        }
    }
}