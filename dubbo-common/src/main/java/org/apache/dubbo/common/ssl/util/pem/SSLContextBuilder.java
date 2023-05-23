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
     * @param keyCertChainInputStream
     * @param keyInputStream
     * @param trustCertCollectionInputStream
     * @param keyPassword
     * @return
     * @throws Exception
     */
    public static SSLContext buildSSLContextByPem(String keyCertChainInputStream, String keyInputStream, String trustCertCollectionInputStream,
                                                  String keyPassword) throws Exception {
        return buildSSLContextByPem(new FileInputStream(keyCertChainInputStream), new FileInputStream(keyInputStream), new FileInputStream(trustCertCollectionInputStream), keyPassword);

    }

    public static SSLContext buildSSLContextByPem(InputStream clientCertChainStream, InputStream clientPrivateKeyStream, InputStream clientTrustCertCollectionStream,
                                                  String keyPassword) throws Exception {


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

        X509Certificate[] x509Certificates = SSlContext.toX509Certificates(trustCertCollectionInputStream);

        return SSlContext.buildTrustManagerFactory(x509Certificates, TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()), null);


    }


    public static SSLContext createSSLContext() throws NoSuchAlgorithmException {

        return JdkSslUtils.createSSLContext();
    }

    /**
     * build ssl context by original
     *
     * @param keyCertChainPathStream
     * @param privateKeyPathStream
     * @param trustCertStream
     * @param password
     * @return
     */
    public static SSLContext buildJdkSSLContext(InputStream keyCertChainPathStream,
                                                InputStream privateKeyPathStream,
                                                InputStream trustCertStream, String password) {


        return JdkSslUtils.buildJdkSSLContext(keyCertChainPathStream, privateKeyPathStream, trustCertStream, password);

    }

}
