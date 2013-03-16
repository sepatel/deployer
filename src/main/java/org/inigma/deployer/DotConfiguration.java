package org.inigma.deployer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <pre>{
 * type: 'war', // later things like zip or .tar.gz I suppose. possibly even a 'dir' meaning directory
 * url: 'target/test-1.0.war',
 * deploy: 'http://tomcat:tomcat@localhost:8080/manager/deploy?path=/test&update=true',
 * config: { // war specific templating variables, using doT v1.0 passing in config as the it
 * preDeploy: [], // default to nothing if not defined
 * postDeploy: [], // default to nothing if not defined
 * templateExtension: '.dot', // defaults to .dot so this does not need to be declared but can be overwritten if needed.
 * }</pre>
 *
 * @author <a href='mailto:sejal@sejal.org">Sejal Patel</a>
 *         Date: 3/9/13 9:40 PM
 */
public class DotConfiguration {
    private String type;
    private String url;
    private String deploy;
    private Map<String, Object> config = new LinkedHashMap<String, Object>();
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
