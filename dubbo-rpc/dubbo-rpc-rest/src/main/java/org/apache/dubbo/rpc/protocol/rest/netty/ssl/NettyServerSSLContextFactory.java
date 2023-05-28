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
package org.apache.dubbo.rpc.protocol.rest.netty.ssl;


import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.ssl.AuthPolicy;
import org.apache.dubbo.remoting.http.ssl.SSLContextFactory;

import java.io.InputStream;
import java.security.Provider;
import java.security.Security;

/**
 * netty Server SSL Context factory
 */
public class NettyServerSSLContextFactory extends SSLContextFactory {


    @Override
    protected Object createSSLContext(URL url, InputStream certChainStream, InputStream privateKeyStream, InputStream trustCertCollectionStream, char[] passwordCharArray, AuthPolicy authPolicy) throws Exception {

        SslContextBuilder sslClientContextBuilder;
        if (passwordCharArray != null && passwordCharArray.length != 0) {
            sslClientContextBuilder = SslContextBuilder.forServer(certChainStream,
                privateKeyStream, new String(passwordCharArray));
        } else {
            sslClientContextBuilder = SslContextBuilder.forServer(certChainStream,
                privateKeyStream);
        }

        if (trustCertCollectionStream != null) {
            sslClientContextBuilder.trustManager(trustCertCollectionStream);
            if (authPolicy == AuthPolicy.CLIENT_AUTH) {
                sslClientContextBuilder.clientAuth(ClientAuth.REQUIRE);
            } else {
                sslClientContextBuilder.clientAuth(ClientAuth.OPTIONAL);
            }
        }

        return sslClientContextBuilder.sslProvider(findSslProvider()).build();
    }

    /**
     * Returns OpenSSL if available, otherwise returns the JDK provider.
     */
    private static SslProvider findSslProvider() {
        if (OpenSsl.isAvailable()) {
            logger.debug("Using OPENSSL provider.");
            return SslProvider.OPENSSL;
        }
        if (checkJdkProvider()) {
            logger.debug("Using JDK provider.");
            return SslProvider.JDK;
        }
        throw new IllegalStateException(
            "Could not find any valid TLS provider, please check your dependency or deployment environment, " +
                "usually netty-tcnative, Conscrypt, or Jetty NPN/ALPN is needed.");
    }

    private static boolean checkJdkProvider() {
        Provider[] jdkProviders = Security.getProviders("SSLContext.TLS");
        return (jdkProviders != null && jdkProviders.length > 0);
    }

}
