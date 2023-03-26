/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.remoting.http.rest;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.url.component.ServiceConfigURL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.http.*;
import org.apache.dubbo.remoting.http.config.HttpClientConfig;
import org.apache.dubbo.remoting.http.jetty.JettyHttpServer;
import org.apache.dubbo.remoting.http.restclient.HttpClientRestClient;
import org.apache.dubbo.remoting.http.restclient.OKHttpRestClient;
import org.apache.dubbo.remoting.http.restclient.URLConnectionRestClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class RestClientTest {

    @Test
    public void testRestClient() throws Exception {
        int port = NetUtils.getAvailablePort();
        URL url = new ServiceConfigURL("http", "localhost", port,
            new String[]{Constants.BIND_PORT_KEY, String.valueOf(port)});
        HttpServer httpServer = new JettyHttpServer(url, new HttpHandler<HttpServletRequest, HttpServletResponse>() {
            @Override
            public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
                response.getWriter().write("Jetty");
            }
        });

        RequestTemplate requestTemplate = new RequestTemplate(null, "POST", "localhost:" + port);

        requestTemplate.addParam("p1", "value1");
        requestTemplate.addParam("p2", "value2");

        requestTemplate.addParams("p3", Arrays.asList("value3", "value3.1"));
        requestTemplate.addHeader("test", "dubbo");
        requestTemplate.addKeepAliveHeader(60);

        requestTemplate.addHeaders("header", Arrays.asList("h1", "h2"));

        requestTemplate.path("/test");
        requestTemplate.serializeBody("test".getBytes(StandardCharsets.UTF_8));

        RestClient restClient = new OKHttpRestClient(new HttpClientConfig());

        CompletableFuture<RestResult> send = restClient.send(requestTemplate);

        RestResult restResult = send.get();


        assertThat(new String(restResult.getBody()), is("Jetty"));


        restClient = new HttpClientRestClient(new HttpClientConfig());

        send = restClient.send(requestTemplate);

        restResult = send.get();

        assertThat(new String(restResult.getBody()), is("Jetty"));

        restClient = new URLConnectionRestClient(new HttpClientConfig());

        send = restClient.send(requestTemplate);

        restResult = send.get();

        assertThat(new String(restResult.getBody()), is("Jetty"));

        httpServer.close();
    }


    @Test
    public void testError() throws Exception {
        int port = NetUtils.getAvailablePort();
        URL url = new ServiceConfigURL("http", "localhost", port,
            new String[]{Constants.BIND_PORT_KEY, String.valueOf(port)});
        HttpServer httpServer = new JettyHttpServer(url, new HttpHandler<HttpServletRequest, HttpServletResponse>() {
            @Override
            public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
                response.setStatus(500);
                response.getWriter().write("server error");
                response.addHeader("Content-Type", "text/html");
            }
        });

        RequestTemplate requestTemplate = new RequestTemplate(null, null, null);

        requestTemplate.httpMethod("POST");
        requestTemplate.setAddress("localhost:" + port);
        requestTemplate.setProtocol("http://");
        requestTemplate.addHeader("test", "dubbo");
        requestTemplate.path("/test");
        requestTemplate.serializeBody("test".getBytes(StandardCharsets.UTF_8));

        RestClient restClient = new OKHttpRestClient(new HttpClientConfig());

        CompletableFuture<RestResult> send = restClient.send(requestTemplate);

        String error = "Server Error\n" +
            " error info is: server error";
        RestResult restResult = send.get();

        String contentType = "text/html;charset=iso-8859-1";

        Assertions.assertEquals(500, restResult.getResponseCode());
        Assertions.assertEquals(error, restResult.getMessage());
        Assertions.assertEquals(contentType, restResult.getContentType());

        Map<String, List<String>> headers = restResult.headers();
        restClient.close();

        restClient = new HttpClientRestClient(new HttpClientConfig());
        send = restClient.send(requestTemplate);
        restResult = send.get();


        Assertions.assertEquals(500, restResult.getResponseCode());
        Assertions.assertEquals(error, restResult.getMessage());
        Assertions.assertEquals(contentType, restResult.getContentType());

        restClient.close();


        restClient = new URLConnectionRestClient(new HttpClientConfig());
        send = restClient.send(requestTemplate);
        restResult = send.get();

        Assertions.assertEquals(500, restResult.getResponseCode());
        Assertions.assertEquals(error, restResult.getMessage());
        Assertions.assertEquals(contentType, restResult.getContentType());
        restClient.close();


        httpServer.close();
    }

    @Test
    public void testMethod() {

        RequestTemplate requestTemplate = new RequestTemplate(null, null, null);

        requestTemplate.body(new Object(), Object.class);

        Assertions.assertEquals(requestTemplate.getBodyType(),Object.class);


        requestTemplate.addHeader("Content-Length",1);

        Integer contentLength = requestTemplate.getContentLength();

        Assertions.assertEquals(1,contentLength);

        List<String> strings = Arrays.asList("h1", "h2");

        requestTemplate.addHeaders("header",strings);


        Assertions.assertArrayEquals(strings.toArray(new String[0]),requestTemplate.getHeaders("header").toArray(new String[0]));

         strings = Arrays.asList("p1", "p2");

        requestTemplate.addParams("param",strings);

        Assertions.assertArrayEquals(strings.toArray(new String[0]),requestTemplate.getParam("param").toArray(new String[0]));



    }
}
