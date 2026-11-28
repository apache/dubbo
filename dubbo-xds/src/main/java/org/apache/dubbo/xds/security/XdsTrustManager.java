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

import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;

import java.io.ByteArrayInputStream;
import java.net.Socket;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.transport_sockets.tls.v3.CertificateValidationContext;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.transport_sockets.tls.v3.SubjectAltNameMatcher;
import io.grpc.xds.shaded.io.envoyproxy.envoy.type.matcher.v3.StringMatcher;
import io.netty.handler.ssl.util.SimpleTrustManagerFactory;

/** Validates PKIX first, then the control plane's SAN constraints instead of DNS host-name guessing. */
public final class XdsTrustManager extends X509ExtendedTrustManager {
    private final X509TrustManager delegate;
    private final List<SanMatch> matchers = new ArrayList<>();

    public XdsTrustManager(byte[] roots, CertificateValidationContext validation) {
        try {
            if (roots == null || roots.length == 0) {
                throw new CertificateException("Missing xDS trust roots");
            }
            Collection<? extends Certificate> certificates =
                    CertificateFactory.getInstance("X.509").generateCertificates(new ByteArrayInputStream(roots));
            if (certificates.isEmpty()) {
                throw new CertificateException("Empty xDS trust roots");
            }
            KeyStore store = KeyStore.getInstance(KeyStore.getDefaultType());
            store.load(null, null);
            int index = 0;
            for (Certificate certificate : certificates) {
                store.setCertificateEntry("xds-root-" + index++, certificate);
            }
            TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            factory.init(store);
            X509TrustManager selected = null;
            for (TrustManager manager : factory.getTrustManagers()) {
                if (manager instanceof X509TrustManager) {
                    selected = (X509TrustManager) manager;
                    break;
                }
            }
            if (selected == null) {
                throw new CertificateException("No PKIX trust manager available");
            }
            delegate = selected;
            for (StringMatcher matcher : validation.getMatchSubjectAltNamesList()) {
                matchers.add(new SanMatch(-1, XdsMatchers.string(matcher)));
            }
            for (SubjectAltNameMatcher matcher : validation.getMatchTypedSubjectAltNamesList()) {
                int type;
                switch (matcher.getSanType()) {
                    case EMAIL:
                        type = 1;
                        break;
                    case DNS:
                        type = 2;
                        break;
                    case URI:
                        type = 6;
                        break;
                    case IP_ADDRESS:
                        type = 7;
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported typed SAN");
                }
                matchers.add(new SanMatch(type, XdsMatchers.string(matcher.getMatcher())));
            }
        } catch (Exception error) {
            throw new IllegalArgumentException("Cannot initialize xDS certificate validation", error);
        }
    }

    public TrustManagerFactory factory() {
        return new SimpleTrustManagerFactory() {
            @Override
            protected void engineInit(KeyStore store) {}

            @Override
            protected void engineInit(ManagerFactoryParameters parameters) {}

            @Override
            protected TrustManager[] engineGetTrustManagers() {
                return new TrustManager[] {XdsTrustManager.this};
            }
        };
    }

    private void checkSans(X509Certificate certificate) throws CertificateException {
        if (matchers.isEmpty()) {
            return;
        }
        Collection<List<?>> alternatives = certificate.getSubjectAlternativeNames();
        if (alternatives != null) {
            for (List<?> alternative : alternatives) {
                if (alternative.size() < 2
                        || !(alternative.get(0) instanceof Integer)
                        || !(alternative.get(1) instanceof String)) {
                    continue;
                }
                int type = (Integer) alternative.get(0);
                if (type != 1 && type != 2 && type != 6 && type != 7) {
                    continue;
                }
                for (SanMatch matcher : matchers) {
                    if ((matcher.type == -1 || matcher.type == type)
                            && matcher.predicate.test((String) alternative.get(1))) {
                        return;
                    }
                }
            }
        }
        throw new CertificateException("Peer certificate does not match xDS subject alternative names");
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        delegate.checkClientTrusted(chain, authType);
        checkSans(chain[0]);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        delegate.checkServerTrusted(chain, authType);
        checkSans(chain[0]);
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket)
            throws CertificateException {
        checkClientTrusted(chain, authType);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket)
            throws CertificateException {
        checkServerTrusted(chain, authType);
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
            throws CertificateException {
        checkClientTrusted(chain, authType);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
            throws CertificateException {
        checkServerTrusted(chain, authType);
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return delegate.getAcceptedIssuers();
    }

    private static final class SanMatch {
        final int type;
        final Predicate<String> predicate;

        SanMatch(int type, Predicate<String> predicate) {
            this.type = type;
            this.predicate = predicate;
        }
    }
}
