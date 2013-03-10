import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href='mailto:sejal@sejal.org">Sejal Patel</a>
 *         Date: 3/9/13 9:40 PM
 */
public class DotConfiguration {
    private String type;
    private String url;
    private Map<String, Object> config = new LinkedHashMap<String, Object>();
    private String deploy;
    private List<String> preDeploy = new ArrayList<String>();
    private List<String> postDeploy = new ArrayList<String>();
    private String templateExtension = ".dot";

    public Map<String, Object> getConfig() {
        return config;
    }

    public String getDeploy() {
        return deploy;
    }

    public List<String> getPostDeploy() {
        return postDeploy;
    }

    public List<String> getPreDeploy() {
        return preDeploy;
    }

    public String getTemplateExtension() {
        return templateExtension;
    }

    public String getType() {
        return type;
    }

    public String getUrl() {
        return url;
    }

    public void setConfig(Map<String, Object> config) {
        if (config != null) {
            this.config = config;
        }
    }

    public void setDeploy(String deploy) {
        this.deploy = deploy;
    }

    public void setPostDeploy(List<String> postDeploy) {
        if (postDeploy != null) {
            this.postDeploy = postDeploy;
        }
    }

    public void setPreDeploy(List<String> preDeploy) {
        if (preDeploy != null) {
            this.preDeploy = preDeploy;
        }
    }

    public void setTemplateExtension(String templateExtension) {
        if (templateExtension != null) {
            this.templateExtension = templateExtension;
        }
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
