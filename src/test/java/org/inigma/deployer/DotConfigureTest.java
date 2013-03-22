package org.inigma.deployer;

import org.junit.Test;

import javax.script.ScriptException;
import java.io.IOException;

import static org.junit.Assert.*;

public class DotConfigureTest {
    @Test
    public void initialize() throws IOException, ScriptException {
        DotConfigure dot = new DotConfigure("src/test/resources/sample-config.json");
        DotConfiguration config = dot.getConfig();
        assertNotNull(config);
        assertEquals("war", config.getType());
        assertEquals("http://localhost/download/test-1.0.war", config.getUrl());
        assertEquals("http://tomcat:tomcat@localhost:8080/manager/deploy?path=/test&update=true", config.getDeploy());
        assertEquals("Flying Purple Monkey", config.getConfig().get("name"));
        assertEquals(0, config.getPreDeploy().size());
        assertEquals(0, config.getPostDeploy().size());
        assertEquals(".dot", config.getTemplateExtension());
    }

    @Test
    public void invokeTarGzConfig() {
        DotConfigure dot = new DotConfigure(DotConfigure.class.getResourceAsStream("/invoke-config-tarball.json"));
        dot.invoke();
    }

    @Test
    public void invokeWarConfig() {
        DotConfigure dot = new DotConfigure(DotConfigure.class.getResourceAsStream("/invoke-config-war.json"));
        dot.invoke();
    }
}
