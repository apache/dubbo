package org.apache.dubbo.config;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class MetricsConfigTest {

    @Test
    public void testPort() throws Exception{
        MetricsConfig metrics = new MetricsConfig();
        metrics.setPort("8080");
        Map<String, String> parameters = new HashMap<>();
        MetricsConfig.appendParameters(parameters, metrics);
        assertThat(metrics.getPort(), equals("8080"));
        assertThat(parameters.isEmpty(), is(true));
    }

    @Test
    public void testProtocol() throws Exception{
        MetricsConfig metrics = new MetricsConfig();
        metrics.setProtocol("protocol");
        Map<String, String> parameters = new HashMap<>();
        MetricsConfig.appendParameters(parameters, metrics);
        assertThat(metrics.getProtocol(), equals("protocol"));
        assertThat(parameters.isEmpty(), is(true));
    }
}
