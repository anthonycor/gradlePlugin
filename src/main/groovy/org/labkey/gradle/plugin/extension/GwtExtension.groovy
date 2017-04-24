package org.labkey.gradle.plugin.extension

import org.labkey.gradle.plugin.Gwt

/**
 * Created by susanh on 4/23/17.
 */
class GwtExtension
{
    String srcDir = Gwt.SOURCE_DIR
    String style = "OBF"
    String logLevel = "INFO"
    String extrasDir = "gwtExtras"
    Boolean draftCompile = false
    Boolean allBrowserCompile = true
}
