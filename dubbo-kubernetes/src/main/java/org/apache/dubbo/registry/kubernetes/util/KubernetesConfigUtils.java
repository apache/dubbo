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
package org.apache.dubbo.registry.kubernetes.util;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.StringUtils;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;

import java.util.Base64;

import static org.apache.dubbo.registry.kubernetes.util.KubernetesClientConst.API_VERSION;
import static org.apache.dubbo.registry.kubernetes.util.KubernetesClientConst.CA_CERT_DATA;
import static org.apache.dubbo.registry.kubernetes.util.KubernetesClientConst.CA_CERT_FILE;
import static org.apache.dubbo.registry.kubernetes.util.KubernetesClientConst.CLIENT_CERT_DATA;
import static org.apache.dubbo.registry.kubernetes.util.KubernetesClientConst.CLIENT_CERT_FILE;
import static org.apache.dubbo.registry.kubernetes.util.KubernetesClientConst.CLIENT_KEY_ALGO;
import static org.apache.dubbo.registry.kubernetes.util.KubernetesClientConst.CLIENT_KEY_DATA;
import static org.apache.dubbo.registry.kubernetes.util.KubernetesClientConst.CLIENT_KEY_FILE;
import static org.apache.dubbo.registry.kubernetes.util.KubernetesClientConst.CLIENT_KEY_PASSPHRASE;
import static org.apache.dubbo.registry.kubernetes.util.KubernetesClientConst.CONNECTION_TIMEOUT;
import static org.apache.dubbo.registry.kubernetes.util.KubernetesClientConst.DEFAULT_MASTER_PLACEHOLDER;
import static org.apache.dubbo.registry.kubernetes.util.KubernetesClientConst.DEFAULT_MASTER_URL;
import static org.apache.dubbo.registry.kubernetes.util.KubernetesClientConst.HTTP2_DISABLE;
import static org.apache.dubbo.registry.kubernetes.util.KubernetesClientConst.HTTPS_PROXY;
import static org.apache.dubbo.registry.kubernetes.util.KubernetesClientConst.HTTP_PROXY;
import static org.apache.dubbo.registry.kubernetes.util.KubernetesClientConst.LOGGING_INTERVAL;
import static org.apache.dubbo.registry.kubernetes.util.KubernetesClientConst.NAMESPACE;
import static org.apache.dubbo.registry.kubernetes.util.KubernetesClientConst.NO_PROXY;
import static org.apache.dubbo.registry.kubernetes.util.KubernetesClientConst.OAUTH_TOKEN;
import static org.apache.dubbo.registry.kubernetes.util.KubernetesClientConst.PASSWORD;
import static org.apache.dubbo.registry.kubernetes.util.KubernetesClientConst.PROXY_PASSWORD;
import static org.apache.dubbo.registry.kubernetes.util.KubernetesClientConst.PROXY_USERNAME;
import static org.apache.dubbo.registry.kubernetes.util.KubernetesClientConst.REQUEST_TIMEOUT;
import static org.apache.dubbo.registry.kubernetes.util.KubernetesClientConst.ROLLING_TIMEOUT;
import static org.apache.dubbo.registry.kubernetes.util.KubernetesClientConst.TRUST_CERTS;
import static org.apache.dubbo.registry.kubernetes.util.KubernetesClientConst.USERNAME;
import static org.apache.dubbo.registry.kubernetes.util.KubernetesClientConst.USE_HTTPS;
import static org.apache.dubbo.registry.kubernetes.util.KubernetesClientConst.WATCH_RECONNECT_INTERVAL;
import static org.apache.dubbo.registry.kubernetes.util.KubernetesClientConst.WATCH_RECONNECT_LIMIT;

public class KubernetesConfigUtils {

    public static Config createKubernetesConfig(URL url) {
        // Init default config
        Config base = Config.autoConfigure(null);

        // replace config with parameters if presents
        return new ConfigBuilder(base) //
            .withMasterUrl(buildMasterUrl(url)) //
            .withApiVersion(url.getParameter(API_VERSION, base.getApiVersion())) //
            .withNamespace(url.getParameter(NAMESPACE, base.getNamespace())) //
            .withUsername(url.getParameter(USERNAME, base.getUsername())) //
            .withPassword(url.getParameter(PASSWORD, base.getPassword())) //

            .withOauthToken(url.getParameter(OAUTH_TOKEN, base.getOauthToken())) //

            .withCaCertFile(url.getParameter(CA_CERT_FILE, base.getCaCertFile())) //
            .withCaCertData(url.getParameter(CA_CERT_DATA, decodeBase64(base.getCaCertData()))) //

            .withClientKeyFile(url.getParameter(CLIENT_KEY_FILE, base.getClientKeyFile())) //
            .withClientKeyData(url.getParameter(CLIENT_KEY_DATA, decodeBase64(base.getClientKeyData()))) //

            .withClientCertFile(url.getParameter(CLIENT_CERT_FILE, base.getClientCertFile())) //
            .withClientCertData(url.getParameter(CLIENT_CERT_DATA, decodeBase64(base.getClientCertData()))) //

            .withClientKeyAlgo(url.getParameter(CLIENT_KEY_ALGO, base.getClientKeyAlgo())) //
            .withClientKeyPassphrase(url.getParameter(CLIENT_KEY_PASSPHRASE, base.getClientKeyPassphrase())) //

            .withConnectionTimeout(url.getParameter(CONNECTION_TIMEOUT, base.getConnectionTimeout())) //
            .withRequestTimeout(url.getParameter(REQUEST_TIMEOUT, base.getRequestTimeout())) //
            .withRollingTimeout(url.getParameter(ROLLING_TIMEOUT, base.getRollingTimeout())) //

            .withWatchReconnectInterval(url.getParameter(WATCH_RECONNECT_INTERVAL, base.getWatchReconnectInterval())) //
            .withWatchReconnectLimit(url.getParameter(WATCH_RECONNECT_LIMIT, base.getWatchReconnectLimit())) //
            .withLoggingInterval(url.getParameter(LOGGING_INTERVAL, base.getLoggingInterval())) //

            .withTrustCerts(url.getParameter(TRUST_CERTS, base.isTrustCerts())) //
            .withHttp2Disable(url.getParameter(HTTP2_DISABLE, base.isTrustCerts())) //

            .withHttpProxy(url.getParameter(HTTP_PROXY, base.getHttpProxy())) //
            .withHttpsProxy(url.getParameter(HTTPS_PROXY, base.getHttpsProxy())) //
            .withProxyUsername(url.getParameter(PROXY_USERNAME, base.getProxyUsername())) //
            .withProxyPassword(url.getParameter(PROXY_PASSWORD, base.getProxyPassword())) //
            .withNoProxy(url.getParameter(NO_PROXY, base.getNoProxy())) //
            .build();
    }

    private static String buildMasterUrl(URL url) {
        if (DEFAULT_MASTER_PLACEHOLDER.equalsIgnoreCase(url.getHost())) {
            return DEFAULT_MASTER_URL;
        }
        return (url.getParameter(USE_HTTPS, true) ?
            "https://" : "http://")
            + url.getHost() + ":" + url.getPort();
    }

    private static String decodeBase64(String str) {
        return StringUtils.isNotEmpty(str) ?
            new String(Base64.getDecoder().decode(str)) :
            null;
    }
}
