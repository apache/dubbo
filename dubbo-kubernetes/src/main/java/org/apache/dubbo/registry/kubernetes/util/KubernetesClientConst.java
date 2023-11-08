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

public class KubernetesClientConst {
    public static final String DEFAULT_MASTER_PLACEHOLDER = "DEFAULT_MASTER_HOST";
    public static final String DEFAULT_MASTER_URL = "https://kubernetes.default.svc";

    public static final String ENABLE_REGISTER = "enableRegister";

    public static final String TRUST_CERTS = "trustCerts";

    public static final String USE_HTTPS = "useHttps";

    public static final String HTTP2_DISABLE = "http2Disable";

    public static final String NAMESPACE = "namespace";

    public static final String API_VERSION = "apiVersion";

    public static final String CA_CERT_FILE = "caCertFile";

    public static final String CA_CERT_DATA = "caCertData";

    public static final String CLIENT_CERT_FILE = "clientCertFile";

    public static final String CLIENT_CERT_DATA = "clientCertData";

    public static final String CLIENT_KEY_FILE = "clientKeyFile";

    public static final String CLIENT_KEY_DATA = "clientKeyData";

    public static final String CLIENT_KEY_ALGO = "clientKeyAlgo";

    public static final String CLIENT_KEY_PASSPHRASE = "clientKeyPassphrase";

    public static final String OAUTH_TOKEN = "oauthToken";

    public static final String USERNAME = "username";

    public static final String PASSWORD = "password";

    public static final String WATCH_RECONNECT_INTERVAL = "watchReconnectInterval";

    public static final String WATCH_RECONNECT_LIMIT = "watchReconnectLimit";

    public static final String CONNECTION_TIMEOUT = "connectionTimeout";

    public static final String REQUEST_TIMEOUT = "requestTimeout";

    public static final String ROLLING_TIMEOUT = "rollingTimeout";

    public static final String LOGGING_INTERVAL = "loggingInterval";

    public static final String HTTP_PROXY = "httpProxy";

    public static final String HTTPS_PROXY = "httpsProxy";

    public static final String PROXY_USERNAME = "proxyUsername";

    public static final String PROXY_PASSWORD = "proxyPassword";

    public static final String NO_PROXY = "noProxy";
}
