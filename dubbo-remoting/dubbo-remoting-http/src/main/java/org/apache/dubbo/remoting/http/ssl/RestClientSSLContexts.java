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

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.TRANSPORT_FAILED_CLOSE_STREAM;

/**
 * for rest client ssl context build
 */
public class RestClientSSLContexts {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(RestClientSSLContexts.class);

    public static <T> T buildClientSslContext(URL url, RestClientSSLSetter restClientSSLSetter, T t) {


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

            // TODO add  SSLContext cache for decreasing cost of SSLContext build
            // TODO add others format certificate  parsing
            // TODO add openssl certificate support
            SSLContext sslContext =
                SSLContextBuilder.createSSLContext();

            KeyManagerFactory keyManagerFactory = SSLContextBuilder.keyManager(clientCertChainStream, clientPrivateKeyStream, consumerConnectionConfig.getPassword());


            TrustManagerFactory trustManagerFactory = SSLContextBuilder.trustManager(clientTrustCertCollectionStream);

            TrustManager[] trustManagers = SSLContextBuilder.buildTrustManagers(trustManagerFactory);

            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagers, null);

            restClientSSLSetter.initSSLContext(sslContext, trustManagers);

            restClientSSLSetter.setHostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not build rest client SSLContext: ", e);
        } finally {
            safeCloseStream(clientTrustCertCollectionStream);
            safeCloseStream(clientCertChainStream);
            safeCloseStream(clientPrivateKeyStream);
        }

        return t;


    }

    private static void safeCloseStream(InputStream stream) {
        if (stream == null) {
            return;
        }
        try {
            stream.close();
        } catch (IOException e) {
            logger.warn(TRANSPORT_FAILED_CLOSE_STREAM, "", "", "Failed to close a stream.", e);
        }
    }


}
