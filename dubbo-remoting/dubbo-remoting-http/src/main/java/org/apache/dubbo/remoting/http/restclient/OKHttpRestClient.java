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

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.internal.http.HttpMethod;
import org.apache.dubbo.remoting.http.BaseRestClient;
import org.apache.dubbo.remoting.http.RequestTemplate;
import org.apache.dubbo.remoting.http.config.HttpClientConfig;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class OKHttpRestClient extends BaseRestClient<Response, OkHttpClient> {

    public OKHttpRestClient(HttpClientConfig clientConfig) {
        super(clientConfig);
    }

    @Override
    public Response send(RequestTemplate requestTemplate) throws IOException {

        Request.Builder builder = new Request.Builder();
        // url
        builder.url(requestTemplate.getURL());

        Map<String, Collection<String>> allHeaders = requestTemplate.getAllHeaders();

        // header
        for (String headerName : allHeaders.keySet()) {
            Collection<String> headerValues = allHeaders.get(headerName);

            for (String headerValue : headerValues) {
                builder.addHeader(headerName, headerValue);
            }
        }
        RequestBody requestBody = null;
        if (HttpMethod.permitsRequestBody(requestTemplate.getHttpMethod())) {
            requestBody = RequestBody.create(null, requestTemplate.getSerializedBody());
        }
        builder.method(requestTemplate.getHttpMethod(), requestBody);
        return getClient().newCall(builder.build()).execute();
    }

    @Override
    public void close() {
        getClient().connectionPool().evictAll();
    }

    @Override
    public void close(int timeout) {

    }

    @Override
    public boolean isClosed() {
        return getClient().retryOnConnectionFailure();
    }

    public OkHttpClient createHttpClient(HttpClientConfig httpClientConfig) {
        OkHttpClient client = new OkHttpClient.Builder().
            readTimeout(httpClientConfig.getReadTimeout(), TimeUnit.SECONDS).
            writeTimeout(httpClientConfig.getWriteTimeout(), TimeUnit.SECONDS).
            connectTimeout(httpClientConfig.getConnectTimeout(), TimeUnit.SECONDS).
            build();
        return client;
    }
}
