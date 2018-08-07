package org.apache.dubbo.remoting.http.jetty;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.remoting.http.HttpHandler;
import org.apache.dubbo.remoting.http.HttpServer;
import org.apache.http.client.fluent.Request;
import org.junit.Assert;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class JettyHttpServerTest {
    @Test
    public void shouldAbleCloseForJettyHttpServer() throws Exception {
        int port = NetUtils.getAvailablePort();
        URL url = new URL("http", "localhost", port,
                new String[]{Constants.BIND_PORT_KEY, String.valueOf(port)});
        HttpServer httpServer = new JettyHttpServer(url, new HttpHandler() {
            @Override
            public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
                response.getWriter().write("Jetty");
            }
        });

        String response = Request.Get(url.toJavaURL().toURI()).execute().returnContent().asString();

        assertThat(response, is("Jetty"));

        try {
            httpServer.close();
            Assert.fail();
        } catch (Throwable e) {
            Assert.assertTrue(e instanceof Exception);
        }
    }
}
