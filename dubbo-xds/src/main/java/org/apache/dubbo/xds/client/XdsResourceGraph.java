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
package org.apache.dubbo.xds.client;

import org.apache.dubbo.xds.bootstrap.Bootstrapper.BootstrapInfo;
import org.apache.dubbo.xds.client.XdsClient.ResourceWatcher;
import org.apache.dubbo.xds.registry.DiscoveryTarget;
import org.apache.dubbo.xds.traffic.XdsDropPolicy;
import org.apache.dubbo.xds.traffic.XdsFaultFilters;
import org.apache.dubbo.xds.traffic.XdsLoadBalance;
import org.apache.dubbo.xds.traffic.XdsLoadStats;
import org.apache.dubbo.xds.traffic.XdsRingHash;
import org.apache.dubbo.xds.traffic.XdsRouteTable;
import org.apache.dubbo.xds.traffic.XdsTimeouts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

import com.google.protobuf.Message;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.cluster.v3.Cluster;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.ClusterLoadAssignment;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.listener.v3.Listener;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.Route;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RouteConfiguration;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.WeightedCluster;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager;

/** Maintains the LDS -> RDS -> CDS -> EDS dependency graph without creating another ADS client. */
public final class XdsResourceGraph implements AutoCloseable {
    private final XdsClient client;
    private final DiscoveryTarget target;
    private final boolean fixedMetadata;
    private final Consumer<Snapshot> listener;
    private final Consumer<Throwable> errorListener;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final Map<String, AutoCloseable> routeWatches = new LinkedHashMap<>();
    private final Map<String, AutoCloseable> clusterWatches = new LinkedHashMap<>();
    private final Map<String, AutoCloseable> endpointWatches = new LinkedHashMap<>();
    private final Map<String, RouteConfiguration> routes = new LinkedHashMap<>();
    private final Map<String, Cluster> clusters = new LinkedHashMap<>();
    private final Map<String, ClusterLoadAssignment> endpoints = new LinkedHashMap<>();
    private final Set<String> requiredClusters = new LinkedHashSet<>();
    private final Map<String, Integer> retainedClusters = new LinkedHashMap<>();
    private AutoCloseable listenerWatch;
    private Listener outboundListener;
    private volatile Snapshot snapshot;
    private volatile Map<String, Boolean> ringEndpoints = Collections.emptyMap();

    public XdsResourceGraph(
            XdsClient client,
            BootstrapInfo bootstrap,
            DiscoveryTarget target,
            Consumer<Snapshot> listener,
            Consumer<Throwable> errorListener) {
        this(client, bootstrap, target, listener, errorListener, false);
    }

    /** Retains outbound fault/deadline policy but discovers every fixed metadata endpoint from the Service cluster. */
    public static XdsResourceGraph forMetadata(
            XdsClient client,
            BootstrapInfo bootstrap,
            DiscoveryTarget target,
            Consumer<Snapshot> listener,
            Consumer<Throwable> errorListener) {
        return new XdsResourceGraph(client, bootstrap, target, listener, errorListener, true);
    }

    private XdsResourceGraph(
            XdsClient client,
            BootstrapInfo bootstrap,
            DiscoveryTarget target,
            Consumer<Snapshot> listener,
            Consumer<Throwable> errorListener,
            boolean fixedMetadata) {
        this.client = client;
        this.target = target;
        this.fixedMetadata = fixedMetadata;
        this.listener = listener;
        this.errorListener = errorListener;
        snapshot = new Snapshot(this, target, null, routes, clusters, endpoints, requiredClusters);
        synchronized (this) {
            if ("dubbo".equals(target.getProtocol())) {
                // Binary discovery does not depend on Istio's HTTP route generation for a TCP service.
                reconcileClusters(Collections.singleton(target.defaultCluster()));
            } else {
                listenerWatch = watch(
                        XdsResourceType.LDS,
                        bootstrap.clientListenerName(target.authority()),
                        resource -> {
                            outboundListener = resource;
                            reconcileRoutes(resource);
                        },
                        () -> {
                            outboundListener = null;
                            closeAll(routeWatches);
                            routes.clear();
                            reconcileClusters(Collections.emptySet());
                        });
            }
        }
    }

    public Snapshot getSnapshot() {
        return snapshot;
    }

