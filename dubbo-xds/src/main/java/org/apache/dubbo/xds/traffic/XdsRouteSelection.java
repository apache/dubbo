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

import org.apache.dubbo.xds.client.XdsResourceGraph.ClusterLease;
import org.apache.dubbo.xds.client.XdsResourceGraph.Snapshot;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/** One call's route/cluster choice, also retained for retries without another weighted selection. */
public final class XdsRouteSelection implements AutoCloseable {
    public static final String ATTRIBUTE = "dubbo.xds.route-selection";
    private final Snapshot snapshot;
    private final XdsRouteTable.Match route;
    private final String cluster;
    private final Map<String, String> headers;
    private final Map<String, String> faultHeaders;
    private final ClusterLease lease;
    private final long requestHash;

    XdsRouteSelection(Snapshot snapshot, XdsRouteTable.Match route, String cluster, Map<String, String> headers) {
        this(snapshot, route, cluster, headers, null);
    }

    XdsRouteSelection(
            Snapshot snapshot,
            XdsRouteTable.Match route,
            String cluster,
            Map<String, String> headers,
            ClusterLease lease) {
        this(snapshot, route, cluster, headers, lease, headers);
    }

    XdsRouteSelection(
            Snapshot snapshot,
            XdsRouteTable.Match route,
            String cluster,
            Map<String, String> headers,
            ClusterLease lease,
            Map<String, String> faultHeaders) {
        this(
                snapshot,
                route,
                cluster,
                headers,
                lease,
                faultHeaders,
                ThreadLocalRandom.current().nextLong());
    }

    XdsRouteSelection(
            Snapshot snapshot,
            XdsRouteTable.Match route,
            String cluster,
            Map<String, String> headers,
            ClusterLease lease,
            Map<String, String> faultHeaders,
            long requestHash) {
        this.snapshot = snapshot;
        this.route = route;
        this.cluster = cluster;
        this.headers = Collections.unmodifiableMap(new LinkedHashMap<>(headers));
        this.faultHeaders = Collections.unmodifiableMap(new LinkedHashMap<>(faultHeaders));
        this.lease = lease;
        this.requestHash = requestHash;
    }

    public Snapshot getSnapshot() {
        return snapshot;
    }

    public XdsRouteTable.Match getRoute() {
        return route;
    }

    public String getCluster() {
        return cluster;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Snapshot getCurrentSnapshot() {
        return lease == null ? snapshot : lease.getSnapshot();
    }

    public Map<String, String> getFaultHeaders() {
        return faultHeaders;
    }

    public long getRequestHash() {
        return requestHash;
    }

    XdsRouteSelection updated(Snapshot current) {
        return new XdsRouteSelection(current, route, cluster, headers, lease, faultHeaders, requestHash);
    }

    public void close() {
        if (lease != null) {
            lease.close();
        }
    }
}
