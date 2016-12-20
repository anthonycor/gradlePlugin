package org.labkey.gradle.util

import org.gradle.api.Project

import java.util.regex.Matcher
import java.util.regex.Pattern

class PropertiesUtils
{
    public static final Pattern PROPERTY_PATTERN = Pattern.compile("@@([^@]+)@@")
    private static final Pattern VALUE_PATTERN = Pattern.compile("(\\\$\\{\\w*\\})")

    static Properties readFileProperties(Project project, String fileName)
    {
        Properties props = new Properties()
        props.load(new FileInputStream(project.file(fileName)))
        return props;
    }

    static String parseCompositeProp(Properties props, String prop)
    {
        Matcher valMatcher = VALUE_PATTERN.matcher(prop);
        while (valMatcher.find())
        {
            String p = valMatcher.group(1).replace("\${", "").replace("}", "");
            prop = prop.replace(valMatcher.group(1), (String)(props.getProperty(p)));
        }
        return prop;
    }

    static String parseCompositeProp(Map<String, Object> props, String prop)
    {
        Properties propWrapper = new Properties();
        propWrapper.putAll(props);
        return parseCompositeProp(propWrapper, prop);
    }

    static void replaceDatabaseProperty(Project project, String name, String value)
    {
        project.ant.propertyfile(
                file: "${project.project(":server")}/config.properties"
        )
                {
                    entry( key: name, value: value)
                }
    }

    //Convenience method. Mimics ant's "<property file="${basedir}/config.properties"/>"
    static Properties readDatabaseProperties(Project project)
    {
        Properties props = readFileProperties(project.project(":server"), "config.properties");
        return props;
    }

    static String replaceProps(String line, Properties props)
    {
        Matcher matcher = PROPERTY_PATTERN.matcher(line);
        while(matcher.find())
        {
            String propName = matcher.group(1);
            if (props.containsKey(propName))
            {
                line = line.replace("@@" + propName + "@@", props.get(propName).toString());
            }
        }
        return line;
    }

    static void readProperties(File propertiesFile, Properties properties)
    {
        if (propertiesFile.exists())
        {
            FileInputStream is;
            try
            {
                is = new FileInputStream(propertiesFile)
                properties.load(is)
            }
            finally
            {
                if (is != null)
                    is.close()
            }
        }
    }
}