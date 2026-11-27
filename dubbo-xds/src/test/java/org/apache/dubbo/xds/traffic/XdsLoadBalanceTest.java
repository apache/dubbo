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
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.xds.client.XdsResourceGraph.Snapshot;
import org.apache.dubbo.xds.registry.DiscoveryTarget;
import org.apache.dubbo.xds.registry.XdsEndpoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class XdsLoadBalanceTest {
    private static final Cluster CLUSTER =
            Cluster.newBuilder().setName("cluster").build();

    @Test
    void endpointWeightsUseTheirLogicalIdentityAndRemovalStopsSelection() {
        Snapshot snapshot = snapshot();
        Invocation invocation = invocation(snapshot);
        Invoker<Object> first = invoker(snapshot, "zone", 1, 0, 17070, 1);
        Invoker<Object> second = invoker(snapshot, "zone", 1, 0, 17071, 3);
        XdsLoadBalance lb = new XdsLoadBalance();
        int selectedFirst = 0;
        for (int i = 0; i < 100; i++) {
            if (lb.select(Arrays.asList(first, second), first.getUrl(), invocation) == first) {
                selectedFirst++;
            }
        }
        assertEquals(25, selectedFirst);
        for (int i = 0; i < 30; i++) {
            assertSame(second, lb.select(Collections.singletonList(second), second.getUrl(), invocation));
        }
    }

    @Test
    void localityWeightsAreIndependentOfTheirEndpointCountsAndUnavailablePriorityCanFailOver() {
        Snapshot snapshot = snapshot();
        Invocation invocation = invocation(snapshot);
        Invoker<Object> first = invoker(snapshot, "small", 1, 0, 17070, 1);
        Invoker<Object> second = invoker(snapshot, "large", 1, 0, 17071, 1);
        Invoker<Object> third = invoker(snapshot, "large", 1, 0, 17072, 1);
        List<Invoker<Object>> candidates = Arrays.asList(first, second, third);
        XdsLoadBalance lb = new XdsLoadBalance();
        int small = 0;
        for (int i = 0; i < 1000; i++) {
            if (lb.select(candidates, first.getUrl(), invocation) == first) {
                small++;
            }
        }
        assertTrue(small >= 400 && small <= 600, "Observed small locality requests: " + small);
        when(first.isAvailable()).thenReturn(false);
        when(second.isAvailable()).thenReturn(false);
        when(third.isAvailable()).thenReturn(false);
        Invoker<Object> fallback = invoker(snapshot, "fallback", 1, 1, 17073, 1);
        assertSame(fallback, lb.select(Arrays.asList(first, second, third, fallback), first.getUrl(), invocation));
    }

    @Test
    void zeroWeightsDoNotBlockAnEligibleLowerPriorityOrAnotherLocality() {
        Snapshot snapshot = snapshot();
        Invocation invocation = invocation(snapshot);
        Invoker<Object> zeroLocality = invoker(snapshot, "disabled", 0, 0, 17070, 1);
        Invoker<Object> zeroEndpoint = invoker(snapshot, "empty", 1, 0, 17071, 0);
        Invoker<Object> fallback = invoker(snapshot, "fallback", 1, 1, 17072, 1);
        XdsLoadBalance lb = new XdsLoadBalance();
        List<Invoker<Object>> candidates = Arrays.asList(zeroLocality, zeroEndpoint, fallback);
        for (int n = 0; n < 20; n++) assertSame(fallback, lb.select(candidates, fallback.getUrl(), invocation));
        Invoker<Object> recovered = invoker(snapshot, "recovered", 1, 0, 17073, 1);
        candidates = Arrays.asList(zeroLocality, zeroEndpoint, recovered, fallback);
        for (int n = 0; n < 20; n++) assertSame(recovered, lb.select(candidates, recovered.getUrl(), invocation));
        assertThrows(
                RuntimeException.class,
                () -> lb.select(Arrays.asList(zeroLocality, zeroEndpoint), fallback.getUrl(), invocation));
    }

    @Test
    void leastRequestIgnoresZeroEndpointWeightsButStillHonorsLocalityWeights() {
        Snapshot snapshot = snapshot(leastRequest(2));
        Invoker<Object> zeroEndpoint = invoker(snapshot, "primary", 1, 0, 17070, 0);
        Invoker<Object> zeroLocality = invoker(snapshot, "disabled", 0, 0, 17071, 1);
        Invoker<Object> fallback = invoker(snapshot, "fallback", 1, 1, 17072, 1);
        XdsLoadBalance lb = new XdsLoadBalance();
        for (int n = 0; n < 20; n++)
            assertSame(
                    zeroEndpoint,
                    lb.select(
                            Arrays.asList(zeroLocality, zeroEndpoint, fallback),
                            zeroEndpoint.getUrl(),
                            invocation(snapshot)));
    }

    @Test
    void leastRequestUsesActiveRequestsInsteadOfEndpointWeightOrDuplicateAddresses() {
        Snapshot snapshot = snapshot(leastRequest(2));
        Invocation invocation = invocation(snapshot);
        Invoker<Object> first = invoker(snapshot, "zone", 1, 0, 17070, 1000);
        Invoker<Object> second = invoker(snapshot, "zone", 1, 0, 17071, 1);
        Invoker<Object> duplicate = invoker(snapshot, "zone", 1, 0, 17070, 1000);
        List<Invoker<Object>> candidates = Arrays.asList(first, second, duplicate);
        XdsLoadBalance lb = new XdsLoadBalance();
        try (XdsLoadBalance.Permit permit = lb.acquire(first)) {
            int busy = 0;
            for (int i = 0; i < 2000; i++) {
                if (lb.select(candidates, first.getUrl(), invocation) != second) busy++;
            }
            assertTrue(busy > 370 && busy < 630, "Busy endpoint must receive only duplicate samples: " + busy);
            assertEquals(1, lb.active(endpoint(first)));
        }
        int idleFirst = 0;
        for (int i = 0; i < 2000; i++) {
            if (lb.select(candidates, first.getUrl(), invocation) != second) idleFirst++;
        }
        assertTrue(idleFirst > 800 && idleFirst < 1200, "Equal idle endpoints: " + idleFirst);
        assertEquals(0, lb.active(endpoint(first)));
    }

    @Test
    void outstandingCallsSurvivePolicyChangesAndCountersAreLocalToEachLb() {
        Snapshot rr = snapshot();
        Snapshot least = snapshot(leastRequest(10));
        Invoker<Object> original = invoker(rr, "zone", 1, 0, 17070, 1);
        Invoker<Object> rebuilt = invoker(least, "zone", 1, 0, 17070, 30);
        Invoker<Object> other = invoker(least, "zone", 1, 0, 17071, 1);
        XdsLoadBalance lb = new XdsLoadBalance();
        XdsLoadBalance independent = new XdsLoadBalance();
        XdsLoadBalance.Permit first = lb.acquire(original);
        assertEquals(1, lb.active(endpoint(rebuilt)));
        assertEquals(0, independent.active(endpoint(rebuilt)));
        for (int i = 0; i < 20; i++) {
            assertSame(other, lb.select(Collections.singletonList(other), other.getUrl(), invocation(least)));
        }
        assertEquals(1, lb.active(endpoint(original)), "Removal cannot erase an outstanding permit");
        first.close();
        first.close();
        assertEquals(0, lb.active(endpoint(original)));
        XdsLoadBalance.Permit second = lb.acquire(rebuilt);
        lb.close();
        assertEquals(1, lb.active(endpoint(rebuilt)), "Close does not fake transport completion");
        assertThrows(RuntimeException.class, () -> lb.acquire(rebuilt));
        second.close();
        assertEquals(0, lb.active(endpoint(rebuilt)));
    }

    @Test
    void racingAcquisitionAndReleaseNeverLoseAnOutstandingCounter() throws Exception {
        Snapshot snapshot = snapshot();
        Invoker<Object> invoker = invoker(snapshot, "zone", 1, 0, 17070, 1);
        XdsLoadBalance lb = new XdsLoadBalance();
        ExecutorService workers = Executors.newFixedThreadPool(8);
        try (XdsLoadBalance.Permit held = lb.acquire(invoker)) {
            List<Future<?>> completed = new ArrayList<>();
            for (int i = 0; i < 8; i++)
                completed.add(workers.submit(() -> {
                    for (int n = 0; n < 300; n++) {
                        try (XdsLoadBalance.Permit permit = lb.acquire(invoker)) {
                            assertTrue(lb.active(endpoint(invoker)) >= 2);
                        }
                    }
                }));
            for (Future<?> future : completed) future.get(3, TimeUnit.SECONDS);
            assertEquals(1, lb.active(endpoint(invoker)));
        } finally {
            workers.shutdownNow();
            assertTrue(workers.awaitTermination(3, TimeUnit.SECONDS));
        }
        assertEquals(0, lb.active(endpoint(invoker)));
    }

    @Test
    void leastRequestValidatesUnsignedChoiceCountAndCapsSamplingWork() {
        assertEquals(
                2,
                XdsLoadBalance.choiceCount(CLUSTER.toBuilder()
                        .setLbPolicy(Cluster.LbPolicy.LEAST_REQUEST)
                        .build()));
        assertEquals(10, XdsLoadBalance.choiceCount(leastRequest(-1)));
        assertEquals(10, XdsLoadBalance.choiceCount(leastRequest(11)));
        for (int invalid : new int[] {0, 1})
            assertThrows(IllegalArgumentException.class, () -> XdsLoadBalance.validate(leastRequest(invalid)));
        assertThrows(
                IllegalArgumentException.class,
                () -> XdsLoadBalance.validate(
                        CLUSTER.toBuilder().setLbPolicy(Cluster.LbPolicy.MAGLEV).build()));
    }

    private static Cluster leastRequest(int choices) {
        return CLUSTER.toBuilder()
                .setLbPolicy(Cluster.LbPolicy.LEAST_REQUEST)
                .setLeastRequestLbConfig(
                        Cluster.LeastRequestLbConfig.newBuilder().setChoiceCount(UInt32Value.of(choices)))
                .build();
    }

    private static XdsEndpoint endpoint(Invoker<?> invoker) {
        return (XdsEndpoint) invoker.getUrl().getAttribute(XdsEndpoint.ATTRIBUTE);
    }

    private static Snapshot snapshot() {
        return snapshot(CLUSTER);
    }

    private static Snapshot snapshot(Cluster cluster) {
        Snapshot snapshot = mock(Snapshot.class);
        when(snapshot.getTarget()).thenReturn(new DiscoveryTarget("echo.test.svc.cluster.local", 7070, "tri"));
        when(snapshot.getClusters()).thenReturn(Collections.singletonMap(cluster.getName(), cluster));
        return snapshot;
    }

    private static Invocation invocation(Snapshot snapshot) {
        Invocation invocation = mock(Invocation.class);
        when(invocation.get(XdsRouteSelection.ATTRIBUTE))
                .thenReturn(new XdsRouteSelection(snapshot, null, CLUSTER.getName(), Collections.emptyMap()));
        return invocation;
    }

    @SuppressWarnings("unchecked")
    private static Invoker<Object> invoker(
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
        URL url = URL.valueOf("tri://127.0.0.1:" + port + "/Business")
                .putAttribute(XdsEndpoint.ATTRIBUTE, new XdsEndpoint(snapshot, CLUSTER, locality, endpoint));
        Invoker<Object> invoker = mock(Invoker.class);
        when(invoker.getUrl()).thenReturn(url);
        when(invoker.isAvailable()).thenReturn(true);
        return invoker;
    }
}
