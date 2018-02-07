package com.alibaba.dubbo.remoting.http.jetty;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.http.HttpHandler;
import com.alibaba.dubbo.remoting.http.HttpServer;
import com.alibaba.dubbo.remoting.http.TestUtil;
import org.apache.http.client.fluent.Request;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class JettyHttpBinderTest {
    @Test
    public void shouldAbleHandleRequestForJettyBinder() throws Exception {
        int port = TestUtil.getFreePort();
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

        httpServer.close();
    }
}