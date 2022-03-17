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
package org.apache.dubbo.remoting.transport.netty4;

import io.netty.handler.ssl.JdkSslContext;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.OpenSslContext;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.ReferenceCountedOpenSslContext;
import org.apache.dubbo.config.SslConfig;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * ssl contexts test
 */
public class SslContextsTest {
    @Test
    public void testSslContexts() throws NoSuchFieldException, IllegalAccessException {
        //test openssl
        testSslContextsItem();

        MockedStatic<OpenSsl> openSslMockedStatic = Mockito.mockStatic(OpenSsl.class);
        openSslMockedStatic.when(OpenSsl::isAvailable).thenReturn(false);

        //test jdk
        testSslContextsItem();
    }

    protected void testSslContextsItem() throws NoSuchFieldException, IllegalAccessException {
        String cipher = "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256";
        String protocol = "TLSv1.3";

        ConfigManager globalConfigManager = ApplicationModel.getConfigManager();
        SslConfig sslConfig = new SslConfig();
        sslConfig.setCiphers(Arrays.asList(cipher));
        sslConfig.setProtocols(Arrays.asList(protocol));
        globalConfigManager.setSsl(sslConfig);

        SslContext sslContext = SslContexts.buildClientSslContext(null);
        if(sslContext instanceof JdkSslContext){
            JdkSslContext jdkSslContext = (JdkSslContext) sslContext;
            List<String> cipherSuites = jdkSslContext.cipherSuites();
            Assertions.assertTrue(cipherSuites.size() == 1 && cipherSuites.get(0).equals(cipher));
            Field protocols = JdkSslContext.class.getDeclaredField("protocols");
            protocols.setAccessible(true);
            String[] item = (String[])protocols.get(jdkSslContext);
            Assertions.assertTrue(item.length == 1 && item[0].equals(protocol));
        }
        else if(sslContext instanceof OpenSslContext){
            OpenSslContext openSslContext = (OpenSslContext) sslContext;
            Assertions.assertTrue(openSslContext instanceof ReferenceCountedOpenSslContext);
            List<String> cipherSuites = openSslContext.cipherSuites();
            Assertions.assertTrue(cipherSuites.size() == 1 && cipherSuites.get(0).equals(cipher));
            Field protocols = ReferenceCountedOpenSslContext.class.getDeclaredField("protocols");
            protocols.setAccessible(true);
            final String[] item = (String[]) protocols.get(openSslContext);
            Assertions.assertTrue(item.length == 1 && item[0].equals(protocol));
        }
    }
}
