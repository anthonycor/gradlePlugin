package org.labkey.gradle.plugin.extension

import org.gradle.api.Project
import org.labkey.gradle.util.BuildUtils
import org.labkey.gradle.util.PropertiesUtils

import java.text.SimpleDateFormat

/**
 * Created by susanh on 4/23/17.
 */
class ModuleExtension
{
    private static final String ENLISTMENT_PROPERTIES = "enlistment.properties"
    public static final String MODULE_PROPERTIES_FILE = "module.properties"
    private Properties modProperties
    private Project project

    ModuleExtension(Project project)
    {
        this.project = project
        setModuleProperties(project)
    }

    Project getProject()
    {
        return project
    }

    Properties getModProperties()
    {
        return modProperties
    }

    String getPropertyValue(String propertyName, String defaultValue)
    {
        String value = modProperties.getProperty(propertyName)
        return value == null ? defaultValue : value

    }

    String getPropertyValue(String propertyName)
    {
        return getPropertyValue(propertyName, null)
    }

    Object get(String propertyName)
    {
        return modProperties.get(propertyName)
    }

    void setModuleProperties(Project project)
    {
        File propertiesFile = project.file(MODULE_PROPERTIES_FILE)
        this.modProperties = new Properties()
        PropertiesUtils.readProperties(propertiesFile, this.modProperties)

        if (modProperties.getProperty("Version") == null)
        // remove -SNAPSHOT and any feature branch prefix from the module version number
        // because the module loader does not expect or handle decorated version numbers
            modProperties.setProperty("Version", BuildUtils.getLabKeyModuleVersion(project))

        setBuildInfoProperties()
        setModuleInfoProperties()
        setVcsProperties()
        setEnlistmentId()

    }

    private void setVcsProperties()
    {
        modProperties.putAll(BuildUtils.getStandardVCSProperties(project))
    }

    private setEnlistmentId()
    {
        if (!LabKeyExtension.isDevMode(project))
            return

        File enlistmentFile = new File(project.getRootProject().getProjectDir(), ENLISTMENT_PROPERTIES)
        Properties enlistmentProperties = new Properties()
        if (!enlistmentFile.exists())
        {
            UUID id = UUID.randomUUID()
            enlistmentProperties.setProperty("enlistment.id", id.toString())
            enlistmentProperties.store(new FileWriter(enlistmentFile), SimpleDateFormat.getDateTimeInstance().format(new Date()))
        }
        else
        {
            PropertiesUtils.readProperties(enlistmentFile, enlistmentProperties)
        }
        modProperties.setProperty("EnlistmentId", enlistmentProperties.getProperty("enlistment.id"))
    }

    private void setBuildInfoProperties()
    {
        modProperties.setProperty("RequiredServerVersion", "0.0")
        if (modProperties.getProperty("BuildType") == null)
            modProperties.setProperty("BuildType", LabKeyExtension.getDeployModeName(project))
        modProperties.setProperty("BuildUser", System.getProperty("user.name"))
        modProperties.setProperty("BuildOS", System.getProperty("os.name"))
        modProperties.setProperty("BuildTime", SimpleDateFormat.getDateTimeInstance().format(new Date()))
        modProperties.setProperty("BuildPath", project.buildDir.getAbsolutePath())
        if (LabKeyExtension.isDevMode(project))
            modProperties.setProperty("SourcePath", project.projectDir.getAbsolutePath())
        modProperties.setProperty("ResourcePath", "") // TODO  _project.getResources().... ???
        boolean isExternalModule = project.projectDir.getAbsolutePath().contains("externalModules")
        if (modProperties.getProperty("ConsolidateScripts") == null)
        {
            if (isExternalModule)
                modProperties.setProperty("ConsolidateScripts", "false")
            else
                modProperties.setProperty("ConsolidateScripts", "true")
        }
        if (modProperties.getProperty("ManageVersion") == null)
        {
            if (isExternalModule)
                modProperties.setProperty("ManageVersion", "false")
            else
                modProperties.setProperty("ManageVersion", "true")
        }
    }

    private void setModuleInfoProperties()
    {
        if (modProperties.getProperty("Name") == null)
            modProperties.setProperty("Name", project.name)
        if (modProperties.getProperty("ModuleClass") == null)
            modProperties.setProperty("ModuleClass", "org.labkey.api.module.SimpleModule")
    }
}
