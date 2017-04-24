package org.labkey.gradle.plugin.extension

/**
 * Created by susanh on 4/23/17.
 */
class JsDocExtension
{
    String root
    String[] paths = ["api/webapp/clientapi",
                      "api/webapp/clientapi/dom",
                      "api/webapp/clientapi/core",
                      "api/webapp/clientapi/ext3",
                      "api/webapp/clientapi/ext4",
                      "api/webapp/clientapi/ext4/data",
                      "internal/webapp/labkey.js",
                      "modules/visualization/resources/web/vis/genericChart/genericChartHelper.js",
                      "modules/visualization/resources/web/vis/timeChart/timeChartHelper.js",
                      "internal/webapp/vis/src"]
    String outputDir
}
