/*
 * Copyright (c) 2016 LabKey Corporation
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

package org.labkey.test.components.@@MODULE_LOWERCASE_NAME@@;

import org.labkey.test.Locator;
import org.labkey.test.components.BodyWebPart;
import org.labkey.test.components.WebPart;
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class @@MODULE_NAME@@WebPart extends BodyWebPart
{
    private Elements _elements;

    public @@MODULE_NAME@@WebPart(WebDriver driver)
    {
        this(driver, 0);
    }

    public @@MODULE_NAME@@WebPart(WebDriver driver, int index)
    {
        super(driver, "@@MODULE_NAME@@", index); // Assuming your modules has a WebPart named @@MODULE_NAME@@
    }

    protected Elements elements()
    {
        if (null == _elements)
            _elements = new Elements();
        return _elements;
    }

    protected class Elements extends WebPart.Elements
    {
        protected WebElement example = new LazyWebElement(Locator.css("button"), this);
    }
}