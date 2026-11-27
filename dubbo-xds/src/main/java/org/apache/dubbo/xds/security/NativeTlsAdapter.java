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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.client.InstanceAddressURL;
import org.apache.dubbo.remoting.api.connection.ConnectionStateProvider;
import org.apache.dubbo.rpc.protocol.tri.Http3Exchanger;
import org.apache.dubbo.xds.registry.XdsEndpoint;
import org.apache.dubbo.xds.registry.XdsServiceInstance;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.apache.dubbo.common.constants.CommonConstants.SSL_ENABLED_KEY;

/** Applies a frozen security context before an Invoker can enter the connection pool. */
public final class NativeTlsAdapter {
    public static final String CERTIFICATE_ATTRIBUTE = "dubbo.xds.certificate";

    private NativeTlsAdapter() {}

    public static XdsEndpoint endpoint(URL url) {
        if (url instanceof InstanceAddressURL
                && ((InstanceAddressURL) url).getInstance() instanceof XdsServiceInstance) {
            return ((XdsServiceInstance) ((InstanceAddressURL) url).getInstance()).getEndpoint();
        }
        Object value = url.getAttribute(XdsEndpoint.ATTRIBUTE);
        return value instanceof XdsEndpoint ? (XdsEndpoint) value : null;
    }

    public static URL decorate(URL url) {
        XdsEndpoint endpoint = endpoint(url);
        if (endpoint == null) {
            return url;
        }
        url = url.putAttribute("rpc.authority", endpoint.getTarget().authority());
        if ("tri".equals(endpoint.getTarget().getProtocol())) {
            url = url.putAttribute(ConnectionStateProvider.BACKOFF_ATTRIBUTE, Boolean.TRUE);
            if (!Http3Exchanger.isEnabled(url)) {
                url = url.putAttribute(ConnectionStateProvider.ASYNC_CONNECT_ATTRIBUTE, Boolean.TRUE);
            }
        }
        if (endpoint.getSecurity() == null) {
            if (endpoint.getCluster().hasTransportSocket()
                    || endpoint.getCluster().getTransportSocketMatchesCount() > 0) {
                throw new IllegalStateException("xDS transport security has no runtime");
            }
            return url;
        }
        NativeCertificate certificate = endpoint.getCertificate();
        url = url.putAttribute(SSL_ENABLED_KEY, certificate.isTlsEnabled());
        url = url.putAttribute(CERTIFICATE_ATTRIBUTE, certificate);
        url = url.putAttribute("ssl.server-name", certificate.getServerName());
        try {
            String policy = CertificateFiles.hex(MessageDigest.getInstance("SHA-256")
                    .digest(endpoint.getCluster().toByteArray()));
            url = url.putAttribute(
                    "connection.identity",
                    endpoint.getSecurity().getBootstrap().getFingerprint() + "/" + endpoint.identity() + "/" + policy
                            + "/" + certificate.getVersion());
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 unavailable", error);
        }
        return url;
    }
}