    private void reconcileRoutes(Listener resource) {
        try {
            if (!resource.hasApiListener()
                    || !resource.getApiListener().getApiListener().is(HttpConnectionManager.class)) {
                throw new IllegalArgumentException(
                        "Expected an outbound API listener containing HttpConnectionManager");
            }
            HttpConnectionManager hcm =
                    resource.getApiListener().getApiListener().unpack(HttpConnectionManager.class);
            if (hcm.hasRouteConfig()) {
                closeAll(routeWatches);
                routes.clear();
                routes.put(hcm.getRouteConfig().getName(), hcm.getRouteConfig());
                updateClusters();
            } else if (hcm.hasRds()) {
                String name = hcm.getRds().getRouteConfigName();
                if (name.isEmpty()) {
                    throw new IllegalArgumentException("RDS route name is missing");
                }
                removeWatches(routeWatches, Collections.singleton(name), routes);
                if (!routeWatches.containsKey(name)) {
                    routeWatches.put(
                            name,
                            watch(
                                    XdsResourceType.RDS,
                                    name,
                                    route -> {
                                        routes.put(name, route);
                                        updateClusters();
                                    },
                                    () -> {
                                        routes.remove(name);
                                        updateClusters();
                                    }));
                }
                updateClusters();
            } else {
                throw new IllegalArgumentException("No outbound route configuration");
            }
        } catch (Exception error) {
            errorListener.accept(error);
        }
    }

    private void updateClusters() {
        if (fixedMetadata) {
            reconcileClusters(Collections.singleton(target.defaultCluster()));
            return;
        }
        Set<String> names = new LinkedHashSet<>();
        for (RouteConfiguration config : routes.values()) {
            config.getVirtualHostsList().forEach(host -> {
                for (Route route : host.getRoutesList()) {
                    switch (route.getRoute().getClusterSpecifierCase()) {
                        case CLUSTER:
                            names.add(route.getRoute().getCluster());
                            break;
                        case WEIGHTED_CLUSTERS:
                            for (WeightedCluster.ClusterWeight weight :
                                    route.getRoute().getWeightedClusters().getClustersList()) {
                                names.add(weight.getName());
                            }
                            break;
                        default:
                            throw new IllegalArgumentException("Unsupported outbound route cluster specifier");
                    }
                }
            });
        }
        reconcileClusters(names);
    }

    private void reconcileClusters(Set<String> names) {
        requiredClusters.clear();
        requiredClusters.addAll(names);
        refreshClusters();
    }

    private void refreshClusters() {
        Set<String> names = new LinkedHashSet<>(requiredClusters);
        names.addAll(retainedClusters.keySet());
        removeWatches(clusterWatches, names, clusters);
        for (String name : names) {
            if (!clusterWatches.containsKey(name)) {
                clusterWatches.put(
                        name,
                        watch(
                                XdsResourceType.CDS,
                                name,
                                cluster -> {
                                    clusters.put(name, cluster);
                                    updateEndpoints();
                                },
                                () -> {
                                    clusters.remove(name);
                                    updateEndpoints();
                                }));
            }
        }
        updateEndpoints();
    }

    private void updateEndpoints() {
        Set<String> names = new LinkedHashSet<>();
        Map<String, Boolean> ringPolicies = new LinkedHashMap<>();
        for (Cluster cluster : clusters.values()) {
            if (cluster.getType() != Cluster.DiscoveryType.EDS) {
                errorListener.accept(
                        new IllegalArgumentException("Non-EDS cluster needs an explicit discovery adapter"));
                continue;
            }
            names.add(edsName(cluster));
            if (cluster.getLbPolicy() == Cluster.LbPolicy.RING_HASH) ringPolicies.put(edsName(cluster), true);
        }
        // Validators run under the client lock; publish immutable context without acquiring the graph lock.
        ringEndpoints = Collections.unmodifiableMap(ringPolicies);
        removeWatches(endpointWatches, names, endpoints);
        for (String name : names) {
            if (!endpointWatches.containsKey(name)) {
                endpointWatches.put(
                        name,
                        watch(
                                XdsResourceType.EDS,
                                name,
                                assignment -> {
                                    endpoints.put(name, assignment);
                                    publish();
                                },
                                () -> {
                                    endpoints.remove(name);
                                    publish();
                                }));
            }
        }
        publish();
    }

