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


import org.apache.dubbo.common.ssl.util.JdkSslUtils;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;

public class JdkSSLContextFactory extends SSLContextFactory {

    @Override
    protected SSLContext createSSLContext(InputStream clientCertChainStream, InputStream clientPrivateKeyStream, InputStream clientTrustCertCollectionStream, char[] passwordCharArray) throws Exception {
        // init ssl context
        SSLContext sslContext = JdkSslUtils.createSSLContext();

        KeyManagerFactory keyManagerFactory = JdkSslUtils.createKeyManagerFactory(clientPrivateKeyStream, passwordCharArray);

        TrustManagerFactory trustManagerFactory = JdkSslUtils.createTrustManagerFactory(clientTrustCertCollectionStream, passwordCharArray);

        TrustManager[] trustManagers = JdkSslUtils.buildTrustManagers(trustManagerFactory);
        setTrustManagers(trustManagers);
        KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();
        setKeyManagers(keyManagers);
        sslContext.init(keyManagers, trustManagers, null);

        return sslContext;
    }
}
