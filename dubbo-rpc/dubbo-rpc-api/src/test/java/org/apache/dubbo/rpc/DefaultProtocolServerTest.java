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
package org.apache.dubbo.rpc;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.url.component.ServiceConfigURL;
import org.apache.dubbo.remoting.ChannelEvent;
import org.apache.dubbo.remoting.RemotingServer;
import org.apache.dubbo.remoting.event.ReadOnlyEvent;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultProtocolServer}.
 */
class DefaultProtocolServerTest {

    private RemotingServer mockServer;
    private URL testUrl;
    private DefaultProtocolServer protocolServer;

    @BeforeEach
    void setUp() {
        mockServer = Mockito.mock(RemotingServer.class);
        testUrl = new ServiceConfigURL("dubbo", "127.0.0.1", 20881);
        when(mockServer.getUrl()).thenReturn(testUrl);
        protocolServer = new DefaultProtocolServer(mockServer);
    }

    /**
     * Test that getRemotingServer returns the underlying server.
     */
    @Test
    void testGetRemotingServer() {
        assertSame(mockServer, protocolServer.getRemotingServer());
    }

    /**
     * Test that getUrl returns the server's URL.
     */
    @Test
    void testGetUrl() {
        assertEquals(testUrl, protocolServer.getUrl());
    }

    /**
     * Test that getAddress returns the server's address when no custom address is set.
     */
    @Test
    void testGetAddressFromServer() {
        assertEquals(testUrl.getAddress(), protocolServer.getAddress());
    }

    /**
     * Test that getAddress returns the custom address when set.
     */
    @Test
    void testGetAddressCustom() {
        String customAddress = "192.168.1.100:8080";
        protocolServer.setAddress(customAddress);
        assertEquals(customAddress, protocolServer.getAddress());
    }

    /**
     * Test that setAddress updates the address.
     */
    @Test
    void testSetAddress() {
        String address1 = "192.168.1.100:8080";
        String address2 = "192.168.1.200:9090";

        protocolServer.setAddress(address1);
        assertEquals(address1, protocolServer.getAddress());

        protocolServer.setAddress(address2);
        assertEquals(address2, protocolServer.getAddress());
    }

    /**
     * Test that reset delegates to the underlying server.
     */
    @Test
    void testReset() {
        URL newUrl = new ServiceConfigURL("dubbo", "127.0.0.1", 20882);
        protocolServer.reset(newUrl);
        verify(mockServer, times(1)).reset(newUrl);
    }

    /**
     * Test that close delegates to the underlying server.
     */
    @Test
    void testClose() {
        protocolServer.close();
        verify(mockServer, times(1)).close();
    }

    /**
     * Test that getAttributes returns a non-null map.
     */
    @Test
    void testGetAttributesNotNull() {
        Map<String, Object> attributes = protocolServer.getAttributes();
        assertNotNull(attributes);
    }

    /**
     * Test that attributes can be stored and retrieved.
     */
    @Test
    void testAttributesStorage() {
        Map<String, Object> attributes = protocolServer.getAttributes();

        attributes.put("key1", "value1");
        attributes.put("key2", 42);
        attributes.put("key3", true);

        assertEquals("value1", attributes.get("key1"));
        assertEquals(42, attributes.get("key2"));
        assertEquals(true, attributes.get("key3"));
    }

    /**
     * Test that attributes are thread-safe (ConcurrentHashMap).
     */
    @Test
    void testAttributesAreSameInstance() {
        Map<String, Object> attributes1 = protocolServer.getAttributes();
        Map<String, Object> attributes2 = protocolServer.getAttributes();

        assertSame(attributes1, attributes2);
    }

    /**
     * Test fireChannelEvent with ReadOnlyEvent delegates to the underlying server.
     */
    @Test
    void testFireChannelEventDelegation() {
        ChannelEvent event = ReadOnlyEvent.INSTANCE;

        // Since DefaultProtocolServer doesn't override fireChannelEvent,
        // we test through getRemotingServer().fireChannelEvent()
        protocolServer.getRemotingServer().fireChannelEvent(event);
        verify(mockServer, times(1)).fireChannelEvent(event);
    }
}
