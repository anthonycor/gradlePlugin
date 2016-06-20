package org.labkey.test.pages.@@MODULE_LOWERCASE_NAME@@;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class BeginPage extends LabKeyPage
{
    Elements _elements;

    public BeginPage(BaseWebDriverTest test)
    {
        super(test);
    }

    public static BeginPage beginAt(BaseWebDriverTest test, String containerPath)
    {
        test.beginAt(WebTestHelper.buildURL("@@MODULE_LOWERCASE_NAME@@", containerPath, "begin"));
        return new BeginPage(test);
    }

    public Elements elements()
    {
        if (_elements == null)
            _elements = new Elements();
        return _elements;
    }

    private class Elements extends LabKeyPage.ElementCache
    {
        WebElement example = new LazyWebElement(Locator.css("button"), this);
    }
}
