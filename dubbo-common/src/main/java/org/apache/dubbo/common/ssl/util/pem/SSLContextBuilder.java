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


import org.apache.dubbo.common.ssl.util.JdkSslUtils;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;


public class SSLContextBuilder {


    /**
     * build ssl context by pem
     *
     * @param keyCertChainInputPath
     * @param keyInputPath
     * @param trustCertCollectionPath
     * @param keyPassword
     * @return
     * @throws Exception
     */
    public static SSLContext buildSSLContextByPem(String keyCertChainInputPath,
                                                  String keyInputPath,
                                                  String trustCertCollectionPath,
                                                  String keyPassword) throws Exception {

        FileInputStream keyCertChainInputStream = null;
        FileInputStream keyInputPathStream = null;
        FileInputStream trustCertCollectionInputStream = null;
        try {
            keyCertChainInputStream = new FileInputStream(keyCertChainInputPath);
            keyInputPathStream = new FileInputStream(keyInputPath);
            trustCertCollectionInputStream = new FileInputStream(trustCertCollectionPath);
            return buildSSLContextByPem(keyCertChainInputStream, keyInputPathStream, trustCertCollectionInputStream, keyPassword);
        } finally {
            JdkSslUtils.safeCloseStream(keyCertChainInputStream);
            JdkSslUtils.safeCloseStream(keyInputPathStream);
            JdkSslUtils.safeCloseStream(trustCertCollectionInputStream);

        }

    }

    public static SSLContext buildSSLContextByPem(InputStream clientCertChainStream, InputStream clientPrivateKeyStream, InputStream clientTrustCertCollectionStream,
                                                  String keyPassword) throws Exception {
        return buildSSLContextByPem(clientCertChainStream, clientPrivateKeyStream, clientTrustCertCollectionStream, JdkSslUtils.strPasswordToCharArray(keyPassword));
    }

    public static SSLContext buildSSLContextByPem(InputStream clientCertChainStream, InputStream clientPrivateKeyStream, InputStream clientTrustCertCollectionStream,
                                                  char[] keyPassword) throws Exception {


        SSLContext sslContext = createSSLContext();

        KeyManagerFactory keyManagerFactory = JdkSslUtils.createKeyManagerFactory(PemReader.readCertificates(clientCertChainStream), PemReader.readPrivateKey(clientPrivateKeyStream), keyPassword);


        TrustManagerFactory trustManagerFactory = SSLContextBuilder.trustManagerByPem(clientTrustCertCollectionStream);

        TrustManager[] trustManagers = JdkSslUtils.buildTrustManagers(trustManagerFactory);

        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagers, null);

        return sslContext;

    }

    public static TrustManagerFactory trustManagerByPem(InputStream trustCertCollectionInputStream) throws Exception {
        if (trustCertCollectionInputStream == null) {
            return null;
        }

        X509Certificate[] x509Certificates = TrustManagerBuilder.toX509Certificates(trustCertCollectionInputStream);

        return TrustManagerBuilder.buildTrustManagerFactory(x509Certificates, TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()), null);


    }


    public static SSLContext createSSLContext() throws NoSuchAlgorithmException {

        return JdkSslUtils.createSSLContext();
    }

}
