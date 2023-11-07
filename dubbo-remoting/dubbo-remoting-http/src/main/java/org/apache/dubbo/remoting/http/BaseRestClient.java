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
package org.apache.dubbo.remoting.http;


import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.ssl.Cert;
import org.apache.dubbo.common.ssl.CertManager;
import org.apache.dubbo.remoting.http.config.HttpClientConfig;

import java.util.concurrent.CompletableFuture;

public abstract class BaseRestClient implements RestClient {
    protected final HttpClientConfig httpClientConfig;
    protected final URL url;


    public BaseRestClient(HttpClientConfig httpClientConfig, URL url) {
        this.httpClientConfig = httpClientConfig;
        this.url = url;
    }


    public URL getUrl() {
        return url;
    }

    public HttpClientConfig getHttpClientConfig() {
        return httpClientConfig;
    }

    /**
     *  ssl config check
     * @return
     */
    public boolean isEnableSSL() {
        Cert consumerConnectionConfig = null;
        try {
            CertManager certManager = url.getOrDefaultFrameworkModel().getBeanFactory().getBean(CertManager.class);
            consumerConnectionConfig = certManager.getConsumerConnectionConfig(url);
        } catch (Exception e) {
            return false;
        }

        if (consumerConnectionConfig == null) {
            return false;
        }
        return true;

    }

    /**
     * set protocol by sslConfig
     *
     * @param requestTemplate
     */
    private void preSend(RequestTemplate requestTemplate) {
        if (isEnableSSL()) {
            requestTemplate.setHttpsProtocol();
        } else {
            requestTemplate.setHttpProtocol();
        }
    }

    @Override
    public CompletableFuture<RestResult> send(RequestTemplate message) {
        preSend(message);
        return doSend(message);
    }

    protected abstract CompletableFuture<RestResult> doSend(RequestTemplate message);
}
