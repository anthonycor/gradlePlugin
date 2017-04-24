package org.labkey.gradle.plugin.extension

/**
 * Created by susanh on 4/23/17.
 */
class TomcatExtension
{
    String assertionFlag = "-ea" // set to -da to disable assertions and -ea to enable assertions
    String maxMemory = "1G"
    boolean recompileJsp = true
    String trustStore = ""
    String trustStorePassword = ""
    String catalinaOpts = ""
    String debugPort = null // this is used for TeamCity catalina options
}
