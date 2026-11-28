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

import org.apache.dubbo.xds.traffic.XdsCircuitBreaker.Permit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.protobuf.UInt32Value;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.cluster.v3.CircuitBreakers;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.cluster.v3.Cluster;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.RoutingPriority;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class XdsCircuitBreakerTest {
    @Test
    void defaultPriorityUnsignedLimitAndZeroAreEnforced() {
        Cluster bare = Cluster.newBuilder().setName("limits").build();
        assertEquals(1024, XdsCircuitBreaker.limit(bare));
        Cluster limits = bare.toBuilder()
                .setCircuitBreakers(CircuitBreakers.newBuilder()
                        .addThresholds(CircuitBreakers.Thresholds.newBuilder()
                                .setPriority(RoutingPriority.HIGH)
                                .setMaxRequests(UInt32Value.of(9)))
                        .addThresholds(CircuitBreakers.Thresholds.newBuilder().setMaxRequests(UInt32Value.of(-1)))
                        .addThresholds(CircuitBreakers.Thresholds.newBuilder().setMaxRequests(UInt32Value.of(1))))
                .build();
        assertEquals(4294967295L, XdsCircuitBreaker.limit(limits));
        assertThrows(XdsCircuitBreaker.DropException.class, () -> XdsCircuitBreaker.acquire("zero", cluster(0, "eds")));
        assertEquals(0, XdsCircuitBreaker.activeCounterCount());
    }

    @Test
    void lowerLimitsKeepOutstandingCallsAndWorkloadsAndEdsNamesAreIndependent() {
        Cluster initial = cluster(2, "eds");
        Permit first = XdsCircuitBreaker.acquire("workload", initial);
        Permit second = XdsCircuitBreaker.acquire("workload", initial);
        try (Permit other = XdsCircuitBreaker.acquire("other", cluster(1, "eds"));
                Permit changed = XdsCircuitBreaker.acquire("workload", cluster(1, "new-eds"))) {
            assertThrows(
                    XdsCircuitBreaker.DropException.class,
                    () -> XdsCircuitBreaker.acquire("workload", cluster(1, "eds")));
            first.close();
            first.close();
            assertEquals(1, XdsCircuitBreaker.active("workload", initial));
            assertThrows(
                    XdsCircuitBreaker.DropException.class,
                    () -> XdsCircuitBreaker.acquire("workload", cluster(1, "eds")));
            second.close();
            try (Permit next = XdsCircuitBreaker.acquire("workload", cluster(1, "eds"))) {
                assertEquals(1, XdsCircuitBreaker.active("workload", initial));
            }
        } finally {
            first.close();
            second.close();
        }
        assertEquals(0, XdsCircuitBreaker.activeCounterCount());
    }

    @Test
    void concurrentAdmissionNeverExceedsTheClusterLimitAndReclaimsItsCounter() throws Exception {
        Cluster cluster = cluster(4, "concurrent");
        ExecutorService executor = Executors.newFixedThreadPool(16);
        CountDownLatch picked = new CountDownLatch(16);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger admitted = new AtomicInteger();
        AtomicInteger dropped = new AtomicInteger();
        List<Future<?>> calls = new ArrayList<>();
        try {
            for (int i = 0; i < 16; i++)
                calls.add(executor.submit(() -> {
                    Permit permit = null;
                    try {
                        permit = XdsCircuitBreaker.acquire("concurrent", cluster);
                        admitted.incrementAndGet();
                    } catch (XdsCircuitBreaker.DropException error) {
                        dropped.incrementAndGet();
                    } finally {
                        picked.countDown();
                    }
                    if (permit != null) {
                        try {
                            release.await();
                        } catch (InterruptedException error) {
                            Thread.currentThread().interrupt();
                        } finally {
                            permit.close();
                        }
                    }
                }));
            assertTrue(picked.await(2, TimeUnit.SECONDS));
            assertEquals(4, admitted.get());
            assertEquals(12, dropped.get());
            assertEquals(4, XdsCircuitBreaker.active("concurrent", cluster));
            assertEquals(12, XdsCircuitBreaker.dropped("concurrent", cluster));
            release.countDown();
            for (Future<?> call : calls) call.get(2, TimeUnit.SECONDS);
            assertEquals(0, XdsCircuitBreaker.activeCounterCount());
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    private static Cluster cluster(int maximum, String eds) {
        return Cluster.newBuilder()
                .setName("shared")
                .setEdsClusterConfig(Cluster.EdsClusterConfig.newBuilder().setServiceName(eds))
                .setCircuitBreakers(CircuitBreakers.newBuilder()
                        .addThresholds(CircuitBreakers.Thresholds.newBuilder().setMaxRequests(UInt32Value.of(maximum))))
                .build();
    }
}
