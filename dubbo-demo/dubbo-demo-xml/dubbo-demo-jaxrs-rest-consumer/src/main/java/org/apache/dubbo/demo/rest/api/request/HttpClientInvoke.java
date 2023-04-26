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
package org.apache.dubbo.demo.rest.api.request;

import org.apache.commons.io.IOUtils;
import org.apache.dubbo.remoting.http.RestResult;
import org.apache.dubbo.rpc.protocol.rest.RestHeaderEnum;
import org.apache.dubbo.rpc.protocol.rest.constans.RestConstant;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * demo of use http client to invoke dubbo rest service directly
 * <p>
 * you can use others http client to invoke
 * <p>
 * you must add header #rest#version  & #rest#group  for service to make difference service
 * <p>
 * you can invoke dubbo rest http service by using http client of others language
 */
@Component
public class HttpClientInvoke {


    private final String versionHeader = RestHeaderEnum.VERSION.getHeader();
    private final String groupHeader = RestHeaderEnum.GROUP.getHeader();
    /**
     * contextPath services
     */
    private final String url = "http://localhost:8888/services/http";


    public void httpServiceHttpClientInvoke() throws IOException {
        CloseableHttpClient httpClient = createHttpClient();
        HttpRequestBase httpUriRequest = new HttpGet(url);
        httpUriRequest.addHeader(versionHeader, "1.0.0");
        httpUriRequest.addHeader(RestConstant.ACCEPT, "text/plain");
        httpUriRequest.addHeader(groupHeader, "test");
        httpUriRequest.addHeader("type", "Http Client Invoke Dubbo Rest Service");
        CloseableHttpResponse response = httpClient.execute(httpUriRequest);

        RestResult restResult = parseResponse(response);

        System.out.println(new String(restResult.getBody()));
    }

    private RestResult parseResponse(CloseableHttpResponse response) {
        return new RestResult() {
            @Override
            public String getContentType() {
                return response.getFirstHeader("Content-Type").getValue();
            }

            @Override
            public byte[] getBody() throws IOException {
                if (response.getEntity() == null) {
                    return new byte[0];
                }
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
                return appendErrorMessage(response.getStatusLine().getReasonPhrase(),
                    new String(getErrorResponse()));
            }
        };
    }


    private CloseableHttpClient createHttpClient() {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        return HttpClients.custom().setConnectionManager(connectionManager).build();
    }
}
