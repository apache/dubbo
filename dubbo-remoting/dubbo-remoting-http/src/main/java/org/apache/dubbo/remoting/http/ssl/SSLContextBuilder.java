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
import javax.net.ssl.TrustManagerFactory;
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

    public static SSLContext sslContextBuild(InputStream keyCertChainInputStream, InputStream keyInputStream, InputStream trustCertCollectionInputStream,
                                             String keyPassword) throws Exception {
        X509Certificate[] keyCertChain;
        PrivateKey key;
        try {
            keyCertChain = SslContext.toX509Certificates(keyCertChainInputStream);
        } catch (Exception e) {
            throw new IllegalArgumentException("Input stream not contain valid certificates.", e);
        }
        try {
            key = SslContext.toPrivateKey(keyInputStream, keyPassword);
        } catch (Exception e) {
            throw new IllegalArgumentException("Input stream does not contain valid private key.", e);
        }

        SSLContext context = SSLContext.getInstance("TLS");

        KeyManagerFactory kmf = keyManager(keyPassword, keyCertChain, key);


        TrustManagerFactory trustManagerFactory = trustManager(trustCertCollectionInputStream);


        context.init(kmf.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

        return context;

    }

    public static KeyManagerFactory keyManager(String keyPassword, X509Certificate[] keyCertChain, PrivateKey key) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {
        KeyStore keystore = KeyStore.getInstance("JKS");

        keystore.load(null);

        char[] password = keyPassword == null ? new char[0] : keyPassword.toCharArray();


        keystore.setKeyEntry("keyEntry", key, password, keyCertChain);


        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");

        kmf.init(keystore, password);
        return kmf;
    }

    public static TrustManagerFactory trustManager(InputStream trustCertCollectionInputStream) throws Exception {

        X509Certificate[] x509Certificates = SslContext.toX509Certificates(trustCertCollectionInputStream);

        return SslContext.buildTrustManagerFactory(x509Certificates, TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()), null);


    }

}
