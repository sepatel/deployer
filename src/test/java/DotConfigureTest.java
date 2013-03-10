import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.script.ScriptException;
import java.io.IOException;

import static org.junit.Assert.*;

public class DotConfigureTest {
    @Test
    public void initialize() throws IOException, ScriptException {
        DotConfigure dot = new DotConfigure("file:src/test/resources/sample-config.json");
        DotConfiguration config = dot.getConfig();
        assertNotNull(config);
        assertEquals("war", config.getType());
        assertEquals("target/test-1.0.war", config.getUrl());
        assertEquals("http://tomcat:tomcat@localhost:8080/manager/deploy?path=/test&update=true", config.getDeploy());
        assertEquals("Flying Purple Monkey", config.getConfig().get("name"));
        assertEquals(0, config.getPreDeploy().size());
        assertEquals(0, config.getPostDeploy().size());
        assertEquals(".dot", config.getTemplateExtension());
    }
}
