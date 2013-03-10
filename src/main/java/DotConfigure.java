import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * @author <a href='mailto:sejal@sejal.org">Sejal Patel</a>
 *         Date: 3/8/13 12:07 AM
 */
public class DotConfigure {
    private static ScriptEngine JAVASCRIPT = new ScriptEngineManager().getEngineByName("JavaScript");

    static { // initialize the javascript engine with the doT.js implementation
        InputStream in = DotConfigure.class.getResourceAsStream("doT.js");
        try {
            JAVASCRIPT.eval(new InputStreamReader(in));
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }

    private DotConfiguration config;

    public DotConfigure(String configUrl) {
        if (configUrl == null) {
            throw new DotException("URL to configuration is required!");
        } else if (!configUrl.matches("^\\w+:.*")) {
            configUrl = "file:" + configUrl;
        }
        try {
            URL url = new URL(configUrl);
            Scanner scanner = new Scanner(url.openStream()).useDelimiter("\\A"); // beginning of input boundary
            if (!scanner.hasNext()) {
                throw new DotException("Config URL: " + configUrl + " contains no data");
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
        } catch (IOException e) {
            throw new DotException(e);
        } catch (ScriptException e) {
            throw new DotException(e);
        }
    }

    public DotConfiguration getConfig() {
        return config;
    }
}

