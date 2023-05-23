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
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.ssl.Cert;
import org.apache.dubbo.common.ssl.CertManager;
import org.apache.dubbo.common.ssl.DefaultHostnameVerifier;
import org.apache.dubbo.common.ssl.util.JdkSslUtils;
import org.apache.dubbo.common.ssl.util.pem.PemReader;
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
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(RestClientSSLContexts.class);


    public static <T> T buildClientSSLContext(URL url, RestClientSSLContextSetter restClientSSLSetter, T t) {

        try {
            // first pem file
            return buildClientSSlContextByPem(url, restClientSSLSetter, t);
        } catch (Throwable e) {
            logger.warn("",e.getMessage() , "", "rest client build ssl context by pem  failed", e);

            return buildClientJdkSSlContext(url, restClientSSLSetter, t);
        }

    }


    public static <T> T buildClientJdkSSlContext(URL url, RestClientSSLContextSetter restClientSSLSetter, T t) {


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
            SSLContext sslContext = JdkSslUtils.createSSLContext();

            KeyManagerFactory keyManagerFactory = JdkSslUtils.createKeyManagerFactory(clientPrivateKeyStream, passwordCharArray);

            TrustManagerFactory trustManagerFactory = JdkSslUtils.createTrustManagerFactory(clientTrustCertCollectionStream, passwordCharArray);

            TrustManager[] trustManagers = JdkSslUtils.buildTrustManagers(trustManagerFactory);

            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagers, null);

            restClientSSLSetter.initSSLContext(sslContext, trustManagers);

            restClientSSLSetter.setHostnameVerifier(new DefaultHostnameVerifier());
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not build rest client SSLContext: ", e);
        } finally {
            JdkSslUtils.safeCloseStream(clientTrustCertCollectionStream);
            JdkSslUtils.safeCloseStream(clientCertChainStream);
            JdkSslUtils.safeCloseStream(clientPrivateKeyStream);
        }

        return t;


    }


    public static <T> T buildClientSSlContextByPem(URL url, RestClientSSLContextSetter restClientSSLSetter, T t) {


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


            SSLContext sslContext =
                SSLContextBuilder.createSSLContext();

            KeyManagerFactory keyManagerFactory = JdkSslUtils.createKeyManagerFactory(PemReader.readCertificates(clientCertChainStream), PemReader.readPrivateKey(clientPrivateKeyStream), consumerConnectionConfig.getPassword());


            TrustManagerFactory trustManagerFactory = SSLContextBuilder.trustManagerByPem(clientTrustCertCollectionStream);

            TrustManager[] trustManagers = JdkSslUtils.buildTrustManagers(trustManagerFactory);

            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagers, null);

            restClientSSLSetter.initSSLContext(sslContext, trustManagers);

            restClientSSLSetter.setHostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not build rest client SSLContext by pem cert: ", e);
        } finally {
            JdkSslUtils.safeCloseStream( clientTrustCertCollectionStream);
            JdkSslUtils.safeCloseStream(clientCertChainStream);
            JdkSslUtils.safeCloseStream(clientPrivateKeyStream);
        }

        return t;


    }


}
