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

import org.apache.dubbo.remoting.ChannelEvent;
import org.apache.dubbo.remoting.RemotingServer;
import org.apache.dubbo.remoting.event.ReadOnlyEvent;
import org.apache.dubbo.remoting.event.WriteableEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
 * Unit tests for {@link AbstractGracefulShutdown}.
 */
class AbstractGracefulShutdownTest {

    private ProtocolServer mockServer1;
    private ProtocolServer mockServer2;
    private RemotingServer mockRemotingServer1;
    private RemotingServer mockRemotingServer2;
    private List<ChannelEvent> capturedEvents;

    @BeforeEach
    void setUp() {
        mockServer1 = Mockito.mock(ProtocolServer.class);
        mockServer2 = Mockito.mock(ProtocolServer.class);
        mockRemotingServer1 = Mockito.mock(RemotingServer.class);
        mockRemotingServer2 = Mockito.mock(RemotingServer.class);

        when(mockServer1.getRemotingServer()).thenReturn(mockRemotingServer1);
        when(mockServer2.getRemotingServer()).thenReturn(mockRemotingServer2);

        capturedEvents = new ArrayList<>();
    }

    /**
     * Test fireChannelEvent fires event to single server.
     */
    @Test
    void testFireChannelEventSingleServer() {
        TestGracefulShutdown shutdown = new TestGracefulShutdown(Collections.singletonList(mockServer1));

        shutdown.readonly();

        verify(mockRemotingServer1, times(1)).fireChannelEvent(ReadOnlyEvent.INSTANCE);
    }

    /**
     * Test fireChannelEvent fires event to multiple servers.
     */
    @Test
    void testFireChannelEventMultipleServers() {
        TestGracefulShutdown shutdown = new TestGracefulShutdown(Arrays.asList(mockServer1, mockServer2));

        shutdown.readonly();

        verify(mockRemotingServer1, times(1)).fireChannelEvent(ReadOnlyEvent.INSTANCE);
        verify(mockRemotingServer2, times(1)).fireChannelEvent(ReadOnlyEvent.INSTANCE);
    }

    /**
     * Test readonly sends ReadOnlyEvent.
     */
    @Test
    void testReadonlySendsReadOnlyEvent() {
        TestGracefulShutdown shutdown = new TestGracefulShutdown(Collections.singletonList(mockServer1));

        shutdown.readonly();

        ArgumentCaptor<ChannelEvent> captor = ArgumentCaptor.forClass(ChannelEvent.class);
        verify(mockRemotingServer1).fireChannelEvent(captor.capture());

        ChannelEvent capturedEvent = captor.getValue();
        assertTrue(capturedEvent instanceof ReadOnlyEvent);
        assertSame(ReadOnlyEvent.INSTANCE, capturedEvent);
    }

    /**
     * Test writeable sends WriteableEvent.
     */
    @Test
    void testWriteableSendsWriteableEvent() {
        TestGracefulShutdown shutdown = new TestGracefulShutdown(Collections.singletonList(mockServer1));

        shutdown.writeable();

        ArgumentCaptor<ChannelEvent> captor = ArgumentCaptor.forClass(ChannelEvent.class);
        verify(mockRemotingServer1).fireChannelEvent(captor.capture());

        ChannelEvent capturedEvent = captor.getValue();
        assertTrue(capturedEvent instanceof WriteableEvent);
        assertSame(WriteableEvent.INSTANCE, capturedEvent);
    }

    /**
     * Test that exceptions are caught and don't propagate.
     */
    @Test
    void testExceptionsAreCaught() {
        Mockito.doThrow(new RuntimeException("Test exception"))
                .when(mockRemotingServer1)
                .fireChannelEvent(Mockito.any(ChannelEvent.class));

        TestGracefulShutdown shutdown = new TestGracefulShutdown(Collections.singletonList(mockServer1));

        // Should not throw exception
        shutdown.readonly();
    }

    /**
     * Test with empty server list.
     */
    @Test
    void testEmptyServerList() {
        TestGracefulShutdown shutdown = new TestGracefulShutdown(Collections.emptyList());

        // Should not throw exception
        shutdown.readonly();
        shutdown.writeable();
    }

    /**
     * Test implementation of AbstractGracefulShutdown for testing purposes.
     */
    private static class TestGracefulShutdown extends AbstractGracefulShutdown {
        private final Collection<ProtocolServer> servers;

        TestGracefulShutdown(Collection<ProtocolServer> servers) {
            this.servers = servers;
        }

        @Override
        protected Collection<ProtocolServer> getServers() {
            return servers;
        }

        @Override
        public void readonly() {
            fireChannelEvent(ReadOnlyEvent.INSTANCE);
        }

        @Override
        public void writeable() {
            fireChannelEvent(WriteableEvent.INSTANCE);
        }
    }
}
