package org.apache.dubbo.config;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class MetricsConfigTest {

    @Test
    public void testPort() throws Exception{
        MetricsConfig metrics = new MetricsConfig();
        metrics.setPort("8080");
        assertThat(metrics.getPort(), equalTo("8080"));
    }

    @Test
    public void testProtocol() throws Exception{
        MetricsConfig metrics = new MetricsConfig();
        metrics.setProtocol("protocol");
        assertThat(metrics.getProtocol(), equalTo("protocol"));
    }
}
