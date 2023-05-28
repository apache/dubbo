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
import org.apache.dubbo.common.ssl.AuthPolicy;
import org.apache.dubbo.common.ssl.util.JdkSslUtils;
import org.apache.dubbo.common.ssl.util.pem.PemReader;
import org.apache.dubbo.common.ssl.util.pem.SSLContextBuilderByPem;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;

/**
 * cert file is .pem
 */
public class PemSSLContextFactory extends SSLContextFactory {


    @Override
    protected SSLContext createSSLContext(URL url, InputStream clientCertChainStream, InputStream clientPrivateKeyStream, InputStream clientTrustCertCollectionStream, char[] passwordCharArray, AuthPolicy authPolicy) throws Exception {
        SSLContext sslContext = SSLContextBuilderByPem.createSSLContext();

        KeyManagerFactory keyManagerFactory = JdkSslUtils.createKeyManagerFactory(PemReader.readCertificates(clientCertChainStream), PemReader.readPrivateKey(clientPrivateKeyStream), passwordCharArray);


        TrustManagerFactory trustManagerFactory = SSLContextBuilderByPem.trustManagerByPem(clientTrustCertCollectionStream);

        TrustManager[] trustManagers = JdkSslUtils.buildTrustManagers(trustManagerFactory);

        setTrustManagers(trustManagers);
        KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();
        setKeyManagers(keyManagers);
        sslContext.init(keyManagers, trustManagers, null);
        return sslContext;
    }
}