    public static String edsName(Cluster cluster) {
        String name = cluster.getEdsClusterConfig().getServiceName();
        return name.isEmpty() ? cluster.getName() : name;
    }

    private <T extends Message> AutoCloseable watch(
            XdsResourceType<T> type, String name, Consumer<T> update, Runnable absent) {
        AtomicBoolean active = new AtomicBoolean(true);
        AutoCloseable subscription = client.watch(
                type,
                name,
                new ResourceWatcher<T>() {
                    @Override
                    public void onChanged(T resource) {
                        synchronized (XdsResourceGraph.this) {
                            if (active.get() && !closed.get()) {
                                update.accept(resource);
                            }
                        }
                    }

                    @Override
                    public void onAbsent() {
                        synchronized (XdsResourceGraph.this) {
                            if (active.get() && !closed.get()) {
                                absent.run();
                            }
                        }
                    }

                    @Override
                    public void onError(Throwable error) {
                        if (active.get() && !closed.get()) {
                            errorListener.accept(error);
                        }
                    }
                },
                resource -> {
                    if (type == XdsResourceType.CDS && "tri".equals(target.getProtocol())) {
                        Cluster cluster = (Cluster) resource;
                        XdsLoadBalance.validate(cluster);
                        ClusterLoadAssignment assignment =
                                snapshot.getEndpoints().get(edsName(cluster));
                        if (cluster.getLbPolicy() == Cluster.LbPolicy.RING_HASH && assignment != null) {
                            XdsRingHash.validateEndpoints(assignment);
                        }
                    } else if (type == XdsResourceType.EDS && "tri".equals(target.getProtocol())) {
                        XdsDropPolicy.validate((ClusterLoadAssignment) resource);
                        if (ringEndpoints.containsKey(name))
                            XdsRingHash.validateEndpoints((ClusterLoadAssignment) resource);
                    }
                });
        return () -> {
            active.set(false);
            subscription.close();
        };
    }

    private void publish() {
        snapshot = new Snapshot(this, target, outboundListener, routes, clusters, endpoints, requiredClusters);
        listener.accept(snapshot);
    }

    private synchronized ClusterLease retainCluster(Function<Snapshot, String> selector) {
        if (closed.get()) {
            throw new IllegalStateException("xDS resource graph is closed");
        }
        Snapshot selected = snapshot;
        String name = selector.apply(selected);
        if (!requiredClusters.contains(name)) {
            throw new IllegalArgumentException("Selected cluster is not in the current route configuration");
        }
        retainedClusters.merge(name, 1, Integer::sum);
        return new ClusterLease(this, selected, name);
    }

    private synchronized void releaseCluster(String name) {
        if (closed.get()) {
            return;
        }
        Integer count = retainedClusters.get(name);
        if (count == null) {
            return;
        }
        if (count > 1) {
            retainedClusters.put(name, count - 1);
        } else {
            retainedClusters.remove(name);
            if (!requiredClusters.contains(name)) {
                refreshClusters();
            }
        }
    }

