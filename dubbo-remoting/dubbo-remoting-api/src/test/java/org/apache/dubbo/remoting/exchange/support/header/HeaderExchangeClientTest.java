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
package org.apache.dubbo.remoting.exchange.support.header;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.Client;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class HeaderExchangeClientTest {
    @Test
    void testReconnect() {
        HeaderExchangeClient headerExchangeClient = new HeaderExchangeClient(Mockito.mock(Client.class), false);

        Assertions.assertTrue(headerExchangeClient.shouldReconnect(URL.valueOf("localhost")));
        Assertions.assertTrue(headerExchangeClient.shouldReconnect(URL.valueOf("localhost?reconnect=true")));
        Assertions.assertTrue(headerExchangeClient.shouldReconnect(URL.valueOf("localhost?reconnect=tRue")));
        Assertions.assertTrue(headerExchangeClient.shouldReconnect(URL.valueOf("localhost?reconnect=30000")));
        Assertions.assertTrue(headerExchangeClient.shouldReconnect(URL.valueOf("localhost?reconnect=0")));
        Assertions.assertTrue(headerExchangeClient.shouldReconnect(URL.valueOf("localhost?reconnect=-1")));
        Assertions.assertFalse(headerExchangeClient.shouldReconnect(URL.valueOf("localhost?reconnect=false")));
        Assertions.assertFalse(headerExchangeClient.shouldReconnect(URL.valueOf("localhost?reconnect=FALSE")));
    }

    @Test
    void testIsClosedWhenUnderlyingClientClosed() {
        Client mockClient = Mockito.mock(Client.class);
        // Underlying client is closed, but HeaderExchangeChannel is not
        Mockito.when(mockClient.isClosed()).thenReturn(true);
        Mockito.when(mockClient.getUrl()).thenReturn(URL.valueOf("dubbo://localhost:20880"));

        HeaderExchangeClient headerExchangeClient = new HeaderExchangeClient(mockClient, false);

        Assertions.assertTrue(headerExchangeClient.isClosed(),
                "HeaderExchangeClient should report closed when underlying client is closed");
    }

    @Test
    void testIsNotClosedWhenBothOpen() {
        Client mockClient = Mockito.mock(Client.class);
        Mockito.when(mockClient.isClosed()).thenReturn(false);
        Mockito.when(mockClient.getUrl()).thenReturn(URL.valueOf("dubbo://localhost:20880"));

        HeaderExchangeClient headerExchangeClient = new HeaderExchangeClient(mockClient, false);

        Assertions.assertFalse(headerExchangeClient.isClosed(),
                "HeaderExchangeClient should report open when both channel and client are open");
    }
}
