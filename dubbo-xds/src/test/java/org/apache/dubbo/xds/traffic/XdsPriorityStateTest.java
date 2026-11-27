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
package org.apache.dubbo.xds.traffic;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.api.connection.ConnectionStateProvider;
import org.apache.dubbo.remoting.api.connection.ConnectionStateProvider.State;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.xds.client.XdsResourceGraph.Snapshot;
import org.apache.dubbo.xds.registry.DiscoveryTarget;
import org.apache.dubbo.xds.registry.XdsEndpoint;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import com.google.protobuf.UInt32Value;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.cluster.v3.Cluster;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.Address;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.Locality;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.SocketAddress;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.Endpoint;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.LbEndpoint;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.LocalityLbEndpoints;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class XdsPriorityStateTest {
    @Test
    void initialGraceExpiresOnceFailureCancelsItAndRecoveryAllowsAnotherGrace() {
        AtomicLong clock = new AtomicLong();
        try (XdsPriorityState state = new XdsPriorityState(clock::get, TimeUnit.SECONDS.toNanos(10))) {
            Source primary = new Source(State.CONNECTING);
            Source fallback = new Source(State.READY);
            Invoker<Object> first = invoker(0, 17070, primary);
            Invoker<Object> second = invoker(1, 17071, fallback);
            List<Invoker<Object>> all = Arrays.asList(first, second);
            assertNull(state.ready("service/cluster", all));
            clock.set(TimeUnit.SECONDS.toNanos(10) - 1);
            assertNull(state.ready("service/cluster", all));
            clock.incrementAndGet();
            assertEquals(Collections.singletonList(second), state.ready("service/cluster", all));
            primary.set(State.READY);
            assertEquals(Collections.singletonList(first), state.ready("service/cluster", all));
            primary.set(State.TRANSIENT_FAILURE);
            assertEquals(Collections.singletonList(second), state.ready("service/cluster", all));
            primary.set(State.CONNECTING);
            assertEquals(
                    Collections.singletonList(second),
                    state.ready("service/cluster", all),
                    "A failed child cannot extend grace on every reconnect");
            primary.set(State.READY);
            primary.set(State.CONNECTING);
            assertNull(state.ready("service/cluster", all));
            clock.addAndGet(TimeUnit.SECONDS.toNanos(10));
            assertEquals(Collections.singletonList(second), state.ready("service/cluster", all));
        }
    }

    @Test
    void allConnectingStillBuffersAfterGraceButAllFailedReturnsUnavailableCandidates() {
        AtomicLong clock = new AtomicLong();
        try (XdsPriorityState state = new XdsPriorityState(clock::get, 10)) {
            Source first = new Source(State.CONNECTING);
            Source second = new Source(State.CONNECTING);
            List<Invoker<Object>> all = Arrays.asList(invoker(0, 17070, first), invoker(1, 17071, second));
            assertNull(state.ready("service/cluster", all));
            clock.set(30);
            assertNull(state.ready("service/cluster", all));
            first.set(State.TRANSIENT_FAILURE);
            second.set(State.TRANSIENT_FAILURE);
            assertTrue(state.ready("service/cluster", all).isEmpty());
            second.set(State.IDLE);
            assertNull(state.ready("service/cluster", all));
            second.set(State.SHUTDOWN);
            assertTrue(state.ready("service/cluster", all).isEmpty());
        }
    }

    @Test
    void priorityAggregatesAllItsEndpointsAndOneFailureDoesNotMakeItsConnectingSiblingFail() {
        AtomicLong clock = new AtomicLong();
        try (XdsPriorityState state = new XdsPriorityState(clock::get, 10)) {
            Source failed = new Source(State.TRANSIENT_FAILURE);
            Source connecting = new Source(State.CONNECTING);
            Source ready = new Source(State.READY);
            Invoker<Object> first = invoker(0, 17070, failed);
            Invoker<Object> second = invoker(0, 17071, connecting);
            Invoker<Object> fallback = invoker(1, 17072, ready);
            List<Invoker<Object>> all = Arrays.asList(first, second, fallback);
            assertNull(state.ready("service/cluster", all));
            connecting.set(State.READY);
            assertEquals(Collections.singletonList(second), state.ready("service/cluster", all));
            connecting.set(State.CONNECTING);
            assertNull(state.ready("service/cluster", all));
            connecting.set(State.TRANSIENT_FAILURE);
            assertEquals(Collections.singletonList(fallback), state.ready("service/cluster", all));
        }
    }

    @Test
    void removalAndReplacementReleaseSubscriptionsAndLateOldNotificationsCannotReviveAnEndpoint() {
        AtomicLong clock = new AtomicLong();
        Source old = new Source(State.CONNECTING);
        Source current = new Source(State.CONNECTING);
        Source fallback = new Source(State.READY);
        Invoker<Object> first = invoker(0, 17070, old);
        Invoker<Object> replacement = invoker(0, 17070, current);
        Invoker<Object> second = invoker(1, 17071, fallback);
        try (XdsPriorityState state = new XdsPriorityState(clock::get, 10)) {
            assertNull(state.ready("service/cluster", Arrays.asList(first, second)));
            assertEquals(1, old.listeners.size());
            assertNull(state.ready("service/cluster", Arrays.asList(replacement, second)));
            assertEquals(0, old.listeners.size());
            old.set(State.READY);
            assertNull(state.ready("service/cluster", Arrays.asList(replacement, second)));
            current.set(State.READY);
            assertEquals(
                    Collections.singletonList(replacement),
                    state.ready("service/cluster", Arrays.asList(replacement, second)));
            assertEquals(
                    Collections.singletonList(second),
                    state.ready("service/cluster", Collections.singletonList(second)));
            assertEquals(0, current.listeners.size());
        }
        assertEquals(0, fallback.listeners.size());
    }

    @Test
    void targetsOwnTheirHistoryAndSubscriptionsIndependently() {
        AtomicLong clock = new AtomicLong();
        Source source = new Source(State.CONNECTING);
        Invoker<Object> invoker = invoker(0, 17070, source);
        XdsPriorityState state = new XdsPriorityState(clock::get, 10);
        assertNull(state.ready("service-a/cluster", Collections.singletonList(invoker)));
        assertNull(state.ready("service-b/cluster", Collections.singletonList(invoker)));
        assertEquals(2, source.listeners.size());
        assertTrue(state.ready("service-a/cluster", Collections.emptyList()).isEmpty());
        assertEquals(1, source.listeners.size());
        source.set(State.READY);
        assertEquals(
                Collections.singletonList(invoker),
                state.ready("service-b/cluster", Collections.singletonList(invoker)));
        state.close();
        assertEquals(0, source.listeners.size());
        assertThrows(
                IllegalStateException.class,
                () -> state.ready("service-b/cluster", Collections.singletonList(invoker)));
    }

    @Test
    void currentTransportStateWinsOverDelayedAndStaleNotifications() {
        AtomicLong clock = new AtomicLong();
        try (XdsPriorityState state = new XdsPriorityState(clock::get, 10)) {
            Source primary = new Source(State.READY);
            Source fallback = new Source(State.READY);
            Invoker<Object> first = invoker(0, 17070, primary);
            Invoker<Object> second = invoker(1, 17071, fallback);
            List<Invoker<Object>> all = Arrays.asList(first, second);
            assertEquals(Collections.singletonList(first), state.ready("service/cluster", all));
            // The transport publishes its state before invoking registered listeners.
            primary.state = State.CONNECTING;
            assertNull(state.ready("service/cluster", all));
            clock.set(9);
            primary.listeners.forEach(listener -> listener.accept(State.READY));
            assertNull(state.ready("service/cluster", all), "A delayed READY cannot promote a live CONNECTING child");
            clock.set(10);
            assertEquals(Collections.singletonList(second), state.ready("service/cluster", all));
            primary.state = State.TRANSIENT_FAILURE;
            assertEquals(Collections.singletonList(second), state.ready("service/cluster", all));
            primary.state = State.READY;
            primary.listeners.forEach(listener -> listener.accept(State.TRANSIENT_FAILURE));
            assertEquals(Collections.singletonList(first), state.ready("service/cluster", all));
        }
    }

    @Test
    void aReadyEndpointChangingToConnectingDuringSelectionBuffersInsteadOfFailingUnavailable() {
        AtomicLong clock = new AtomicLong();
        try (XdsPriorityState state = new XdsPriorityState(clock::get, 10)) {
            Source primary = new Source(State.READY);
            Invoker<Object> first = invoker(0, 17070, primary);
            assertEquals(
                    Collections.singletonList(first), state.ready("service/cluster", Collections.singletonList(first)));
            when(first.isAvailable()).thenAnswer(ignored -> {
                primary.state = State.CONNECTING;
                return false;
            });
            assertNull(state.ready("service/cluster", Collections.singletonList(first)));
            primary.state = State.TRANSIENT_FAILURE;
            assertTrue(state.ready("service/cluster", Collections.singletonList(first))
                    .isEmpty());
        }
    }

    @SuppressWarnings("unchecked")
    private static Invoker<Object> invoker(int priority, int port, Source source) {
        Snapshot snapshot = mock(Snapshot.class);
        when(snapshot.getTarget()).thenReturn(new DiscoveryTarget("echo.test.svc.cluster.local", 7070, "tri"));
        Cluster cluster = Cluster.newBuilder().setName("cluster").build();
        LbEndpoint endpoint = LbEndpoint.newBuilder()
                .setEndpoint(Endpoint.newBuilder()
                        .setAddress(Address.newBuilder()
                                .setSocketAddress(SocketAddress.newBuilder()
                                        .setAddress("127.0.0.1")
                                        .setPortValue(port))))
                .build();
        LocalityLbEndpoints locality = LocalityLbEndpoints.newBuilder()
                .setPriority(priority)
                .setLocality(Locality.newBuilder().setZone("zone"))
                .setLoadBalancingWeight(UInt32Value.of(1))
                .addLbEndpoints(endpoint)
                .build();
        URL url = URL.valueOf("tri://127.0.0.1:" + port + "/Business")
                .putAttribute(XdsEndpoint.ATTRIBUTE, new XdsEndpoint(snapshot, cluster, locality, endpoint))
                .putAttribute(ConnectionStateProvider.ATTRIBUTE, source);
        Invoker<Object> invoker = mock(Invoker.class);
        when(invoker.getUrl()).thenReturn(url);
        when(invoker.isAvailable()).thenReturn(true);
        return invoker;
    }

    private static final class Source implements ConnectionStateProvider {
        volatile State state;
        final List<Consumer<State>> listeners = new CopyOnWriteArrayList<>();

        Source(State state) {
            this.state = state;
        }

        public State getConnectivityState() {
            return state;
        }

        public AutoCloseable addConnectivityListener(Consumer<State> listener) {
            listeners.add(listener);
            listener.accept(state);
            return () -> listeners.remove(listener);
        }

        void set(State state) {
            this.state = state;
            listeners.forEach(listener -> listener.accept(state));
        }
    }
}
