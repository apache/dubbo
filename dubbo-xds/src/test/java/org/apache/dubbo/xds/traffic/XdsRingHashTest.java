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
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.xds.client.XdsResourceGraph.Snapshot;
import org.apache.dubbo.xds.registry.DiscoveryTarget;
import org.apache.dubbo.xds.registry.XdsEndpoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import com.google.protobuf.UInt32Value;
import com.google.protobuf.UInt64Value;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.cluster.v3.Cluster;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.cluster.v3.Cluster.RingHashLbConfig;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.Address;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.Locality;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.SocketAddress;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.ClusterLoadAssignment;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.Endpoint;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.LbEndpoint;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.LocalityLbEndpoints;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class XdsRingHashTest {
    private static final Cluster CLUSTER = Cluster.newBuilder()
            .setName("ring")
            .setLbPolicy(Cluster.LbPolicy.RING_HASH)
            .build();

    @Test
    void aSingleRingCombinesLocalityAndEndpointWeightsWithoutRandomLocalitySelection() {
        Snapshot snapshot = snapshot();
        List<Invoker<Object>> invokers =
                Arrays.asList(invoker(snapshot, "a", 3, 0, 17070, 1), invoker(snapshot, "b", 1, 0, 17071, 3));
        XdsRingHash ring =
                XdsRingHash.update(null, invokers, CLUSTER, invokers.get(0).getUrl());
        int first = 0;
        for (int i = 0; i < 8000; i++)
            if (ring.select(invokers, XdsHash64.string("key-" + i)) == invokers.get(0)) first++;
        assertTrue(first >= 3200 && first <= 4800, "Combined weights are 3:3; observed " + first);
        assertEquals(1024, ring.size());
    }

    @Test
    void orderingAndInvokerReplacementKeepTheRingAndEndpointChangesHaveBoundedChurn() {
        Snapshot snapshot = snapshot();
        List<Invoker<Object>> original = Arrays.asList(
                invoker(snapshot, "a", 1, 0, 17070, 1),
                invoker(snapshot, "b", 1, 0, 17071, 1),
                invoker(snapshot, "b", 1, 0, 17072, 1));
        XdsRingHash ring =
                XdsRingHash.update(null, original, CLUSTER, original.get(0).getUrl());
        List<Invoker<Object>> reordered = new ArrayList<>(original);
        Collections.reverse(reordered);
        assertSame(
                ring,
                XdsRingHash.update(ring, reordered, CLUSTER, original.get(0).getUrl()));
        List<Invoker<Object>> grown = new ArrayList<>(original);
        grown.add(invoker(snapshot, "new", 1, 0, 17073, 1));
        XdsRingHash larger =
                XdsRingHash.update(ring, grown, CLUSTER, original.get(0).getUrl());
        int changed = 0;
        for (int i = 0; i < 8000; i++) {
            long hash = XdsHash64.string("sticky-" + i);
            assertSame(ring.select(original, hash), ring.select(reordered, hash));
            if (ring.select(original, hash) != larger.select(grown, hash)) changed++;
        }
        assertTrue(changed > 1000 && changed < 4000, "Adding one of four endpoints changed " + changed);
        List<Invoker<Object>> replacement =
                Arrays.asList(invoker(snapshot, "a", 1, 0, 17070, 1), original.get(1), original.get(2));
        assertSame(
                ring,
                XdsRingHash.update(ring, replacement, CLUSTER, original.get(0).getUrl()));
    }

    @Test
    void aConnectingHashedEndpointBuffersAndFailureUsesItsRingSuccessorThenRecoveryRestoresIt() {
        Snapshot snapshot = snapshot();
        List<Invoker<Object>> all =
                Arrays.asList(invoker(snapshot, "a", 1, 0, 17070, 1), invoker(snapshot, "b", 1, 0, 17071, 1));
        Source first = new Source();
        Source second = new Source();
        URL firstUrl = all.get(0).getUrl().putAttribute(ConnectionStateProvider.ATTRIBUTE, first);
        when(all.get(0).getUrl()).thenReturn(firstUrl);
        URL secondUrl = all.get(1).getUrl().putAttribute(ConnectionStateProvider.ATTRIBUTE, second);
        when(all.get(1).getUrl()).thenReturn(secondUrl);
        assertSame(first, all.get(0).getUrl().getAttribute(ConnectionStateProvider.ATTRIBUTE));
        assertSame(second, all.get(1).getUrl().getAttribute(ConnectionStateProvider.ATTRIBUTE));
        try (XdsLoadBalance lb = new XdsLoadBalance()) {
            Invocation call = invocation(snapshot, XdsHash64.string("alice"));
            Invoker<Object> target = lb.selectReady(all, all.get(0).getUrl(), call);
            assertEquals(
                    Cluster.LbPolicy.RING_HASH,
                    snapshot.getClusters().get("ring").getLbPolicy());
            assertEquals(1, first.listeners.size());
            assertEquals(1, second.listeners.size());
            Source selected = target == all.get(0) ? first : second;
            selected.change(State.CONNECTING);
            assertEquals(State.CONNECTING, XdsPriorityState.state(target));
            assertNull(
                    lb.selectReady(all, all.get(0).getUrl(), call),
                    "Do not rebuild a ring containing only READY endpoints");
            selected.change(State.TRANSIENT_FAILURE);
            assertNotSame(target, lb.selectReady(all, all.get(0).getUrl(), call));
            selected.change(State.READY);
            assertSame(target, lb.selectReady(all, all.get(0).getUrl(), call));
        }
        assertTrue(first.listeners.isEmpty());
        assertTrue(second.listeners.isEmpty());
    }

    @Test
    void ringsRespectTheConfiguredMemoryCapAndInvalidConfigurationsAndWeightsAreRejected() {
        List<Invoker<Object>> all = Arrays.asList(invoker(snapshot(), "a", 1, 0, 17070, 1));
        Cluster large = CLUSTER.toBuilder()
                .setRingHashLbConfig(RingHashLbConfig.newBuilder().setMinimumRingSize(UInt64Value.of(8192)))
                .build();
        XdsLoadBalance.validate(large);
        assertEquals(
                4096, XdsRingHash.update(null, all, large, all.get(0).getUrl()).size());
        assertEquals(
                128,
                XdsRingHash.update(null, all, large, all.get(0).getUrl().addParameter("xds.ring-size-cap", 128))
                        .size());
        for (RingHashLbConfig invalid : Arrays.asList(
                RingHashLbConfig.newBuilder()
                        .setMinimumRingSize(UInt64Value.of(0))
                        .build(),
                RingHashLbConfig.newBuilder()
                        .setMinimumRingSize(UInt64Value.of(3))
                        .setMaximumRingSize(UInt64Value.of(2))
                        .build(),
                RingHashLbConfig.newBuilder()
                        .setMaximumRingSize(UInt64Value.of(8388609))
                        .build(),
                RingHashLbConfig.newBuilder()
                        .setMaximumRingSize(UInt64Value.of(-1))
                        .build(),
                RingHashLbConfig.newBuilder()
                        .setHashFunction(RingHashLbConfig.HashFunction.MURMUR_HASH_2)
                        .build())) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> XdsLoadBalance.validate(
                            CLUSTER.toBuilder().setRingHashLbConfig(invalid).build()));
        }
        LocalityLbEndpoints locality = endpoint(all.get(0)).getLocality();
        assertThrows(
                IllegalArgumentException.class,
                () -> XdsRingHash.validateEndpoints(ClusterLoadAssignment.newBuilder()
                        .addEndpoints(locality.toBuilder()
                                .setLoadBalancingWeight(UInt32Value.of(-1))
                                .setLbEndpoints(
                                        0,
                                        locality.getLbEndpoints(0).toBuilder()
                                                .setLoadBalancingWeight(UInt32Value.of(2))))
                        .build()));
        assertThrows(
                IllegalArgumentException.class,
                () -> XdsRingHash.validateEndpoints(ClusterLoadAssignment.newBuilder()
                        .addEndpoints(locality)
                        .addEndpoints(locality.toBuilder()
                                .setLocality(Locality.newBuilder().setZone("duplicate")))
                        .build()));
        XdsRingHash.validateEndpoints(ClusterLoadAssignment.newBuilder()
                .addEndpoints(locality)
                .addEndpoints(locality.toBuilder().setPriority(1))
                .build());
    }

    @Test
    void twoExplicitRingFailuresPermitPriorityFallbackEvenWhileAnotherEndpointConnects() {
        Snapshot snapshot = snapshot();
        List<Invoker<Object>> all = Arrays.asList(
                invoker(snapshot, "a", 1, 0, 17070, 1),
                invoker(snapshot, "b", 1, 0, 17071, 1),
                invoker(snapshot, "c", 1, 0, 17072, 1),
                invoker(snapshot, "fallback", 1, 1, 17073, 1));
        Source connecting = new Source();
        connecting.change(State.CONNECTING);
        URL connectingUrl = all.get(0).getUrl().putAttribute(ConnectionStateProvider.ATTRIBUTE, connecting);
        when(all.get(0).getUrl()).thenReturn(connectingUrl);
        when(all.get(1).isAvailable()).thenReturn(false);
        when(all.get(2).isAvailable()).thenReturn(false);
        try (XdsLoadBalance lb = new XdsLoadBalance()) {
            assertSame(all.get(3), lb.selectReady(all, all.get(0).getUrl(), invocation(snapshot, 42)));
        }
    }

    static Snapshot snapshot() {
        Snapshot snapshot = mock(Snapshot.class);
        when(snapshot.getTarget()).thenReturn(new DiscoveryTarget("echo.test.svc.cluster.local", 7070, "tri"));
        when(snapshot.getClusters()).thenReturn(Collections.singletonMap(CLUSTER.getName(), CLUSTER));
        return snapshot;
    }

    static Invocation invocation(Snapshot snapshot, long hash) {
        Invocation invocation = mock(Invocation.class);
        when(invocation.get(XdsRouteSelection.ATTRIBUTE))
                .thenReturn(new XdsRouteSelection(
                        snapshot, null, CLUSTER.getName(), Collections.emptyMap(), null, Collections.emptyMap(), hash));
        return invocation;
    }

    @SuppressWarnings("unchecked")
    static Invoker<Object> invoker(
            Snapshot snapshot, String zone, int localityWeight, int priority, int port, int weight) {
        LbEndpoint endpoint = LbEndpoint.newBuilder()
                .setLoadBalancingWeight(UInt32Value.of(weight))
                .setEndpoint(Endpoint.newBuilder()
                        .setAddress(Address.newBuilder()
                                .setSocketAddress(SocketAddress.newBuilder()
                                        .setAddress("127.0.0.1")
                                        .setPortValue(port))))
                .build();
        LocalityLbEndpoints locality = LocalityLbEndpoints.newBuilder()
                .setPriority(priority)
                .setLocality(Locality.newBuilder().setZone(zone))
                .setLoadBalancingWeight(UInt32Value.of(localityWeight))
                .addLbEndpoints(endpoint)
                .build();
        Invoker<Object> invoker = mock(Invoker.class);
        when(invoker.getUrl())
                .thenReturn(URL.valueOf("tri://127.0.0.1:" + port + "/Business")
                        .putAttribute(XdsEndpoint.ATTRIBUTE, new XdsEndpoint(snapshot, CLUSTER, locality, endpoint)));
        when(invoker.isAvailable()).thenReturn(true);
        return invoker;
    }

    private static XdsEndpoint endpoint(Invoker<?> invoker) {
        return (XdsEndpoint) invoker.getUrl().getAttribute(XdsEndpoint.ATTRIBUTE);
    }

    private static final class Source implements ConnectionStateProvider {
        volatile State state = State.READY;
        final List<Consumer<State>> listeners = new CopyOnWriteArrayList<>();

        public State getConnectivityState() {
            return state;
        }

        public AutoCloseable addConnectivityListener(Consumer<State> listener) {
            listeners.add(listener);
            listener.accept(state);
            return () -> listeners.remove(listener);
        }

        void change(State state) {
            this.state = state;
            listeners.forEach(listener -> listener.accept(state));
        }
    }
}
