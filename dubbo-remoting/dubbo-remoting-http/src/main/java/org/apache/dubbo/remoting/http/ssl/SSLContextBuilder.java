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
package org.apache.dubbo.remoting.http.ssl;


import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class SSLContextBuilder {

    public static SSLContext sslContextBuild(String keyCertChainInputStream, String keyInputStream, String trustCertCollectionInputStream,
                                             String keyPassword) throws Exception {
        return sslContextBuild(new FileInputStream(keyCertChainInputStream), new FileInputStream(keyInputStream), new FileInputStream(trustCertCollectionInputStream), keyPassword);

    }

    public static SSLContext sslContextBuild(InputStream clientCertChainStream, InputStream clientPrivateKeyStream, InputStream clientTrustCertCollectionStream,
                                             String keyPassword) throws Exception {


        SSLContext sslContext = createSSLContext();

        KeyManagerFactory keyManagerFactory = SSLContextBuilder.keyManager(clientCertChainStream, clientPrivateKeyStream,keyPassword);


        TrustManagerFactory trustManagerFactory = SSLContextBuilder.trustManager(clientTrustCertCollectionStream);

        TrustManager[] trustManagers = buildTrustManagers(trustManagerFactory);

        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagers, null);

        return sslContext;

    }

    public static KeyManagerFactory keyManager(InputStream keyCertChainInputStream, InputStream keyInputStream,String keyPassword) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {

        X509Certificate[] keyCertChain;

        try {
            keyCertChain = SslContext.toX509Certificates(keyCertChainInputStream);
        } catch (Exception e) {
            throw new IllegalArgumentException("Input stream not contain valid certificates.", e);
        }

        PrivateKey key;
        try {
            key = SslContext.toPrivateKey(keyInputStream, keyPassword);
        } catch (Exception e) {
            throw new IllegalArgumentException("Input stream does not contain valid private key.", e);
        }
        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());

        keystore.load(null);

        char[] password = keyPassword == null ? new char[0] : keyPassword.toCharArray();


        keystore.setKeyEntry("keyEntry", key, password, keyCertChain);


        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());

        kmf.init(keystore, password);
        return kmf;
    }

    public static TrustManagerFactory trustManager(InputStream trustCertCollectionInputStream) throws Exception {

        X509Certificate[] x509Certificates = SslContext.toX509Certificates(trustCertCollectionInputStream);

        return SslContext.buildTrustManagerFactory(x509Certificates, TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()), null);


    }

    public static TrustManager[] buildTrustManagers(TrustManagerFactory trustManagerFactory) {
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
        if (trustManagers != null && trustManagers.length > 0) {

            return trustManagers;
        }

        return new TrustManager[]{
            new X509TrustManager() {
                @Override
                public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                }

                @Override
                public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                }

                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return new java.security.cert.X509Certificate[]{};
                }
            }
        };
    }

    public static SSLContext createSSLContext() throws NoSuchAlgorithmException {
        SSLContext context = SSLContext.getInstance("TLS");
        return context;
    }
}
