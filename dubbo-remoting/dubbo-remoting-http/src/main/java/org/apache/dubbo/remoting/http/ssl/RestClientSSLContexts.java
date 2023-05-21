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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.ssl.Cert;
import org.apache.dubbo.common.ssl.CertManager;
import org.apache.dubbo.common.ssl.util.JDKSSLUtils;
import org.apache.dubbo.common.ssl.util.pem.SSLContextBuilder;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;


/**
 * for rest client ssl context build
 */
public class RestClientSSLContexts {


    public static <T> T buildClientSSLContext(URL url, RestClientSSLSetter restClientSSLSetter, T t) {

        try {
            // first pem file
            return buildClientSSlContextByPem(url, restClientSSLSetter, t);
        } catch (Throwable e) {
            return buildClientJDKSSlContext(url, restClientSSLSetter, t);
        }

    }


    public static <T> T buildClientJDKSSlContext(URL url, RestClientSSLSetter restClientSSLSetter, T t) {


        InputStream clientTrustCertCollectionStream = null;
        InputStream clientCertChainStream = null;
        InputStream clientPrivateKeyStream = null;

        try {

            if (url == null) {
                return t;
            }


            CertManager certManager = url.getOrDefaultFrameworkModel().getBeanFactory().getBean(CertManager.class);
            Cert consumerConnectionConfig = certManager.getConsumerConnectionConfig(url);

            if (consumerConnectionConfig == null) {
                return t;
            }

            clientCertChainStream = consumerConnectionConfig.getKeyCertChainInputStream();
            clientPrivateKeyStream = consumerConnectionConfig.getPrivateKeyInputStream();
            clientTrustCertCollectionStream = consumerConnectionConfig.getTrustCertInputStream();
            String password = consumerConnectionConfig.getPassword();


            if (clientPrivateKeyStream == null) {
                return t;
            }

            char[] passwordCharArray = password == null ? new char[0] : password.toCharArray();


            // init ssl context
            SSLContext sslContext = JDKSSLUtils.createSslContext();

            KeyManagerFactory keyManagerFactory = JDKSSLUtils.createKeyManagerFactory(clientPrivateKeyStream, passwordCharArray);

            TrustManagerFactory trustManagerFactory = JDKSSLUtils.createTrustManagerFactory(clientTrustCertCollectionStream, passwordCharArray);

            TrustManager[] trustManagers = JDKSSLUtils.buildTrustManagers(trustManagerFactory);

            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagers, null);

            restClientSSLSetter.initSSLContext(sslContext, trustManagers);

            restClientSSLSetter.setHostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not build rest client SSLContext: ", e);
        } finally {
            JDKSSLUtils.safeCloseStream(clientTrustCertCollectionStream);
            JDKSSLUtils.safeCloseStream(clientCertChainStream);
            JDKSSLUtils.safeCloseStream(clientPrivateKeyStream);
        }

        return t;


    }


    public static <T> T buildClientSSlContextByPem(URL url, RestClientSSLSetter restClientSSLSetter, T t) {


        InputStream clientTrustCertCollectionStream = null;
        InputStream clientCertChainStream = null;
        InputStream clientPrivateKeyStream = null;

        try {

            if (url == null) {
                return t;
            }


            CertManager certManager = url.getOrDefaultFrameworkModel().getBeanFactory().getBean(CertManager.class);
            Cert consumerConnectionConfig = certManager.getConsumerConnectionConfig(url);

            if (consumerConnectionConfig == null) {
                return t;
            }

            clientCertChainStream = consumerConnectionConfig.getKeyCertChainInputStream();
            clientPrivateKeyStream = consumerConnectionConfig.getPrivateKeyInputStream();
            clientTrustCertCollectionStream = consumerConnectionConfig.getTrustCertInputStream();


            if (clientCertChainStream == null || clientPrivateKeyStream == null) {
                return t;
            }

            SSLContext sslContext =
                SSLContextBuilder.createSSLContext();

            KeyManagerFactory keyManagerFactory = SSLContextBuilder.keyManagerByPem(clientCertChainStream, clientPrivateKeyStream, consumerConnectionConfig.getPassword());


            TrustManagerFactory trustManagerFactory = SSLContextBuilder.trustManagerByPem(clientTrustCertCollectionStream);

            TrustManager[] trustManagers = JDKSSLUtils.buildTrustManagers(trustManagerFactory);

            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagers, null);

            restClientSSLSetter.initSSLContext(sslContext, trustManagers);

            restClientSSLSetter.setHostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not build rest client SSLContext: ", e);
        } finally {
            JDKSSLUtils.safeCloseStream( clientTrustCertCollectionStream);
            JDKSSLUtils.safeCloseStream(clientCertChainStream);
            JDKSSLUtils.safeCloseStream(clientPrivateKeyStream);
        }

        return t;


    }


}
