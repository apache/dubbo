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
package org.apache.dubbo.xds.security;

import org.apache.dubbo.remoting.http12.HttpHeaders;

import javax.net.ssl.ExtendedSSLSession;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLSession;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Wire request and transport identities; identity headers are never promoted to peer principals. */
public final class RbacRequest {
    final Map<String, String> headers;
    final InetSocketAddress local;
    final InetSocketAddress remote;
    final List<String> principals;
    final String serverName;

    public RbacRequest(HttpHeaders headers, SocketAddress local, SocketAddress remote, SSLSession session) {
        Map<String, String> values = new LinkedHashMap<>();
        headers.toMap()
                .forEach((name, entries) -> values.put(name.toLowerCase(Locale.ROOT), String.join(",", entries)));
        this.headers = Collections.unmodifiableMap(values);
        this.local = local instanceof InetSocketAddress ? (InetSocketAddress) local : null;
        this.remote = remote instanceof InetSocketAddress ? (InetSocketAddress) remote : null;
        principals = Collections.unmodifiableList(peerPrincipals(session));
        String requestedName = "";
        if (session instanceof ExtendedSSLSession) {
            try {
                List<SNIServerName> names = ((ExtendedSSLSession) session).getRequestedServerNames();
                if (names != null) {
                    for (SNIServerName name : names) {
                        if (name instanceof SNIHostName) {
                            requestedName = ((SNIHostName) name).getAsciiName();
                            break;
                        }
                    }
                }
            } catch (UnsupportedOperationException unavailable) {
                /* A transport without SNI information cannot satisfy a requested-server-name matcher. */
            }
        }
        serverName = requestedName;
    }

    private static List<String> peerPrincipals(SSLSession session) {
        List<String> result = new ArrayList<>();
        if (session == null) {
            return result;
        }
        try {
            Certificate[] chain = session.getPeerCertificates();
            if (chain.length == 0 || !(chain[0] instanceof X509Certificate)) {
                return result;
            }
            X509Certificate certificate = (X509Certificate) chain[0];
            Collection<List<?>> sans = certificate.getSubjectAlternativeNames();
            for (int type : new int[] {6, 2}) {
                if (sans != null) {
                    for (List<?> san : sans) {
                        if (san.size() >= 2
                                && Integer.valueOf(type).equals(san.get(0))
                                && san.get(1) instanceof String) {
                            result.add((String) san.get(1));
                        }
                    }
                }
                if (!result.isEmpty()) {
                    return result;
                }
            }
            result.add(certificate.getSubjectX500Principal().getName());
        } catch (Exception ignored) {
            /* Unverified sessions have no authenticated principal. */
        }
        return result;
    }

    String path() {
        String path = headers.getOrDefault(":path", "");
        int query = path.indexOf('?');
        int fragment = path.indexOf('#');
        int end = path.length();
        if (query >= 0) {
            end = query;
        }
        if (fragment >= 0) {
            end = Math.min(end, fragment);
        }
        return path.substring(0, end);
    }
}
