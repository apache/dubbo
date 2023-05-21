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
package org.apache.dubbo.common.ssl.util.pem;


import org.apache.dubbo.common.ssl.util.JDKSSLUtils;

import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;

public class SslContext {

    protected static X509Certificate[] toX509Certificates(InputStream in) throws CertificateException {
        if (in == null) {
            return null;
        }
        return getCertificatesFromBuffers(PemReader.readCertificates(in));
    }

    private static X509Certificate[] getCertificatesFromBuffers(List<byte[]> certs) throws CertificateException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate[] x509Certs = new X509Certificate[certs.size()];
        for (int i = 0; i < certs.size(); i++) {
            InputStream is = new ByteArrayInputStream(certs.get(i));
            try {
                x509Certs[i] = (X509Certificate) cf.generateCertificate(is);
            } finally {
                JDKSSLUtils.safeCloseStream(is);
            }
        }

        return x509Certs;
    }

    public static TrustManagerFactory buildTrustManagerFactory(
        X509Certificate[] certCollection, TrustManagerFactory trustManagerFactory, String keyStoreType)
        throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException {
        if (keyStoreType == null) {
            keyStoreType = KeyStore.getDefaultType();
        }
        final KeyStore ks = KeyStore.getInstance(keyStoreType);
        ks.load(null, null);

        int i = 1;
        for (X509Certificate cert : certCollection) {
            String alias = Integer.toString(i);
            ks.setCertificateEntry(alias, cert);
            i++;
        }

        // Set up trust manager factory to use our key store.
        if (trustManagerFactory == null) {
            trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        }
        trustManagerFactory.init(ks);

        return trustManagerFactory;
    }
}
