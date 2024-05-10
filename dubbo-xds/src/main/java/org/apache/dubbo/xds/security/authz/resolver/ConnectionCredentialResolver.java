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
package org.apache.dubbo.xds.security.authz.resolver;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.remoting.api.ChannelContextListener;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcContextAttachment;
import org.apache.dubbo.xds.security.authz.RequestCredential;
import org.apache.dubbo.xds.security.authz.rule.RequestAuthProperty;

import javax.net.ssl.SSLSession;

import java.net.InetSocketAddress;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.grpc.netty.shaded.io.netty.channel.ChannelHandlerContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslHandler;

@Activate(order = -20)
public class ConnectionCredentialResolver implements CredentialResolver, ChannelContextListener {

    private final Map<String, ConnectionCredential> connectionInfos = new ConcurrentHashMap<>();

    private static final ErrorTypeAwareLogger logger =
            LoggerFactory.getErrorTypeAwareLogger(ConnectionCredentialResolver.class);

    @Override
    public void appendRequestCredential(URL url, Invocation invocation, RequestCredential requestCredential) {
        requestCredential.add(
                RequestAuthProperty.TARGET_VERSION,
                invocation.getInvoker().getUrl().getVersion());
        RpcContextAttachment serverContext = RpcContext.getServerContext();
        requestCredential.add(
                RequestAuthProperty.DIRECT_REMOTE_IP,
                serverContext.getRemoteAddress().getHostName());
        requestCredential.add(RequestAuthProperty.REMOTE_PORT, serverContext.getRemotePort());
        requestCredential.add(RequestAuthProperty.REMOTE_APPLICATION, serverContext.getRemoteApplicationName());
        requestCredential.add(RequestAuthProperty.REMOTE_GROUP, serverContext.getGroup());
        requestCredential.add(RequestAuthProperty.DESTINATION_IP, serverContext.getLocalHost());
        requestCredential.add(RequestAuthProperty.DESTINATION_PORT, serverContext.getLocalPort());

        ConnectionCredential credential = connectionInfos.get(url.getIp() + ":" + url.getPort());
        if (credential != null) {
            requestCredential.add(RequestAuthProperty.CONNECTION_CREDENTIAL, credential);
            requestCredential.add(RequestAuthProperty.REQUESTED_SERVER_NAME, credential.getSni());
        }
    }

