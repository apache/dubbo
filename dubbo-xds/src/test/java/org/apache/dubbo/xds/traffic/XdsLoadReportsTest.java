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

import org.apache.dubbo.xds.bootstrap.Bootstrapper;
import org.apache.dubbo.xds.bootstrap.Bootstrapper.ServerInfo;

import java.util.Collections;
import java.util.List;

import io.grpc.xds.shaded.io.envoyproxy.envoy.config.cluster.v3.Cluster;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.ConfigSource;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.Locality;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.SelfConfigSource;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.ClusterStats;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class XdsLoadReportsTest {
    private final Cluster cluster = Cluster.newBuilder()
            .setName("cluster")
            .setLrsServer(ConfigSource.newBuilder().setSelf(SelfConfigSource.getDefaultInstance()))
            .build();
    private final Locality locality =
            Locality.newBuilder().setRegion("region").setZone("zone").build();

    @Test
    void checkpointRetainsEventsBetweenCollectionAndHandoffAndFailedSendsDoNotReset() throws Exception {
        ServerInfo server = servers().get(0);
        XdsLoadReports reports = new XdsLoadReports();
        reports.accepted(cluster, server);
        XdsLoadReports.Attempt first = reports.started(cluster, locality);
        reports.dropped(cluster, "throttle");
        XdsLoadReports.Batch failedSend = reports.prepare(server, Collections.emptySet(), true);
        assertEquals(1, failedSend.getStats().get(0).getTotalDroppedRequests());
        XdsLoadReports.Batch sent = reports.prepare(server, Collections.emptySet(), true);
        assertEquals(1, sent.getStats().get(0).getUpstreamLocalityStats(0).getTotalIssuedRequests());
        first.finished(null);
        first.finished(new IllegalStateException());
        reports.started(cluster, locality).finished(new IllegalStateException());
        reports.dropped(cluster, "after-collection");
        sent.commit();
        sent.commit();
        XdsLoadReports.Batch next = reports.prepare(server, Collections.emptySet(), true);
        ClusterStats delta = next.getStats().get(0);
        assertEquals(1, delta.getUpstreamLocalityStats(0).getTotalIssuedRequests());
        assertEquals(1, delta.getUpstreamLocalityStats(0).getTotalSuccessfulRequests());
        assertEquals(1, delta.getUpstreamLocalityStats(0).getTotalErrorRequests());
        assertEquals(0, delta.getUpstreamLocalityStats(0).getTotalRequestsInProgress());
        assertEquals(1, delta.getTotalDroppedRequests());
        assertEquals("after-collection", delta.getDroppedRequests(0).getCategory());
        next.commit();
        assertEquals(
                0,
                reports.prepare(server, Collections.emptySet(), true)
                        .getStats()
                        .get(0)
                        .getUpstreamLocalityStatsCount());
    }

    @Test
    void controlPlaneOriginsKeepInflightResultsAndPendingReportsSeparate() throws Exception {
        List<ServerInfo> servers = servers();
        XdsLoadReports reports = new XdsLoadReports();
        reports.accepted(cluster, servers.get(0));
        XdsLoadReports.Attempt held = reports.started(cluster, locality);
        XdsLoadReports.Batch first = reports.prepare(servers.get(0), Collections.emptySet(), true);
        first.commit();
        reports.accepted(cluster, servers.get(1));
        reports.started(cluster, locality).finished(new IllegalStateException());
        held.finished(null);
        XdsLoadReports.Batch old = reports.prepare(servers.get(0), Collections.emptySet(), true);
        assertEquals(0, old.getStats().get(0).getUpstreamLocalityStats(0).getTotalIssuedRequests());
        assertEquals(1, old.getStats().get(0).getUpstreamLocalityStats(0).getTotalSuccessfulRequests());
        old.commit();
        assertEquals(Collections.singleton(servers.get(1)), reports.servers());
        ClusterStats current = reports.prepare(servers.get(1), Collections.emptySet(), true)
                .getStats()
                .get(0);
        assertEquals(1, current.getUpstreamLocalityStats(0).getTotalIssuedRequests());
        assertEquals(1, current.getUpstreamLocalityStats(0).getTotalErrorRequests());
        assertEquals(0, current.getUpstreamLocalityStats(0).getTotalSuccessfulRequests());
    }

    @Test
    void filtersAndEdsServiceChangesDoNotDiscardUnrequestedOrInflightCounters() throws Exception {
        ServerInfo server = servers().get(0);
        XdsLoadReports reports = new XdsLoadReports();
        reports.accepted(cluster, server);
        XdsLoadReports.Attempt held = reports.started(cluster, locality);
        Cluster changed = cluster.toBuilder()
                .setEdsClusterConfig(Cluster.EdsClusterConfig.newBuilder().setServiceName("new-eds"))
                .build();
        reports.accepted(changed, server);
        reports.started(changed, locality).finished(null);
        XdsLoadReports.Batch filtered = reports.prepare(server, Collections.singleton("other"), false);
        assertTrue(filtered.getStats().isEmpty());
        filtered.commit();
        held.finished(new IllegalStateException());
        XdsLoadReports.Batch all = reports.prepare(server, Collections.emptySet(), true);
        assertEquals(2, all.getStats().size());
        assertEquals(
                1,
                all.getStats().stream()
                        .mapToLong(s -> s.getUpstreamLocalityStats(0).getTotalErrorRequests())
                        .sum());
        assertEquals(
                1,
                all.getStats().stream()
                        .mapToLong(s -> s.getUpstreamLocalityStats(0).getTotalSuccessfulRequests())
                        .sum());
        reports.retired("cluster");
        all.commit();
        assertTrue(reports.servers().isEmpty());
    }

    @Test
    void absentConfigurationHasNoReportingAndDiagnosticsRemainCumulative() throws Exception {
        ServerInfo server = servers().get(0);
        XdsLoadStats stats = new XdsLoadStats();
        Cluster disabled = cluster.toBuilder().clearLrsServer().build();
        stats.accepted(disabled, server);
        stats.started(disabled, locality).finished(null);
        assertTrue(stats.getReports().servers().isEmpty());
        stats.accepted(cluster, server);
        stats.started(cluster, locality).finished(null);
        stats.circuitDropped(cluster);
        stats.edsDropped(cluster, "overload");
        XdsLoadReports.Batch batch = stats.getReports().prepare(server, Collections.emptySet(), true);
        assertEquals(1, batch.getStats().get(0).getUpstreamLocalityStats(0).getTotalSuccessfulRequests());
        assertEquals(2, batch.getStats().get(0).getTotalDroppedRequests());
        assertEquals(1, batch.getStats().get(0).getDroppedRequests(0).getDroppedCount());
        batch.commit();
        assertEquals(2, stats.snapshot().get(0).getSuccessful());
        stats.close();
        assertTrue(stats.getReports().servers().isEmpty());
        assertTrue(stats.snapshot().isEmpty());
    }

    private static List<ServerInfo> servers() throws Exception {
        return Bootstrapper.parse(
                        "{\"xds_servers\":[{\"server_uri\":\"localhost:17070\",\"channel_creds\":[{\"type\":\"insecure\"}]},{\"server_uri\":\"localhost:17071\",\"channel_creds\":[{\"type\":\"insecure\"}]}],\"node\":{\"id\":\"test\"}}")
                .getServers();
    }
}
