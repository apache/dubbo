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

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.TRANSPORT_FAILED_CLOSE_STREAM;

public class RestClientSSLContexts {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(RestClientSSLContexts.class);

    public static <T> T buildClientSslContext(URL url, RestClientSSLSetter restClientSSLSetter, T t) {


        InputStream clientTrustCertCollectionPath = null;
        InputStream clientCertChainFilePath = null;
        InputStream clientPrivateKeyFilePath = null;

        try {
//            CertManager certManager = url.getOrDefaultFrameworkModel().getBeanFactory().getBean(CertManager.class);
//            Cert consumerConnectionConfig = certManager.getConsumerConnectionConfig(url);
//
//            SslConfig sslConfig = new SslConfig();
//            sslConfig.setServerKeyCertChainPath("C:\\Users\\86181\\Desktop\\dubbo3\\ssl-demo\\ssl-demo\\TwoWaySSL-Client\\src\\main\\resources\\client_keystore.jks");
//            sslConfig.setServerPrivateKeyPath("C:\\Users\\86181\\Desktop\\dubbo3\\dubbo\\dubbo\\dubbo-common\\src\\test\\resources\\certs\\key.pem");
//            sslConfig.setServerTrustCertCollectionPath("C:\\Users\\86181\\Desktop\\dubbo3\\ssl-demo\\ssl-demo\\TwoWaySSL-Client\\src\\main\\resources\\client_truststore.jks");
//            ProviderCert consumerConnectionConfig=new ProviderCert(
//                IOUtils.toByteArray(sslConfig.getServerKeyCertChainPathStream()),
//                IOUtils.toByteArray(sslConfig.getServerPrivateKeyPathStream()),
//                sslConfig.getServerTrustCertCollectionPath() != null ? IOUtils.toByteArray(sslConfig.getServerTrustCertCollectionPathStream()) : null,
//                sslConfig.getServerKeyPassword(), AuthPolicy.CLIENT_AUTH);
//            if (consumerConnectionConfig == null) {
//                return t;
//            }
//            clientCertChainFilePath = consumerConnectionConfig.getKeyCertChainInputStream();
//            clientTrustCertCollectionPath = consumerConnectionConfig.getTrustCertInputStream();
//
//
//            if (clientCertChainFilePath == null || clientTrustCertCollectionPath == null) {
//                return t;
//            }
//
//            String password = consumerConnectionConfig.getPassword();
//
//            char[] pwdCharArray = password == null ? null : password.toCharArray();
//            // 加载证书库
//            KeyStore keyStore = KeyStore.getInstance("JKS");
//            keyStore.load(clientCertChainFilePath, pwdCharArray);
//
//            // 初始化信任库
//            KeyStore trustStore = KeyStore.getInstance("JKS");
//            trustStore.load(clientTrustCertCollectionPath, pwdCharArray);
//
//            // 初始化SSL上下文
//            SSLContext sslContext = SSLContext.getInstance("TLS");
//            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
//            keyManagerFactory.init(keyStore, pwdCharArray);
//            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
//            trustManagerFactory.init(trustStore);
//            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), new SecureRandom());

            TrustManager[] trustAllCerts = buildTrustManagers();
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            restClientSSLSetter.initSSLContext(sslContext,trustAllCerts);

            restClientSSLSetter.setHostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not build rest client SSLContext: ", e);
        } finally {
            safeCloseStream(clientTrustCertCollectionPath);
            safeCloseStream(clientCertChainFilePath);
            safeCloseStream(clientPrivateKeyFilePath);
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

    private static TrustManager[] buildTrustManagers() {
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

}
