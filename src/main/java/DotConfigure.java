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
import java.util.logging.Logger;

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
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("This application takes only a single argument which is a url to the deployment config.");
            System.exit(1);
        }
        new DotConfigure(args[0]);
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
        if ("war".equalsIgnoreCase(config.getType())) {
            try {
                processedResource = processWar(new JarFile(resource));
            } catch (IOException e) {
                throw new DotException("Resource not a war: " + resource, e);
            }
        } else {
            logger.warning("Terminating, unknown handling of type " + config.getType());
            return;
        }

        executeScripts(config.getPreDeploy());

        deploy(processedResource);

        executeScripts(config.getPostDeploy());
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
            URL url = getUrl(config.getDeploy());
            if ("file".equals(url.getProtocol())) {
                FileOutputStream fos = new FileOutputStream(url.getPath());
                FileInputStream fis = new FileInputStream(resource);
                copy(fis, fos);
                fis.close();
                fos.close();
            } else if ("http".equals(url.getProtocol()) || "https".equals(url.getProtocol())) {

            } else if ("ftp".equals(url.getProtocol())) {

            } else if ("scp".equals(url.getProtocol())) {

            }
        } catch (IOException e) {
            throw new DotException(e);
        }
    }

    private File downloadResource() {
        if (!config.getUrl().matches("^\\w+:.*")) {
            return new File(config.getUrl());
        }

        FileOutputStream fos = null;
        try {
            File temp = File.createTempFile("", "." + config.getType());
            fos = new FileOutputStream(temp);
            URL url = getUrl(config.getUrl());
            copy(url.openStream(), fos);
            return temp;
        } catch (IOException e) {
            throw new DotException(e);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    logger.warning("Integrity issue with downloaded resource: " + config.getUrl());
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

                Process process = Runtime.getRuntime().exec(exec.getAbsolutePath());
                process.waitFor();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                copy(process.getInputStream(), baos);
                copy(process.getErrorStream(), baos);
                if (process.exitValue() != 0) {
                    logger.severe(baos.toString());
                    throw new DotException("Exit status " + process.exitValue() + " executing:\n" + script);
                } else {
                    logger.info(baos.toString());
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

    private InputStream processTemplate(InputStream in) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        copy(in, baos);
        SimpleBindings bindings = new SimpleBindings();
        bindings.putAll(JAVASCRIPT.getBindings(ScriptContext.ENGINE_SCOPE));
        bindings.put("template", baos.toString());
        bindings.put("config", config.getConfig());
        try {
            String result = (String) JAVASCRIPT.eval("doT.template(template)(config)", bindings);
            return new ByteArrayInputStream(result.getBytes());
        } catch (ScriptException e) {
            throw new DotException("Unable to evaluate template", e);
        }
    }

    private File processWar(JarFile jarFile) {
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
}

