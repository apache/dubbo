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
package org.apache.dubbo.rpc.protocol.dubbo;

import org.apache.dubbo.remoting.ChannelEvent;
import org.apache.dubbo.remoting.RemotingServer;
import org.apache.dubbo.remoting.event.ReadOnlyEvent;
import org.apache.dubbo.remoting.event.WriteableEvent;
import org.apache.dubbo.rpc.GracefulShutdown;
import org.apache.dubbo.rpc.ProtocolServer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DubboGracefulShutdown}.
 */
class DubboGracefulShutdownTest {

    private DubboProtocol mockDubboProtocol;
    private ProtocolServer mockServer1;
    private ProtocolServer mockServer2;
    private RemotingServer mockRemotingServer1;
    private RemotingServer mockRemotingServer2;

    @BeforeEach
    void setUp() {
        mockDubboProtocol = Mockito.mock(DubboProtocol.class);
        mockServer1 = Mockito.mock(ProtocolServer.class);
        mockServer2 = Mockito.mock(ProtocolServer.class);
        mockRemotingServer1 = Mockito.mock(RemotingServer.class);
        mockRemotingServer2 = Mockito.mock(RemotingServer.class);

        when(mockServer1.getRemotingServer()).thenReturn(mockRemotingServer1);
        when(mockServer2.getRemotingServer()).thenReturn(mockRemotingServer2);
    }

    /**
     * Test that DubboGracefulShutdown implements GracefulShutdown.
     */
    @Test
    void testImplementsGracefulShutdown() {
        DubboGracefulShutdown shutdown = new DubboGracefulShutdown(mockDubboProtocol);
        assertTrue(shutdown instanceof GracefulShutdown);
    }

    /**
     * Test readonly sends ReadOnlyEvent to all servers.
     */
    @Test
    void testReadonlySendsReadOnlyEvent() {
        when(mockDubboProtocol.getServers()).thenReturn(Collections.singletonList(mockServer1));

        DubboGracefulShutdown shutdown = new DubboGracefulShutdown(mockDubboProtocol);
        shutdown.readonly();

        ArgumentCaptor<ChannelEvent> captor = ArgumentCaptor.forClass(ChannelEvent.class);
        verify(mockRemotingServer1, times(1)).fireChannelEvent(captor.capture());

        ChannelEvent capturedEvent = captor.getValue();
        assertTrue(capturedEvent instanceof ReadOnlyEvent);
        assertSame(ReadOnlyEvent.INSTANCE, capturedEvent);
    }

    /**
     * Test writeable sends WriteableEvent to all servers.
     */
    @Test
    void testWriteableSendsWriteableEvent() {
        when(mockDubboProtocol.getServers()).thenReturn(Collections.singletonList(mockServer1));

        DubboGracefulShutdown shutdown = new DubboGracefulShutdown(mockDubboProtocol);
        shutdown.writeable();

        ArgumentCaptor<ChannelEvent> captor = ArgumentCaptor.forClass(ChannelEvent.class);
        verify(mockRemotingServer1, times(1)).fireChannelEvent(captor.capture());

        ChannelEvent capturedEvent = captor.getValue();
        assertTrue(capturedEvent instanceof WriteableEvent);
        assertSame(WriteableEvent.INSTANCE, capturedEvent);
    }

    /**
     * Test readonly sends event to multiple servers.
     */
    @Test
    void testReadonlyMultipleServers() {
        List<ProtocolServer> servers = Arrays.asList(mockServer1, mockServer2);
        when(mockDubboProtocol.getServers()).thenReturn(servers);

        DubboGracefulShutdown shutdown = new DubboGracefulShutdown(mockDubboProtocol);
        shutdown.readonly();

        verify(mockRemotingServer1, times(1)).fireChannelEvent(ReadOnlyEvent.INSTANCE);
        verify(mockRemotingServer2, times(1)).fireChannelEvent(ReadOnlyEvent.INSTANCE);
    }

    /**
     * Test writeable sends event to multiple servers.
     */
    @Test
    void testWriteableMultipleServers() {
        List<ProtocolServer> servers = Arrays.asList(mockServer1, mockServer2);
        when(mockDubboProtocol.getServers()).thenReturn(servers);

        DubboGracefulShutdown shutdown = new DubboGracefulShutdown(mockDubboProtocol);
        shutdown.writeable();

        verify(mockRemotingServer1, times(1)).fireChannelEvent(WriteableEvent.INSTANCE);
        verify(mockRemotingServer2, times(1)).fireChannelEvent(WriteableEvent.INSTANCE);
    }

    /**
     * Test with empty server list.
     */
    @Test
    void testEmptyServerList() {
        when(mockDubboProtocol.getServers()).thenReturn(Collections.emptyList());

        DubboGracefulShutdown shutdown = new DubboGracefulShutdown(mockDubboProtocol);

        // Should not throw exception
        shutdown.readonly();
        shutdown.writeable();
    }

    /**
     * Test that getServers returns the protocol's servers.
     */
    @Test
    void testGetServersReturnsProtocolServers() {
        List<ProtocolServer> expectedServers = Arrays.asList(mockServer1, mockServer2);
        when(mockDubboProtocol.getServers()).thenReturn(expectedServers);

        DubboGracefulShutdown shutdown = new DubboGracefulShutdown(mockDubboProtocol);

        // Trigger readonly to indirectly verify getServers is called
        shutdown.readonly();

        verify(mockDubboProtocol, times(1)).getServers();
    }
}
