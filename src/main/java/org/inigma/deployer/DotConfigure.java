package org.inigma.deployer;

import org.kamranzafar.jtar.TarEntry;
import org.kamranzafar.jtar.TarInputStream;
import org.kamranzafar.jtar.TarOutputStream;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * @author <a href='mailto:sejal@sejal.org">Sejal Patel</a>
 *         Date: 3/8/13 12:07 AM
 */
public class DotConfigure {
    private static ScriptEngine JAVASCRIPT = new ScriptEngineManager().getEngineByName("JavaScript");
    private static Logger logger = Logger.getLogger(DotConfigure.class.getName());

    static { // initialize the javascript engine with the doT.js implementation
        InputStream in = DotConfigure.class.getResourceAsStream("doT.js");
        try {
            JAVASCRIPT.eval(new InputStreamReader(in));
            JAVASCRIPT.eval("doT.templateSettings.strip = false");
            JAVASCRIPT.eval("doT.templateSettings.varname = 'it, env, prop'");
            JAVASCRIPT.eval("env = {}");
            for (String key : System.getenv().keySet()) {
                JAVASCRIPT.eval("env['" + key + "'] = '" + System.getenv(key) + "'");
            }
            JAVASCRIPT.eval("prop = {}");
            for (Object key : System.getProperties().keySet()) {
                JAVASCRIPT.eval("prop['" + key + "'] = '" + System.getProperty((String) key).replaceAll("\n",
                        "\\\\n").replaceAll("\r", "\\\\r") + "'");
            }
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws IOException {
        LogManager.getLogManager().readConfiguration(DotConfigure.class.getResourceAsStream("logging.properties"));
        if (args.length == 0) {
            System.out.println("Requires an argument which is the url to the deployment configuration.");
            System.exit(1);
        }
        for (String config : args) {
            logger.config("Invoking " + config);
            new DotConfigure(config).invoke();
        }
    }

    private DotConfiguration config;

    public DotConfigure(String configUrl) {
        if (configUrl == null) {
            throw new DotException("URL to configuration is required!");
        }
        try {
            URL url = getUrl(configUrl);
            initialize(url.openStream());
        } catch (IOException e) {
            throw new DotException("Unable to open resource located at " + configUrl, e);
        }
    }

    public DotConfigure(InputStream config) {
        initialize(config);
    }

    public DotConfiguration getConfig() {
        return config;
    }

    public void invoke() {
        File resource = downloadResource();
        File processedResource = null;
        if ("war".equalsIgnoreCase(config.getType()) || "jar".equalsIgnoreCase(config.getType())) {
            try {
                processedResource = processWar(new JarFile(resource));
            } catch (IOException e) {
                throw new DotException("Issue processing war: " + resource, e);
            }
        } else if ("zip".equalsIgnoreCase((config.getType()))) {
            try {
                processedResource = processZip(new ZipFile(resource));
            } catch (IOException e) {
                throw new DotException("Issue processing zip: " + resource, e);
            }
        } else if ("tar.gz".equalsIgnoreCase(config.getType())) {
            try {
                processedResource = processTarGz(resource);
            } catch (IOException e) {
                throw new DotException("Issue processing tarball: " + resource, e);
            }
        } else {
            logger.warning("Terminating, unknown handling of type " + config.getType());
            return;
        }

        executeScripts(config.getPreDeploy());

        deploy(processedResource);

        executeScripts(config.getPostDeploy());
    }

    public File processTarGz(File resource) throws IOException {
        String ext = config.getTemplateExtension();
        Set<String> ignores = new HashSet<String>();
        File temp = File.createTempFile("dot-", ".tar.gz");
        temp.mkdir();

        TarEntry entry;
        TarInputStream tis = new TarInputStream(new GZIPInputStream(new FileInputStream(resource)));
        while ((entry = tis.getNextEntry()) != null) { // gather up list of things to ignore
            String name = entry.getName();
            if (name.endsWith(ext)) {
                ignores.add(name.substring(0, name.lastIndexOf(ext)));
            }
        }
        tis.close();

        TarOutputStream tos = new TarOutputStream(new GZIPOutputStream(new FileOutputStream(temp)));
        tis = new TarInputStream(new GZIPInputStream(new FileInputStream(resource)));
        while ((entry = tis.getNextEntry()) != null) {
            String name = entry.getName();
            if (ignores.contains(name)) { // no reason to do anything with a file that doesn't belong
                continue;
            }

            TarEntry newEntry = new TarEntry(entry.getHeader());
            InputStream source = tis;
            if (name.endsWith(ext)) {
                newEntry.setName(name.substring(0, name.lastIndexOf(ext))); // new name
                ByteArrayInputStream bais = (ByteArrayInputStream) processTemplate(tis);
                newEntry.setSize(bais.available());
                source = bais;
            }
            tos.putNextEntry(newEntry);
            copy(source, tos);
        }
        tos.close();

        return temp;
    }

    public InputStream processTemplate(InputStream in) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        copy(in, baos);
        SimpleBindings bindings = new SimpleBindings();
        bindings.putAll(JAVASCRIPT.getBindings(ScriptContext.ENGINE_SCOPE));
        bindings.put("template", baos.toString());
        bindings.put("config", config.getConfig());
        try {
            String result = (String) JAVASCRIPT.eval("doT.template(template)(config, env, prop)", bindings);
            return new ByteArrayInputStream(result.getBytes());
        } catch (ScriptException e) {
            throw new DotException("Unable to evaluate template", e);
        }
    }

    public File processWar(JarFile jarFile) {
        String ext = config.getTemplateExtension();
        Enumeration<JarEntry> entries = jarFile.entries();
        Set<String> ignores = new HashSet<String>();
        while (entries.hasMoreElements()) {
            JarEntry jarEntry = entries.nextElement();
            String name = jarEntry.getName();
            if (name.endsWith(ext)) {
                ignores.add(name.substring(0, name.lastIndexOf(ext)));
            }
        }

        entries = jarFile.entries();
        try {
            File temp = File.createTempFile("dot-", ".war");
            JarOutputStream jos = new JarOutputStream(new FileOutputStream(temp));
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (ignores.contains(entry.getName())) { // template exists so don't copy over the original
                    continue;
                }
                String name = entry.getName();
                if (name.endsWith(ext)) {
                    JarEntry newentry = new JarEntry(name.substring(0, name.lastIndexOf(ext)));
                    newentry.setTime(entry.getTime());
                    jos.putNextEntry(newentry);
                    copy(processTemplate(jarFile.getInputStream(entry)), jos);
                    jos.closeEntry();
                } else {
                    jos.putNextEntry(entry);
                    copy(jarFile.getInputStream(entry), jos);
                    jos.closeEntry();
                }
            }
            jos.close();
            return temp;
        } catch (IOException e) {
            throw new DotException(e);
        }
    }

