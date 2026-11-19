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
package org.apache.dubbo.rpc.protocol.tri;

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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TripleGracefulShutdown}.
 */
class TripleGracefulShutdownTest {

    private TripleProtocol mockTripleProtocol;
    private ProtocolServer mockServer1;
    private ProtocolServer mockServer2;
    private RemotingServer mockRemotingServer1;
    private RemotingServer mockRemotingServer2;

    @BeforeEach
    void setUp() {
        mockTripleProtocol = Mockito.mock(TripleProtocol.class);
        mockServer1 = Mockito.mock(ProtocolServer.class);
        mockServer2 = Mockito.mock(ProtocolServer.class);
        mockRemotingServer1 = Mockito.mock(RemotingServer.class);
        mockRemotingServer2 = Mockito.mock(RemotingServer.class);

        when(mockServer1.getRemotingServer()).thenReturn(mockRemotingServer1);
        when(mockServer2.getRemotingServer()).thenReturn(mockRemotingServer2);
    }

    /**
     * Test that TripleGracefulShutdown implements GracefulShutdown.
     */
    @Test
    void testImplementsGracefulShutdown() {
        TripleGracefulShutdown shutdown = new TripleGracefulShutdown(mockTripleProtocol);
        assertTrue(shutdown instanceof GracefulShutdown);
    }

    /**
     * Test readonly sends ReadOnlyEvent to all servers.
     */
    @Test
    void testReadonlySendsReadOnlyEvent() {
        when(mockTripleProtocol.getServers()).thenReturn(Collections.singletonList(mockServer1));

        TripleGracefulShutdown shutdown = new TripleGracefulShutdown(mockTripleProtocol);
        shutdown.readonly();

        ArgumentCaptor<ChannelEvent> captor = ArgumentCaptor.forClass(ChannelEvent.class);
        verify(mockRemotingServer1, times(1)).fireChannelEvent(captor.capture());

        ChannelEvent capturedEvent = captor.getValue();
        assertTrue(capturedEvent instanceof ReadOnlyEvent);
        assertSame(ReadOnlyEvent.INSTANCE, capturedEvent);
    }

    /**
     * Test writeable does not send any event (not supported for Triple protocol).
     */
    @Test
    void testWriteableDoesNotSendEvent() {
        when(mockTripleProtocol.getServers()).thenReturn(Collections.singletonList(mockServer1));

        TripleGracefulShutdown shutdown = new TripleGracefulShutdown(mockTripleProtocol);
        shutdown.writeable();

        // Writeable is not supported for Triple protocol, so no event should be sent
        verify(mockRemotingServer1, never()).fireChannelEvent(WriteableEvent.INSTANCE);
    }

    /**
     * Test readonly sends event to multiple servers.
     */
    @Test
    void testReadonlyMultipleServers() {
        List<ProtocolServer> servers = Arrays.asList(mockServer1, mockServer2);
        when(mockTripleProtocol.getServers()).thenReturn(servers);

        TripleGracefulShutdown shutdown = new TripleGracefulShutdown(mockTripleProtocol);
        shutdown.readonly();

        verify(mockRemotingServer1, times(1)).fireChannelEvent(ReadOnlyEvent.INSTANCE);
        verify(mockRemotingServer2, times(1)).fireChannelEvent(ReadOnlyEvent.INSTANCE);
    }

    /**
     * Test with empty server list.
     */
    @Test
    void testEmptyServerList() {
        when(mockTripleProtocol.getServers()).thenReturn(Collections.emptyList());

        TripleGracefulShutdown shutdown = new TripleGracefulShutdown(mockTripleProtocol);

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
        when(mockTripleProtocol.getServers()).thenReturn(expectedServers);

        TripleGracefulShutdown shutdown = new TripleGracefulShutdown(mockTripleProtocol);

        // Trigger readonly to indirectly verify getServers is called
        shutdown.readonly();

        verify(mockTripleProtocol, times(1)).getServers();
    }

    /**
     * Test that writeable can be called multiple times without error.
     */
    @Test
    void testWriteableCanBeCalledMultipleTimes() {
        when(mockTripleProtocol.getServers()).thenReturn(Collections.singletonList(mockServer1));

        TripleGracefulShutdown shutdown = new TripleGracefulShutdown(mockTripleProtocol);

        // Should not throw exception even when called multiple times
        shutdown.writeable();
        shutdown.writeable();
        shutdown.writeable();

        // No events should be sent
        verify(mockRemotingServer1, never()).fireChannelEvent(WriteableEvent.INSTANCE);
    }
}
