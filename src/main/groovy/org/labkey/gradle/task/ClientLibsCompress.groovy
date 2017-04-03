package org.labkey.gradle.task

import com.yahoo.platform.yui.compressor.CssCompressor
import com.yahoo.platform.yui.compressor.JavaScriptCompressor
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.labkey.gradle.plugin.LabKeyExtension
import org.xml.sax.Attributes
import org.xml.sax.SAXException
import org.xml.sax.helpers.DefaultHandler

import javax.xml.parsers.SAXParser
import javax.xml.parsers.SAXParserFactory
import java.nio.charset.StandardCharsets

class ClientLibsCompress extends DefaultTask
{
    public static final String LIB_XML_EXTENSION = ".lib.xml"

    File workingDir = new File((String) project.labkey.explodedModuleWebDir)

    // TODO get rid of this
    @OutputFile
    File upToDateFile = new File(workingDir, ".clientlibrary.uptodate")

    ClientLibsCompress()
    {
    }

    @InputFiles
    FileTree getLibXmlFiles()
    {
        return project.fileTree(dir: workingDir, includes: ["**/*${LIB_XML_EXTENSION}"])
    }

    @TaskAction
    void compressAllFiles()
    {
        FileTree libXmlFiles = getLibXmlFiles()
        libXmlFiles.files.each() {
            File file -> compressSingleFile(file)
        }

        // this file is used to determine if things are up to date
        FileUtils.touch(upToDateFile)
    }

    void compressSingleFile(File xmlFile)
    {
        XmlImporter importer = parseXmlFile(xmlFile)
        compressFiles(xmlFile, importer)
    }

