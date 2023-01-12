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
package org.apache.dubbo.remoting.http.okhttp;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.dubbo.remoting.http.BaseRestClient;
import org.apache.dubbo.remoting.http.config.HttpClientConfig;

import java.io.IOException;
import java.util.concurrent.TimeUnit;


public class OKHttpRestClient extends BaseRestClient<Request, Response, OkHttpClient> {

    public OKHttpRestClient(HttpClientConfig clientConfig) {
        super(clientConfig);
    }

    @Override
    public Response send(Request message) throws IOException {
        return getClient().newCall(message).execute();
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
        getClient().retryOnConnectionFailure();
        return false;
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
