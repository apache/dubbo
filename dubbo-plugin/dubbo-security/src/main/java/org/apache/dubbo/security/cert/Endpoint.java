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

package org.apache.dubbo.security.cert;

import org.apache.dubbo.common.utils.JsonUtils;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

public class Endpoint {
    private static final String SPIFFE_SCHEME = "spiffe://";
    private static final String NAMESPACE_SEGMENT = "ns";
    private static final String SERVICE_ACCOUNT_SEGMENT = "sa";
    private final String trustDomain;
    private final String namespace;
    private final String serviceAccount;
    private final String extensions;
    private final InetSocketAddress socketAddress;

    public Endpoint(String spiffeUrl, String extensions, InetSocketAddress socketAddress) {
        this.extensions = extensions;
        this.socketAddress = socketAddress;

        String[] tags = null;

        if (spiffeUrl != null && spiffeUrl.startsWith(SPIFFE_SCHEME)) {
            spiffeUrl = spiffeUrl.substring(SPIFFE_SCHEME.length());
            tags = spiffeUrl.split("/");
        }

        if (tags == null || tags.length != 5) {
            trustDomain = null;
            namespace = null;
            serviceAccount = null;
            return;
        }

        if (!NAMESPACE_SEGMENT.equals(tags[1]) || !SERVICE_ACCOUNT_SEGMENT.equals(tags[3])) {
            trustDomain = null;
            namespace = null;
            serviceAccount = null;
            return;
        }

        trustDomain = tags[0];
        namespace = tags[2];
        serviceAccount = tags[4];
    }

    public static Endpoint decodeFromCertificates(String commonName, String token, List<String> trustedKeys, InetSocketAddress socketAddress) {
        return JwtUtils.decodeEndpointFromJwt(token, commonName, trustedKeys, socketAddress);
    }

    public String getPrincipal() {
        return trustDomain + "/" + NAMESPACE_SEGMENT + "/" + namespace + "/" + SERVICE_ACCOUNT_SEGMENT + "/" + serviceAccount;
    }

    public String getNamespace() {
        if (namespace != null) {
            return namespace;
        }
        if (extensions != null) {
            Map<?, ?> map = JsonUtils.toJavaObject(extensions, Map.class);
            if (map != null && map.containsKey("namespace")) {
                Object ns = map.get("namespace");
                if (ns instanceof String) {
                    return (String) ns;
                }
            }
        }
        return null;
    }

    public String getIp() {
        if (socketAddress != null) {
            return socketAddress.getAddress().getHostAddress();
        }
        if (extensions != null) {
            Map<?, ?> map = JsonUtils.toJavaObject(extensions, Map.class);
            if (map != null && map.containsKey("ips")) {
                Object ips = map.get("ips");
                if (ips instanceof List && !((List<?>) ips).isEmpty()) {
                    Object ip = ((List<?>) ips).get(0);
                    if (ip instanceof String) {
                        return (String) ip;
                    }
                }
            }
        }
        return null;
    }

    public String getParameter(String key) {
        // TODO support json path
        if (extensions != null) {
            Map<?, ?> map = JsonUtils.toJavaObject(extensions, Map.class);
            return getParameter(map, key);
        }
        return null;
    }

    private String getParameter(Map<?, ?> map, String key) {
        String[] subKeys = key.split("\\.", 2);
        String current = key;
        String next = null;
        if (subKeys.length == 2) {
            current = subKeys[0];
            next = subKeys[1];
        }
        if (map != null && map.containsKey(current)) {
            Object value = map.get(current);
            if (value instanceof Map && next != null) {
                return getParameter((Map<?, ?>) value, next);
            } else if (value instanceof String && next == null) {
                return (String) value;
            }
        }
        return null;
    }
}