    private XmlImporter parseXmlFile(File xmlFile)
    {
        try
        {
            // XMLBeans would be cleaner, but I'm not sure if this
            // step should depend on schemas.jar.  should investigate further
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setValidating(true);
            SAXParser parser = factory.newSAXParser();
            XmlImporter importer = new XmlImporter(xmlFile, xmlFile.getParentFile(), workingDir);
            parser.parse(xmlFile, importer);
            return importer;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private void compressFiles(File xmlFile, XmlImporter importer)
    {
        if (importer.doCompile)
        {
            try
            {
                if (importer.getJavaScriptFiles().size() > 0)
                    compileScripts(xmlFile, importer.getJavaScriptFiles(), "js");
                if (importer.getCssFiles().size() > 0)
                    compileScripts(xmlFile, importer.getCssFiles(), "css");
            }
            catch (Exception e)
            {
                throw new SAXException(e);
            }
        }
        else
        {
            project.logger.info("No compile necessary");
        }
    }

    void compileScripts(File xmlFile, Set<File> srcFiles, String extension) throws IOException, InterruptedException
    {
        File minFile = getOutputFile(xmlFile, "min", extension);

        if (upToDate(xmlFile, minFile, srcFiles))
        {
            project.logger.info("files are up to date");
            return;
        }

        project.logger.info("Concatenating " + extension + " files into single file: ");
        File concatFile = getOutputFile(xmlFile, "combined", extension);
        concatenateFiles(srcFiles, concatFile);

        project.logger.info("Minifying " + extension + " files with YUICompressor: ");
        minFile.delete();

        minifyFile(concatFile, minFile);

        concatFile.delete();

        if (!LabKeyExtension.isDevMode(project))
        {
            project.logger.info("Compressing " + extension + " files");
            project.ant.gzip(
                    src: minFile,
                    destfile: "${minFile.toString()}.gz"
            )
        }
    }

    private File getOutputFile(File xmlFile, String token, String ex)
    {
        return new File(xmlFile.getParentFile(), xmlFile.getName().replaceAll(LIB_XML_EXTENSION, "." + token + "." + ex));
    }

    private void concatenateFiles(Set<File> files, File output)
    {
        try
        {
            output.createNewFile();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        PrintWriter saveAs = null
        try
        {
            saveAs = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output), StandardCharsets.UTF_8)))

            for (File f : files)
            {
                BufferedReader readBuff = null
                try
                {
                    readBuff = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))

                    String line = readBuff.readLine();

                    while (line != null)
                    {
                        saveAs.println(line);
                        line = readBuff.readLine();
                    }
                }
                finally
                {
                    IOUtils.closeQuietly(readBuff)
                }

            }
        }
        finally
        {
            IOUtils.closeQuietly(saveAs)
        }
    }


    private List<String> getCompressorErrors(Set<File> srcFiles) throws IOException
    {
        List<String> allErrors = new ArrayList<>();
        for (File srcFile : srcFiles)
        {
            File minFile = File.createTempFile(srcFile.getName(), null);
            minFile.deleteOnExit();

            List<String> errors = minifyFile(srcFile, minFile);

            if (!errors.isEmpty())
                allErrors.addAll(errors);
        }
        return allErrors;
    }

    // TODO convert to Gradle check with input and output file designations
    private boolean upToDate(File xmlFile, File destFile, Set<File> srcFiles)
    {
        long ts = 0;
        long lastModified;

        if (destFile.exists())
            ts = destFile.lastModified();
        else
            return false;

        //test the xml file itself
        lastModified = xmlFile.lastModified();
        project.logger.debug("${xmlFile}  is ${lastModified < ts ? "older" : "newer"}  than ${destFile.getPath()}");
        if (lastModified > ts)
            return false;

        //then test resources
        for (File srcFile : srcFiles)
        {
            if (srcFile.exists())
            {
                lastModified = srcFile.lastModified();
                project.logger.debug("${srcFile.getPath()}  is ${ (lastModified < ts ? "older" : "newer") }  than ${destFile.getPath()}");
                if (lastModified > ts)
                    return false;
            }
            else
            {
                project.logger.error("File does not exist: " + srcFile.getPath());
            }
        }
        return true;
    }

    private void minifyFile(File srcFile, File destFile) throws IOException
    {
        if (srcFile.getName().endsWith("js"))
        {
            srcFile.withReader { reader ->
                JavaScriptCompressor compressor = new JavaScriptCompressor(reader, null)

                destFile.withWriter { writer ->
                    compressor.compress(writer, null, -1, true, false, false, false)
                }
            }
        }
        else
        {
            srcFile.withReader { reader ->
                CssCompressor compressor = new CssCompressor(reader)
                destFile.withWriter { writer ->
                    compressor.compress(writer, -1)
                }
            }
        }
    }

    private class XmlImporter extends DefaultHandler
    {
        private boolean withinScriptsTag = false;
        private File xmlFile;
        private File sourceDir;
        private LinkedHashSet<File> javaScriptFiles = new LinkedHashSet<>()
        private LinkedHashSet<File> cssFiles = new LinkedHashSet<>()
        private boolean doCompile = true

        XmlImporter(File xml, File outputDir, File sourceDir)
        {
            xmlFile = xml;
            this.sourceDir = sourceDir;
        }

        LinkedHashSet<File> getJavaScriptFiles()
        {
            return javaScriptFiles
        }

        LinkedHashSet<File> getCssFiles()
        {
            return cssFiles
        }

        boolean getDoCompile()
        {
            return doCompile
        }

        void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
        {
            if ("library".equals(localName))
            {
                if (attributes.getValue("compileInProductionMode") != null)
                {
                    doCompile = Boolean.parseBoolean(attributes.getValue("compileInProductionMode"));
                }
                withinScriptsTag = true;
            }
            if (withinScriptsTag && "script".equals(localName))
            {
                String path = attributes.getValue("path");
                File scriptFile = new File(sourceDir, path);
                if (!scriptFile.exists())
                {
                    if (isExternalScript(path))
                    {
                        throw new RuntimeException("ERROR: External scripts (e.g. https://.../script.js) cannot be declared in library definition. Consider making it a <dependency>.");
                    }
                    else
                    {
                        throw new RuntimeException("ERROR: Unable to find script file: " + scriptFile + " from library: " + xmlFile);
                    }
                }
                else
                {
                    //linux will be case-sensitive, so we proactively throw errors on any filesystem
                    try
                    {
                        File f = org.apache.tools.ant.util.FileUtils.getFileUtils().normalize(scriptFile.getPath());
                        if( !scriptFile.getCanonicalFile().getName().equals(f.getName()))
                        {
                            throw new RuntimeException("File must be a case-sensitive match. Found: " + scriptFile.getAbsolutePath() + ", expected: " + scriptFile.getCanonicalPath());
                        }
                    }
                    catch (IOException e)
                    {
                        throw new RuntimeException(e);
                    }

                    if (scriptFile.getName().endsWith(".js"))
                        javaScriptFiles.add(scriptFile);
                    else if (scriptFile.getName().endsWith(".css"))
                        cssFiles.add(scriptFile);
                    else
                        project.logger.info("Unknown file extension, ignoring: " + scriptFile.getName());
                }
            }
        }

        void endElement(String uri, String localName, String qName) throws SAXException
        {
            if ("library".equals(localName))
                withinScriptsTag = false;
        }

        private boolean isExternalScript(String path)
        {
            return path != null && (path.contains("http://") || path.contains("https://"));
        }
    }
}