    @Override
    public void onConnect(Object channelContext) {
        if (channelContext instanceof ChannelHandlerContext) {
            ChannelHandlerContext context = (ChannelHandlerContext) channelContext;
            SslHandler sslHandler = context.pipeline().get(SslHandler.class);
            if (sslHandler != null) {
                SSLSession sslSession = sslHandler.engine().getSession();
                String applicationProtocol = sslSession.getProtocol();
                try {
                    Certificate[] peerCertificates = sslSession.getPeerCertificates();
                    List<CertificateCredential> certCredentialList = new ArrayList<>(1);
                    for (Certificate certificate : peerCertificates) {
                        if (!(certificate instanceof X509Certificate)) {
                            logger.warn(
                                    "99-1",
                                    "",
                                    "",
                                    "One SSL certificate was ignored because it's not in X.509 format: " + certificate);
                            continue;
                        }
                        certCredentialList.add(new CertificateCredential((X509Certificate) certificate));
                    }
                    String remoteAddress = NetUtils.toAddressString(
                            (InetSocketAddress) context.channel().remoteAddress());
                    String sniHostName = sslSession.getPeerHost();
                    connectionInfos.put(
                            remoteAddress,
                            new ConnectionCredential(certCredentialList, applicationProtocol, sniHostName));
                } catch (Exception e) {
                    logger.warn("99-1", "", "", "Got exception when resolving certificate from SSL session", e);
                }
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("No SSL/TLS handler found in pipeline:"
                            + NetUtils.toAddressString(
                                    (InetSocketAddress) context.channel().remoteAddress()) + "-> "
                            + NetUtils.toAddressString(
                                    (InetSocketAddress) context.channel().localAddress()));
                }
            }
        }
    }

    @Override
    public void onDisconnect(Object channelContext) {
        if (channelContext instanceof ChannelHandlerContext) {
            ChannelHandlerContext context = (ChannelHandlerContext) channelContext;
            String remoteAddress = NetUtils.toAddressString(
                    (InetSocketAddress) context.channel().remoteAddress());
            connectionInfos.remove(remoteAddress);
        }
    }

    public static class ConnectionCredential {
        private final List<CertificateCredential> certificateCredentials;
        private final String applicationProtocol;
        private final String sni;

        public ConnectionCredential(
                List<CertificateCredential> certificateCredentials, String applicationProtocol, String sni) {
            this.certificateCredentials = certificateCredentials;
            this.applicationProtocol = applicationProtocol;
            this.sni = sni;
        }

        public List<CertificateCredential> getCertificateCredentials() {
            return certificateCredentials;
        }

        public String getApplicationProtocol() {
            return applicationProtocol;
        }

        public String getSni() {
            return sni;
        }
    }

    public static class CertificateCredential {
        private final X509Certificate certificate;
        private final String subject;
        private final String issuer;
        private final Map<SANType, List<Object>> subjectAltNames;
        private final Date certNotBefore;
        private final Date certNotAfter;
        private final String signatureAlgorithmName;
        private final String publicKeyAlgorithmName;
        private final Set<String> criticalExtensionOIDs;
        private final List<String> extendedKeyUsage;

        public CertificateCredential(X509Certificate cert) throws Exception {
            this.subject = cert.getSubjectX500Principal().getName();
            this.issuer = cert.getIssuerX500Principal().toString();
            this.certNotBefore = cert.getNotBefore();
            this.certNotAfter = cert.getNotAfter();
            this.subjectAltNames = extractDetailedFields(cert);
            this.signatureAlgorithmName = cert.getSigAlgName();
            this.publicKeyAlgorithmName = cert.getPublicKey().getAlgorithm();
            this.criticalExtensionOIDs = cert.getCriticalExtensionOIDs();
            // e.g., TLS Web Server Authentication, TLS Web Client Authentication
            this.extendedKeyUsage = cert.getExtendedKeyUsage();
            this.certificate = cert;
        }

        private Map<SANType, List<Object>> extractDetailedFields(X509Certificate cert) throws Exception {
            Collection<List<?>> subjectAltNames = cert.getSubjectAlternativeNames();
            if (subjectAltNames != null) {
                Map<SANType, List<Object>> sanMap = new HashMap<>();
                for (List<?> sanItem : subjectAltNames) {
                    SANType type = SANType.map((Integer) sanItem.get(0));
                    Object value = sanItem.get(1);
                    sanMap.computeIfAbsent(type, k -> new ArrayList<>()).add(value);
                }
            }

            return Collections.emptyMap();
        }

        public Map<SANType, List<Object>> getSubjectAltNames() {
            return subjectAltNames;
        }

        public List<String> getExtendedKeyUsage() {
            return extendedKeyUsage;
        }

        public Set<String> getCriticalExtensionOIDs() {
            return criticalExtensionOIDs;
        }

        public String getPublicKeyAlgorithmName() {
            return publicKeyAlgorithmName;
        }

        public String getSignatureAlgorithmName() {
            return signatureAlgorithmName;
        }

        public Date getCertNotAfter() {
            return certNotAfter;
        }

        public Date getCertNotBefore() {
            return certNotBefore;
        }

        public String getSubject() {
            return subject;
        }

        public String getIssuer() {
            return issuer;
        }

        public X509Certificate getCertificate() {
            return certificate;
        }
    }

    public enum SANType {
        OTHER_NAME(0),
        RFC_822_NAME(1),
        DNS_NAME(2),
        X400_ADDRESS(3),
        DIRECTORY_NAME(4),
        EDI_PARTY_NAME(5),
        URI(6),
        IP_ADDRESS(7),
        REGISTERED_ID(8);

        private final int value;

        SANType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static SANType map(int value) {
            switch (value) {
                case 0:
                    return OTHER_NAME;
                case 1:
                    return RFC_822_NAME;
                case 2:
                    return DNS_NAME;
                case 3:
                    return X400_ADDRESS;
                case 4:
                    return DIRECTORY_NAME;
                case 5:
                    return EDI_PARTY_NAME;
                case 6:
                    return URI;
                case 7:
                    return IP_ADDRESS;
                case 8:
                    return REGISTERED_ID;
                default:
                    throw new IllegalArgumentException("Unknown SAN value: " + value);
            }
        }
    }
}
