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
package org.apache.dubbo.remoting.http.restclient;

import org.apache.dubbo.remoting.http.RequestTemplate;
import org.apache.dubbo.remoting.http.RestClient;
import org.apache.dubbo.remoting.http.RestResult;
import org.apache.dubbo.remoting.http.config.HttpClientConfig;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;


public class HttpClientRestClient implements RestClient {
    private final CloseableHttpClient closeableHttpClient;
    private final HttpClientConfig httpClientConfig;

    public HttpClientRestClient(HttpClientConfig clientConfig) {
        closeableHttpClient = createHttpClient();
        httpClientConfig = clientConfig;
    }

    @Override
    public CompletableFuture<RestResult> send(RequestTemplate requestTemplate) {

        HttpRequestBase httpRequest = null;
        String httpMethod = requestTemplate.getHttpMethod();

        if ("GET".equals(httpMethod)) {
            httpRequest = new HttpGet(requestTemplate.getURL());
        } else if ("POST".equals(httpMethod)) {
            HttpPost httpPost = new HttpPost(requestTemplate.getURL());
            httpPost.setEntity(new ByteArrayEntity(requestTemplate.getSerializedBody()));
            httpRequest = httpPost;
        }

        Map<String, Collection<String>> allHeaders = requestTemplate.getAllHeaders();

        allHeaders.remove("Content-Length");
        // header
        for (String headerName : allHeaders.keySet()) {
            Collection<String> headerValues = allHeaders.get(headerName);

            for (String headerValue : headerValues) {
                httpRequest.addHeader(headerName, headerValue);
            }
        }

        httpRequest.setConfig(getRequestConfig(httpClientConfig));

        CompletableFuture<RestResult> future = new CompletableFuture<>();
        try {
            CloseableHttpResponse response = closeableHttpClient.execute(httpRequest);
            future.complete(new RestResult() {
                @Override
                public String getContentType() {
                    return response.getFirstHeader("Content-Type").getValue();
                }

                @Override
                public byte[] getBody() throws IOException {
                    return IOUtils.toByteArray(response.getEntity().getContent());
                }

                @Override
                public Map<String, List<String>> headers() {
                    return Arrays.stream(response.getAllHeaders()).collect(Collectors.toMap(Header::getName, h -> Collections.singletonList(h.getValue())));
                }

                @Override
                public byte[] getErrorResponse() throws IOException {
                    return getBody();
                }

                @Override
                public int getResponseCode() {
                    return response.getStatusLine().getStatusCode();
                }

                @Override
                public String getMessage() throws IOException {
                    return response.getStatusLine().getReasonPhrase();
                }
            });
        } catch (IOException e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    private RequestConfig getRequestConfig(HttpClientConfig clientConfig) {

        // TODO config
        return RequestConfig.custom().build();
    }

    @Override
    public void close() {
        try {
            closeableHttpClient.close();
        } catch (IOException e) {

        }
    }

    @Override
    public void close(int timeout) {

    }

    @Override
    public boolean isClosed() {
        // TODO close judge
        return true;
    }

    public CloseableHttpClient createHttpClient() {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        return HttpClients.custom().setConnectionManager(connectionManager).build();
    }
}