    public File processZip(ZipFile zipFile) {
        String ext = config.getTemplateExtension();
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        Set<String> ignores = new HashSet<String>();
        while (entries.hasMoreElements()) {
            ZipEntry jarEntry = entries.nextElement();
            String name = jarEntry.getName();
            if (name.endsWith(ext)) {
                ignores.add(name.substring(0, name.lastIndexOf(ext)));
            }
        }

        entries = zipFile.entries();
        try {
            File temp = File.createTempFile("dot-", ".zip");
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(temp));
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (ignores.contains(entry.getName())) { // template exists so don't copy over the original
                    continue;
                }
                String name = entry.getName();
                if (name.endsWith(ext)) {
                    ZipEntry newentry = new ZipEntry(name.substring(0, name.lastIndexOf(ext)));
                    newentry.setTime(entry.getTime());
                    zos.putNextEntry(newentry);
                    copy(processTemplate(zipFile.getInputStream(entry)), zos);
                    zos.closeEntry();
                } else {
                    zos.putNextEntry(entry);
                    copy(zipFile.getInputStream(entry), zos);
                    zos.closeEntry();
                }
            }
            zos.close();
            return temp;
        } catch (IOException e) {
            throw new DotException(e);
        }
    }

    private String inputToString(InputStream in) {
        StringBuilder sb = new StringBuilder();
        int read;
        byte[] buffer = new byte[4096];
        try {
            while ((read = in.read(buffer)) > 0) {
                sb.append(new String(buffer, 0, read));
            }
        } catch (IOException e) {
            throw new DotException(e);
        }
        return sb.toString();
    }

    private void copy(InputStream in, OutputStream out) {
        byte[] buffer = new byte[4096];
        int read = -1;
        try {
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
        } catch (IOException e) {
            throw new DotException(e);
        }
    }

    private void deploy(File resource) {
        try {
            URL url = getUrl(inputToString(processTemplate(new ByteArrayInputStream(config.getDeploy().getBytes()))));
            logger.config("Deploying to " + url);
            if ("file".equals(url.getProtocol())) {
                FileOutputStream fos = new FileOutputStream(url.getPath());
                FileInputStream fis = new FileInputStream(resource);
                copy(fis, fos);
                fis.close();
                fos.close();
            // } else if ("http".equals(url.getProtocol()) || "https".equals(url.getProtocol())) {
            // } else if ("ftp".equals(url.getProtocol())) {
            // } else if ("scp".equals(url.getProtocol())) {
            } else {
                logger.warning("Protocol '" + url.getProtocol() + "' not yet supported");
            }
        } catch (IOException e) {
            throw new DotException(e);
        }
    }

    private File downloadResource() {
        String uri = inputToString(processTemplate(new ByteArrayInputStream(config.getUrl().getBytes())));

        if (!uri.matches("^\\w+:.*")) {
            return new File(uri);
        }

        FileOutputStream fos = null;
        try {
            File temp = File.createTempFile("dot-", "." + config.getType());
            fos = new FileOutputStream(temp);
            URL url = getUrl(uri);
            copy(url.openStream(), fos);
            temp.deleteOnExit(); // remove temporary resource
            return temp;
        } catch (IOException e) {
            throw new DotException(e);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    logger.warning("Integrity issue with downloaded resource: " + uri);
                }
            }
        }
    }

    private void executeScripts(List<String> scripts) {
        for (String script : scripts) {
            File exec = null;
            FileOutputStream fos = null;
            try {
                exec = File.createTempFile("script", ".sh");
                exec.setExecutable(true);
                fos = new FileOutputStream(exec);
                fos.write(script.getBytes());
                fos.close();

                logger.config("Script: " + script);
                Process process = Runtime.getRuntime().exec(exec.getAbsolutePath());
                process.waitFor();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                copy(process.getInputStream(), baos);
                copy(process.getErrorStream(), baos);
                if (process.exitValue() != 0) {
                    logger.severe(baos.toString());
                    throw new DotException("Exit status " + process.exitValue() + " executing:\n" + script);
                } else {
                    logger.fine(baos.toString());
                }
            } catch (IOException e) {
                throw new DotException(e.getMessage() + " executing:\n" + script);
            } catch (InterruptedException e) {
                throw new DotException(e.getMessage() + " executing:\n" + script);
            } finally {
                if (exec != null) {
                    exec.delete();
                }
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        logger.warning("Error creating shell execution: " + e.getMessage());
                    }
                }
            }
        }
    }

    private URL getUrl(String url) throws MalformedURLException {
        if (!url.matches("^\\w+:.*")) {
            return new URL("file:" + url);
        }
        return new URL(url);
    }

    private void initialize(InputStream in) {
        try {
            Scanner scanner = new Scanner(in).useDelimiter("\\A"); // beginning of input boundary
            if (!scanner.hasNext()) {
                throw new DotException("Configuration contains no data");
            }
            SimpleBindings bindings = new SimpleBindings();
            bindings.putAll(JAVASCRIPT.getBindings(ScriptContext.ENGINE_SCOPE));
            bindings.put("config", scanner.next());

            Map<String, Object> json = (Map<String, Object>) JAVASCRIPT.eval("config = JSON.parse(config)", bindings);
            config = new DotConfiguration();
            config.setType((String) json.get("type"));
            config.setUrl((String) json.get("url"));
            config.setDeploy((String) json.get("deploy"));
            config.setConfig((Map<String, Object>) json.get("config"));
            config.setPreDeploy((List<String>) json.get("preDeploy"));
            config.setPostDeploy((List<String>) json.get("postDeploy"));
            config.setTemplateExtension((String) json.get("templateExtension"));
        } catch (ScriptException e) {
            throw new DotException(e);
        }
    }
}