    public synchronized Map<String, Integer> getRetainedClusters() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(retainedClusters));
    }

    private static <T> void removeWatches(Map<String, AutoCloseable> watches, Set<String> keep, Map<String, T> values) {
        for (String name : new ArrayList<>(watches.keySet())) {
            if (!keep.contains(name)) {
                closeWatch(watches.remove(name));
                values.remove(name);
            }
        }
    }

    private static void closeAll(Map<String, AutoCloseable> watches) {
        watches.values().forEach(XdsResourceGraph::closeWatch);
        watches.clear();
    }

    private static void closeWatch(AutoCloseable watch) {
        if (watch != null) {
            try {
                watch.close();
            } catch (Exception error) {
                throw new IllegalStateException("Failed to cancel xDS resource watch", error);
            }
        }
    }

    @Override
    public synchronized void close() {
        if (closed.compareAndSet(false, true)) {
            closeWatch(listenerWatch);
            closeAll(routeWatches);
            closeAll(clusterWatches);
            closeAll(endpointWatches);
            retainedClusters.clear();
        }
    }

    /** Holds CDS/EDS through a call while routing and endpoint/security updates remain independent. */
    public static final class ClusterLease implements AutoCloseable {
        private final XdsResourceGraph graph;
        private final Snapshot selected;
        private final String cluster;
        private final AtomicBoolean closed = new AtomicBoolean();

        private ClusterLease(XdsResourceGraph graph, Snapshot selected, String cluster) {
            this.graph = graph;
            this.selected = selected;
            this.cluster = cluster;
        }

        public Snapshot getSelectedSnapshot() {
            return selected;
        }

        public Snapshot getSnapshot() {
            return closed.get() || graph.closed.get() ? null : graph.getSnapshot();
        }

        public String getCluster() {
            return cluster;
        }

        public void close() {
            if (closed.compareAndSet(false, true)) {
                graph.releaseCluster(cluster);
            }
        }
    }

    public static final class Snapshot {
        private final XdsResourceGraph graph;
        private final DiscoveryTarget target;
        private final Listener listener;
        private final Map<String, RouteConfiguration> routes;
        private final Map<String, Cluster> clusters;
        private final Map<String, ClusterLoadAssignment> endpoints;
        private final Set<String> expectedClusters;
        private final long defaultTimeoutNanos;
        private final XdsFaultFilters faultFilters;
        private final Map<String, XdsRouteTable> routeTables;

        private Snapshot(
                XdsResourceGraph graph,
                DiscoveryTarget target,
                Listener listener,
                Map<String, RouteConfiguration> routes,
                Map<String, Cluster> clusters,
                Map<String, ClusterLoadAssignment> endpoints,
                Set<String> expectedClusters) {
            this.graph = graph;
            this.target = target;
            this.listener = listener;
            try {
                HttpConnectionManager hcm = listener != null && listener.hasApiListener()
                        ? listener.getApiListener().getApiListener().unpack(HttpConnectionManager.class)
                        : null;
                this.defaultTimeoutNanos = hcm == null ? 0 : XdsTimeouts.listener(hcm);
                this.faultFilters = hcm == null || hcm.getHttpFiltersCount() == 0
                        ? XdsFaultFilters.empty()
                        : new XdsFaultFilters(hcm);
            } catch (IOException error) {
                throw new IllegalArgumentException("Invalid outbound listener", error);
            }
            this.routes = immutable(routes);
            Map<String, XdsRouteTable> tables = new LinkedHashMap<>();
            routes.forEach((name, config) -> tables.put(name, new XdsRouteTable(config)));
            this.routeTables = immutable(tables);
            this.clusters = immutable(clusters);
            this.endpoints = immutable(endpoints);
            this.expectedClusters = Collections.unmodifiableSet(new LinkedHashSet<>(expectedClusters));
        }

        private static <T> Map<String, T> immutable(Map<String, T> values) {
            return Collections.unmodifiableMap(new LinkedHashMap<>(values));
        }

        public XdsLoadStats getLoadStats() {
            return graph.client.getLoadStats();
        }

        public DiscoveryTarget getTarget() {
            return target;
        }

        /** The selector reads the latest snapshot and acquires its cluster under the same graph lock. */
        public ClusterLease retainCluster(Function<Snapshot, String> selector) {
            return graph.retainCluster(selector);
        }

        public Listener getListener() {
            return listener;
        }

        public long getDefaultTimeoutNanos() {
            return defaultTimeoutNanos;
        }

        public XdsFaultFilters getFaultFilters() {
            return faultFilters;
        }

        public Map<String, RouteConfiguration> getRoutes() {
            return routes;
        }

        public Map<String, XdsRouteTable> getRouteTables() {
            return routeTables;
        }

        public Map<String, Cluster> getClusters() {
            return clusters;
        }

        public Map<String, ClusterLoadAssignment> getEndpoints() {
            return endpoints;
        }

        public boolean isReady() {
            if (("tri".equals(target.getProtocol()) && (listener == null || routes.isEmpty()))
                    || clusters.isEmpty()
                    || !clusters.keySet().containsAll(expectedClusters)) {
                return false;
            }
            for (Cluster cluster : clusters.values()) {
                if (cluster.getType() != Cluster.DiscoveryType.EDS || !endpoints.containsKey(edsName(cluster))) {
                    return false;
                }
            }
            return true;
        }
    }
}
