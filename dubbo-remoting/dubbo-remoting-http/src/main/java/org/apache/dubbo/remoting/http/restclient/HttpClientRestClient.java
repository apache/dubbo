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

import org.apache.dubbo.remoting.http.BaseRestClient;
import org.apache.dubbo.remoting.http.RequestTemplate;
import org.apache.dubbo.remoting.http.config.HttpClientConfig;
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
import java.util.Collection;
import java.util.Map;


public class HttpClientRestClient extends BaseRestClient<CloseableHttpResponse, CloseableHttpClient> {

    public HttpClientRestClient(HttpClientConfig clientConfig) {
        super(clientConfig);
    }

    @Override
    public CloseableHttpResponse send(RequestTemplate requestTemplate) throws IOException {

        HttpRequestBase httpRequest = null;
        String httpMethod = requestTemplate.getHttpMethod();

        if ("GET".equals(httpMethod)) {
            httpRequest = new HttpGet(requestTemplate.getURL());
        } else if ("POST".equals(httpMethod)) {
            HttpPost httpPost = new HttpPost(requestTemplate.getURL());
            httpPost.setEntity(new ByteArrayEntity(requestTemplate.getSerializedBody()));
        }

        Map<String, Collection<String>> allHeaders = requestTemplate.getAllHeaders();

        // header
        for (String headerName : allHeaders.keySet()) {
            Collection<String> headerValues = allHeaders.get(headerName);

            for (String headerValue : headerValues) {
                httpRequest.addHeader(headerName, headerValue);
            }
        }

        httpRequest.setConfig(getRequestConfig(clientConfig));
        return getClient().execute(httpRequest);
    }

    private RequestConfig getRequestConfig(HttpClientConfig clientConfig) {

        // TODO config
        return RequestConfig.custom().build();
    }

    @Override
    public void close() {
        try {
            getClient().close();
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

    public CloseableHttpClient createHttpClient(HttpClientConfig httpClientConfig) {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        return HttpClients.custom().setConnectionManager(connectionManager).build();
    }
}
