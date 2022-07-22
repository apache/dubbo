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

    public final static String ENABLE_REGISTER = "enableRegister";

    public final static String TRUST_CERTS = "trustCerts";

    public final static String USE_HTTPS = "useHttps";

    public static final String HTTP2_DISABLE = "http2Disable";

    public final static String NAMESPACE = "namespace";

    public final static String API_VERSION = "apiVersion";

    public final static String CA_CERT_FILE = "caCertFile";

    public final static String CA_CERT_DATA = "caCertData";

    public final static String CLIENT_CERT_FILE = "clientCertFile";

    public final static String CLIENT_CERT_DATA = "clientCertData";

    public final static String CLIENT_KEY_FILE = "clientKeyFile";

    public final static String CLIENT_KEY_DATA = "clientKeyData";

    public final static String CLIENT_KEY_ALGO = "clientKeyAlgo";

    public final static String CLIENT_KEY_PASSPHRASE = "clientKeyPassphrase";

    public final static String OAUTH_TOKEN = "oauthToken";

    public final static String USERNAME = "username";

    public final static String PASSWORD = "password";

    public final static String WATCH_RECONNECT_INTERVAL = "watchReconnectInterval";

    public final static String WATCH_RECONNECT_LIMIT = "watchReconnectLimit";

    public final static String CONNECTION_TIMEOUT = "connectionTimeout";

    public final static String REQUEST_TIMEOUT = "requestTimeout";

    public final static String ROLLING_TIMEOUT = "rollingTimeout";

    public final static String LOGGING_INTERVAL = "loggingInterval";

    public final static String HTTP_PROXY = "httpProxy";

    public final static String HTTPS_PROXY = "httpsProxy";

    public final static String PROXY_USERNAME = "proxyUsername";

    public final static String PROXY_PASSWORD = "proxyPassword";

    public final static String NO_PROXY = "noProxy";
}
