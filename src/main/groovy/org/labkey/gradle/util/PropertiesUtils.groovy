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

    static String parseCompositeProp(Project project, Properties props, String prop)
    {
        if (props == null)
            project.logger.error("${project.path} Properties is null")
        else
        {
            Matcher valMatcher = VALUE_PATTERN.matcher(prop);
            while (valMatcher.find())
            {
                String p = valMatcher.group(1).replace("\${", "").replace("}", "")
                if (props.getProperty(p) != null)
                    prop = prop.replace(valMatcher.group(1), (String) (props.getProperty(p)))
                else
                    project.logger.error("Unable to find value for ${p} in ${props}")
            }
        }
        return prop;
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