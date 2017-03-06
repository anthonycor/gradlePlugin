package org.labkey.gradle.task

import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.xml.sax.Attributes
import org.xml.sax.SAXException
import org.xml.sax.helpers.DefaultHandler

import javax.xml.parsers.SAXParser
import javax.xml.parsers.SAXParserFactory
import java.nio.charset.StandardCharsets

class ClientLibsCompress extends DefaultTask
{

    public static final String LIB_XML_EXTENSION = ".lib.xml"

    File workingDir = project.clientLibs.workingDir

    String yuiCompressorJar

    // TODO get rid of this
    @OutputFile
    File upToDateFile = new File(workingDir, ".clientlibrary.uptodate")

    ClientLibsCompress()
    {
        // TODO get rid of this in favor of porting to Gradle.
        ant.taskdef(
                name: "compressClientLibs",
                classname: "labkey.ant.ClientLibraryBuilder",
                classpath: "${project.labkey.externalDir}/ant/labkeytasks/labkeytasks.jar"
        )
        yuiCompressorJar = "${project.labkey.externalLibDir}/build/yuicompressor-${project.yuiCompressorVersion}.jar"
    }

    @InputFiles
    FileTree getLibXmlFiles()
    {
        return project.fileTree(dir: workingDir,
                includes: ["**/*${LIB_XML_EXTENSION}"]
        )
    }

    @TaskAction
    void compressAllFiles()
    {
        FileTree libXmlFiles = getLibXmlFiles()
        libXmlFiles.files.each() {
            File file ->
                ant.compressClientLibs(
                        srcFile: file.getAbsolutePath(),
                        sourcedir: workingDir,
                        yuicompressorjar: yuiCompressorJar
                )
                // uncomment this when we have completely converted the ClientLibaryBuilder class to Groovy
//                compressSingleFile(file.getAbsolutePath())
        }

        // this file is used by the ClientLibraryBuilder to determine if things are up to date
        FileUtils.touch(upToDateFile)
    }



    void compressSingleFile(String sourceFile)
    {
        File xmlFile = new File(sourceFile);

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
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private class XmlImporter extends DefaultHandler
    {
        private LinkedHashSet<File> _js = new LinkedHashSet<>();
        private LinkedHashSet<File> _css = new LinkedHashSet<>();
        private boolean _doCompile = true;
        private boolean _withinScriptsTag = false;
        private File _xmlFile;
        private File _outputDir;
        private File _sourceDir;

        public XmlImporter(File xml, File outputDir, File sourceDir)
        {
            _xmlFile = xml;
            _sourceDir = sourceDir;
            _outputDir = outputDir;
        }

        public void endDocument() throws SAXException
        {
            if (_doCompile)
            {
                try
                {
                    if(_js.size() > 0)
                        compileScripts(_js, "js");
                    if(_css.size() > 0)
                        compileScripts(_css, "css");
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

        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
        {
            if ("library".equals(localName))
            {
                if(attributes.getValue("compileInProductionMode") != null)
                {
                    _doCompile = Boolean.parseBoolean(attributes.getValue("compileInProductionMode"));
                }
                _withinScriptsTag = true;
            }
            if (_withinScriptsTag && "script".equals(localName))
            {
                String path = attributes.getValue("path");
                File scriptFile = new File(_sourceDir, path);
                if (!scriptFile.exists())
                {
                    if (isExternalScript(path))
                    {
                        throw new RuntimeException("ERROR: External scripts (e.g. https://.../script.js) cannot be declared in library definition. Consider making it a <dependency>.");
                    }
                    else
                    {
                        throw new RuntimeException("ERROR: Unable to find script file: " + scriptFile + " from library: " + _xmlFile);
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
                        _js.add(scriptFile);
                    else if (scriptFile.getName().endsWith(".css"))
                        _css.add(scriptFile);
                    else
                        project.logger.info("Unknown file extension, ignoring: " + scriptFile.getName());
                }
            }
        }

        public void endElement(String uri, String localName, String qName) throws SAXException
        {
            if ("library".equals(localName))
                _withinScriptsTag = false;
        }

        private File getOutputFile(String token, String ex)
        {
            return new File(_outputDir, _xmlFile.getName().replaceAll(LIB_XML_EXTENSION, "." + token + "." + ex));
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

        private boolean isExternalScript(String path)
        {
            return path != null && (path.contains("http://") || path.contains("https://"));
        }

        private void compileScripts(Set<File> srcFiles, String extension) throws IOException, InterruptedException
        {
            // Starting with 16.1, build.xml passes in the yuicompressor.jar location as a parameter.
            if (null == yuiCompressorJar)
                throw new RuntimeException("Must specify yuicompressor jar location via \"yuicompressorjar\" parameter");

            File minFile = getOutputFile("min", extension);

            if(upToDate(minFile, srcFiles))
            {
                project.logger.info("files are up to date");
                return;
            }

            project.logger.info("Concatenating " + extension + " files into single file: ");
            File concatFile = getOutputFile("combined", extension);
            concatenateFiles(srcFiles, concatFile);

            project.logger.info("Minifying " + extension + " files with YUICompressor: ");
            minFile.delete();

            List<String> errors = minifyFile(concatFile, minFile);

            concatFile.delete();

            if (!errors.isEmpty() || !minFile.exists())
            {
                project.logger.error("YUICompressor errors");
                project.logger.error(String.join("\n", getCompressorErrors(srcFiles)));
                throw new RuntimeException("ERROR: YUICompressor did not run properly for '" + extension + "' files in " +
                        _xmlFile.getName() + ". See log for more details.");
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
        private boolean upToDate(File destFile, Set<File> srcFiles)
        {
            long ts = 0;
            long lastModified;

            if (destFile.exists())
                ts = destFile.lastModified();
            else
                return false;

            //test the xml file itself
            lastModified = _xmlFile.lastModified();
            project.logger.debug("${_xmlFile}  is ${lastModified < ts ? "older" : "newer"}  than ${destFile.getPath()}");
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
    }

    private List<String> minifyFile(File srcFile, File destFile) throws IOException
    {
        // TODO convert this to a javaexec action.
        File j = new File(yuiCompressorJar);
        if (!j.exists())
            throw new RuntimeException("YUI JAR not found at expected location: " + j.getAbsolutePath());

        List<String> params = new LinkedList<>();
        params.add("java");
        params.add("-Dfile.encoding=UTF-8");
        params.add("-jar");
        params.add(j.getAbsolutePath());
        params.add("-o");
        params.add(destFile.getPath());
        params.add(srcFile.getPath());

        ProcessBuilder pb = new ProcessBuilder(params);
        Process p = pb.start();

        List<String> errors = new ArrayList<>();
        BufferedReader reader = null
        try
        {
            reader = new BufferedReader(new InputStreamReader(p.getErrorStream(), "US-ASCII"))

            String line;
            while ((line = reader.readLine()) != null)
            {
                errors.add(line);
            }
            return errors;
        }
        finally
        {
            IOUtils.closeQuietly(reader)
        }
    }

}
