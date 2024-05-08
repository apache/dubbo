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

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.http.HttpMethod;

// TODO add version 4.0 implements ,and default version is < 4.0,for dependency conflict
public class OKHttpRestClient implements RestClient {
    private final OkHttpClient okHttpClient;
    private final HttpClientConfig httpClientConfig;

    public OKHttpRestClient(HttpClientConfig clientConfig) {
        this.okHttpClient = createHttpClient(clientConfig);
        this.httpClientConfig = clientConfig;
    }

    @Override
    public CompletableFuture<RestResult> send(RequestTemplate requestTemplate) {

        Request.Builder builder = new Request.Builder();
        // url
        builder.url(requestTemplate.getURL());

        Map<String, Collection<String>> allHeaders = requestTemplate.getAllHeaders();

        boolean hasBody = false;
        RequestBody requestBody = null;
        // GET & HEAD body is forbidden
        if (HttpMethod.permitsRequestBody(requestTemplate.getHttpMethod())) {
            requestBody = RequestBody.create(null, requestTemplate.getSerializedBody());
            hasBody = true;
        }

        // header
        for (String headerName : allHeaders.keySet()) {
            Collection<String> headerValues = allHeaders.get(headerName);
            if (!hasBody && "Content-Length".equals(headerName)) {
                continue;
            }
            for (String headerValue : headerValues) {

                builder.addHeader(headerName, headerValue);
            }
        }

        builder.method(requestTemplate.getHttpMethod(), requestBody);

        CompletableFuture<RestResult> future = new CompletableFuture<>();

        okHttpClient.newCall(builder.build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                future.complete(new RestResult() {
                    @Override
                    public String getContentType() {
                        return response.header("Content-Type");
                    }

                    @Override
                    public byte[] getBody() throws IOException {
                        ResponseBody body = response.body();
                        return body == null ? null : body.bytes();
                    }

                    @Override
                    public Map<String, List<String>> headers() {
                        return response.headers().toMultimap();
                    }

                    @Override
                    public byte[] getErrorResponse() throws IOException {
                        return getBody();
                    }

                    @Override
                    public int getResponseCode() throws IOException {
                        return response.code();
                    }

                    @Override
                    public String getMessage() throws IOException {
                        return appendErrorMessage(response.message(), new String(getBody()));
                    }
                });
            }
        });

        return future;
    }

    @Override
    public void close() {
        okHttpClient.connectionPool().evictAll();
    }

    @Override
    public void close(int timeout) {}

    @Override
    public boolean isClosed() {
        return okHttpClient.retryOnConnectionFailure();
    }

    public OkHttpClient createHttpClient(HttpClientConfig httpClientConfig) {

        return new OkHttpClient.Builder()
                .readTimeout(httpClientConfig.getReadTimeout(), TimeUnit.SECONDS)
                .writeTimeout(httpClientConfig.getWriteTimeout(), TimeUnit.SECONDS)
                .connectTimeout(httpClientConfig.getConnectTimeout(), TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(
                        httpClientConfig.getMaxIdleConnections(),
                        httpClientConfig.getKeepAliveDuration(),
                        TimeUnit.SECONDS))
                .build();
    }
}
