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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.grpc.xds.shaded.io.envoyproxy.envoy.config.cluster.v3.Cluster;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class XdsLoadStatsTest {
    private final Cluster cluster = Cluster.newBuilder().setName("business").build();

    @Test
    void cumulativeDropsSurviveIdleAndSnapshotsDoNotResetCounters() {
        XdsLoadStats stats = new XdsLoadStats();
        stats.accepted(cluster);
        stats.circuitDropped(cluster);
        stats.edsDropped(cluster);
        XdsLoadStats.Attempt call = stats.started(cluster);
        call.finished(null);
        call.finished(new IllegalStateException());
        XdsLoadStats.Snapshot value = stats.snapshot().get(0);
        assertEquals(1, value.getIssued());
        assertEquals(1, value.getSuccessful());
        assertEquals(0, value.getErrors());
        assertEquals(0, value.getInProgress());
        assertEquals(1, value.getCircuitDrops());
        assertEquals(1, value.getEdsDrops());
        assertEquals(1, stats.snapshot().get(0).getSuccessful());
        assertThrows(UnsupportedOperationException.class, () -> stats.snapshot().clear());
        stats.close();
        stats.circuitDropped(cluster);
        stats.started(cluster).finished(null);
        assertTrue(stats.snapshot().isEmpty());
    }

    @Test
    void concurrentCompletionAndCancellationHaveOneTerminalOutcome() {
        XdsLoadStats stats = new XdsLoadStats();
        List<CompletableFuture<Void>> work = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            XdsLoadStats.Attempt call = stats.started(cluster);
            work.add(CompletableFuture.runAsync(() -> call.finished(null)));
            work.add(CompletableFuture.runAsync(() -> call.finished(new IllegalStateException())));
        }
        CompletableFuture.allOf(work.toArray(new CompletableFuture[0])).join();
        XdsLoadStats.Snapshot value = stats.snapshot().get(0);
        assertEquals(100, value.getIssued());
        assertEquals(100, value.getSuccessful() + value.getErrors());
        assertEquals(0, value.getInProgress());
    }

    @Test
    void retirementKeepsInflightCountersAndBoundsIdleHistory() {
        XdsLoadStats stats = new XdsLoadStats();
        stats.accepted(cluster);
        XdsLoadStats.Attempt call = stats.started(cluster);
        stats.retired(cluster.getName());
        for (int i = 0; i < 200; i++) {
            Cluster temporary = Cluster.newBuilder().setName("retired-" + i).build();
            stats.accepted(temporary);
            stats.circuitDropped(temporary);
            stats.retired(temporary.getName());
        }
        assertEquals(129, stats.snapshot().size());
        assertEquals(
                1, stats.snapshot().stream().filter(s -> s.getInProgress() == 1).count());
        call.finished(null);
        assertEquals(128, stats.snapshot().size());
        stats.accepted(cluster);
        assertTrue(stats.snapshot().stream().anyMatch(s -> s.getCluster().equals("business") && s.isSubscribed()));
    }
}
