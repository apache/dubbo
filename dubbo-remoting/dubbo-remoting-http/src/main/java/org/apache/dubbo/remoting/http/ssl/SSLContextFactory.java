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
import org.apache.dubbo.common.ssl.AuthPolicy;
import org.apache.dubbo.common.ssl.Cert;
import org.apache.dubbo.common.ssl.CertManager;
import org.apache.dubbo.common.ssl.DefaultHostnameVerifier;
import org.apache.dubbo.common.ssl.ProviderCert;
import org.apache.dubbo.common.ssl.util.JdkSslUtils;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.io.InputStream;
import java.net.SocketAddress;

/**
 * ssl context factory
 */
public abstract class SSLContextFactory {

    private TrustManager[] trustManagers;
    private KeyManager[] keyManagers;

    protected static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(SSLContextFactory.class);

    public <T> T buildClientSSLContext(URL url, RestClientSSLContextSetter restClientSSLSetter, T restClient) {

        Object sslContext = buildClientSSLContext(url);

        afterClientSSLContextCreate(restClientSSLSetter, sslContext);
        return restClient;

    }

    public SSLContext buildClientSSLContext(URL url) {

        return SSLContextCache.getClientSSLContextFromCache(url, cert -> buildSSLContext(url, getConsumerCert(url)));
    }

    public Object buildServerSSLContext(URL url, ProviderCert providerConnectionConfig) {

        return SSLContextCache.getServerSSLContextFromCache(url, cert -> buildSSLContext(url, providerConnectionConfig));
    }

    public Object buildSSLContext(URL url, Cert certConfig) {


        if (certConfig == null) {
            return null;
        }

        InputStream trustCertCollectionStream = null;
        InputStream certChainStream = null;
        InputStream privateKeyStream = null;

        privateKeyStream = certConfig.getPrivateKeyInputStream();


        if (privateKeyStream == null) {
            return null;
        }

        certChainStream = certConfig.getKeyCertChainInputStream();
        trustCertCollectionStream = certConfig.getTrustCertInputStream();
        String password = certConfig.getPassword();

        char[] passwordCharArray = JdkSslUtils.strPasswordToCharArray(password);

        AuthPolicy authPolicy = null;
        if (certConfig instanceof ProviderCert) {
            authPolicy = ((ProviderCert) certConfig).getAuthPolicy();

        }
        try {

            return createSSLContext(url, certChainStream, privateKeyStream,
                trustCertCollectionStream, passwordCharArray, authPolicy);
        } catch (Exception e) {
            logger.warn("", e.getMessage(), "", "rest client build ssl context failed", e);
            throw new IllegalArgumentException("Could not build rest client SSLContext: ", e);

        } finally {
            JdkSslUtils.safeCloseStream(trustCertCollectionStream);
            JdkSslUtils.safeCloseStream(certChainStream);
            JdkSslUtils.safeCloseStream(privateKeyStream);
        }
    }

    protected HostnameVerifier getHostnameVerifier() {
        return new DefaultHostnameVerifier();
    }


    protected abstract Object createSSLContext(URL url, InputStream clientCertChainStream, InputStream clientPrivateKeyStream, InputStream clientTrustCertCollectionStream, char[] passwordCharArray, AuthPolicy authPolicy) throws Exception;


    private Cert getConsumerCert(URL url) {
        CertManager certManager = url.getOrDefaultFrameworkModel().getBeanFactory().getBean(CertManager.class);
        return certManager.getConsumerConnectionConfig(url);
    }

    private ProviderCert getProviderCert(URL url, SocketAddress remoteAddress) {
        CertManager certManager = url.getOrDefaultFrameworkModel().getBeanFactory().getBean(CertManager.class);
        return certManager.getProviderConnectionConfig(url, remoteAddress);
    }

    public void setTrustManagers(TrustManager[] trustManagers) {
        this.trustManagers = trustManagers;
    }

    public void setKeyManagers(KeyManager[] keyManagers) {
        this.keyManagers = keyManagers;
    }

    public void afterClientSSLContextCreate(RestClientSSLContextSetter restClientSSLSetter, Object sslContext) {

        restClientSSLSetter.initSSLContext((SSLContext) sslContext, trustManagers);

        restClientSSLSetter.setHostnameVerifier(getHostnameVerifier());

    }
}
