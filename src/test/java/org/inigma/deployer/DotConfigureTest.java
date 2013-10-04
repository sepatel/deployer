package org.inigma.deployer;

import org.junit.Test;

import javax.script.ScriptException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

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

        dot = new DotConfigure("src/test/resources/sample-config.yml");
        DotConfiguration yamlConfig = dot.getConfig();
        assertNotNull(yamlConfig);
        assertEquals(config.getConfig().entrySet(), yamlConfig.getConfig().entrySet());
        assertEquals(config.getDeploy(), yamlConfig.getDeploy());
        assertEquals(config.getPostDeploy(), yamlConfig.getPostDeploy());
        assertEquals(config.getPreDeploy(), yamlConfig.getPreDeploy());
        assertEquals(config.getType(), yamlConfig.getType());
        assertEquals(config.getUrl(), yamlConfig.getUrl());
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

    @Test
    public void invokeWarWithVariables() {
        DotConfigure dot = new DotConfigure("src/test/resources/env-config.json");
        dot.invoke();
    }

    @Test
    public void templateSupportsEnvironmentVariables() throws IOException {
        String answer = "Hello Flying Purple Monkey, " + System.getenv("USER");
        ByteArrayInputStream bais = new ByteArrayInputStream("Hello {{=it.name}}, {{=env.USER}}".getBytes());
        DotConfigure dot = new DotConfigure("src/test/resources/env-config.json");
        InputStream inputStream = dot.processTemplate(bais);
        byte[] buffer = new byte[4096];
        int read = inputStream.read(buffer);
        assertTrue(read > 0);
        String result = new String(buffer, 0, read);
        assertEquals(answer, result);
    }

    @Test
    public void templateSupportsSystemProperties() throws IOException {
        String answer = "Hello Flying Purple Monkey, " + System.getProperty("java.vm.vendor");
        ByteArrayInputStream bais = new ByteArrayInputStream(("Hello {{=it.name}}, {{=prop['java.vm.vendor']}}").getBytes());
        DotConfigure dot = new DotConfigure("src/test/resources/env-config.json");
        InputStream inputStream = dot.processTemplate(bais);
        byte[] buffer = new byte[4096];
        int read = inputStream.read(buffer);
        assertTrue(read > 0);
        String result = new String(buffer, 0, read);
        assertEquals(answer, result);
    }
}
