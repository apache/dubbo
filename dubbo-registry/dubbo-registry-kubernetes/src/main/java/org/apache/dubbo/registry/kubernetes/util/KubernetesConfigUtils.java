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

public class KubernetesConfigUtils {

    public static Config createKubernetesConfig(URL url) {
        // Init default config
        Config base = Config.autoConfigure(null);

        // replace config with parameters if presents
        return new ConfigBuilder(base)
                .withMasterUrl(buildMasterUrl(url))
                .withApiVersion(url.getParameter(KubernetesClientConst.API_VERSION,
                        base.getApiVersion()))
                .withNamespace(url.getParameter(KubernetesClientConst.NAMESPACE,
                        base.getNamespace()))
                .withUsername(url.getParameter(KubernetesClientConst.USERNAME,
                        base.getUsername()))
                .withPassword(url.getParameter(KubernetesClientConst.PASSWORD,
                        base.getPassword()))

                .withOauthToken(url.getParameter(KubernetesClientConst.OAUTH_TOKEN,
                        base.getOauthToken()))

                .withCaCertFile(url.getParameter(KubernetesClientConst.CA_CERT_FILE,
                        base.getCaCertFile()))
                .withCaCertData(url.getParameter(KubernetesClientConst.CA_CERT_DATA,
                        decodeBase64(base.getCaCertData())))

                .withClientKeyFile(url.getParameter(KubernetesClientConst.CLIENT_KEY_FILE,
                        base.getClientKeyFile()))
                .withClientKeyData(url.getParameter(KubernetesClientConst.CLIENT_KEY_DATA,
                        decodeBase64(base.getClientKeyData())))

                .withClientCertFile(url.getParameter(KubernetesClientConst.CLIENT_CERT_FILE,
                        base.getClientCertFile()))
                .withClientCertData(url.getParameter(KubernetesClientConst.CLIENT_CERT_DATA,
                        decodeBase64(base.getClientCertData())))

                .withClientKeyAlgo(url.getParameter(KubernetesClientConst.CLIENT_KEY_ALGO,
                        base.getClientKeyAlgo()))
                .withClientKeyPassphrase(url.getParameter(KubernetesClientConst.CLIENT_KEY_PASSPHRASE,
                        base.getClientKeyPassphrase()))

                .withConnectionTimeout(url.getParameter(KubernetesClientConst.CONNECTION_TIMEOUT,
                        base.getConnectionTimeout()))
                .withRequestTimeout(url.getParameter(KubernetesClientConst.REQUEST_TIMEOUT,
                        base.getRequestTimeout()))
                .withRollingTimeout(url.getParameter(KubernetesClientConst.ROLLING_TIMEOUT,
                        base.getRollingTimeout()))

                .withWatchReconnectInterval(url.getParameter(KubernetesClientConst.WATCH_RECONNECT_INTERVAL,
                        base.getWatchReconnectInterval()))
                .withWatchReconnectLimit(url.getParameter(KubernetesClientConst.WATCH_RECONNECT_LIMIT,
                        base.getWatchReconnectLimit()))
                .withLoggingInterval(url.getParameter(KubernetesClientConst.LOGGING_INTERVAL,
                        base.getLoggingInterval()))

                .withTrustCerts(url.getParameter(KubernetesClientConst.TRUST_CERTS,
                        base.isTrustCerts()))
                .withHttp2Disable(url.getParameter(KubernetesClientConst.HTTP2_DISABLE,
                        base.isTrustCerts()))

                .withHttpProxy(url.getParameter(KubernetesClientConst.HTTP_PROXY,
                        base.getHttpProxy()))
                .withHttpsProxy(url.getParameter(KubernetesClientConst.HTTPS_PROXY,
                        base.getHttpsProxy()))
                .withProxyUsername(url.getParameter(KubernetesClientConst.PROXY_USERNAME,
                        base.getProxyUsername()))
                .withProxyPassword(url.getParameter(KubernetesClientConst.PROXY_PASSWORD,
                        base.getProxyPassword()))
                .withNoProxy(url.getParameter(KubernetesClientConst.NO_PROXY,
                        base.getNoProxy()))
                .build();
    }

    private static String buildMasterUrl(URL url) {
        return (url.getParameter(KubernetesClientConst.USE_HTTPS, true) ?
                "https://" : "http://")
                + url.getHost() + ":" + url.getPort();
    }

    private static String decodeBase64(String str) {
        return StringUtils.isNotEmpty(str) ?
                new String(Base64.getDecoder().decode(str)) :
                "";
    }
}
