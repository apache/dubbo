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

import org.apache.dubbo.xds.bootstrap.Bootstrapper.ServerInfo;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.protobuf.util.Durations;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.cluster.v3.Cluster;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.Locality;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.ClusterStats;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.UpstreamLocalityStats;

/** Locality interval counters are separate from cumulative diagnostics and isolated by CDS origin. */
public final class XdsLoadReports implements AutoCloseable {
    private final Map<Key, Counter> counters = new LinkedHashMap<>();
    private final Map<String, Counter> current = new LinkedHashMap<>();
    private final ReferenceQueue<Cluster> releasedResources = new ReferenceQueue<>();
    private final Map<ResourceIdentity, Counter> contexts = new HashMap<>();
    private Runnable changeListener = () -> {};
    private boolean closed;

    public static void validate(Cluster cluster) {
        if (cluster.hasLrsServer() && !cluster.getLrsServer().hasSelf()) {
            throw new UnsupportedLrsServerException();
        }
    }

    public static final class UnsupportedLrsServerException extends IllegalArgumentException {
        private static final long serialVersionUID = 1L;

        private UnsupportedLrsServerException() {
            super("Native xDS LRS requires SELF");
        }
    }

    public synchronized void setChangeListener(Runnable listener) {
        changeListener = listener;
    }

    public synchronized void accepted(Cluster cluster, ServerInfo origin) {
        if (closed) return;
        validate(cluster);
        Counter old = current.remove(cluster.getName());
        if (old != null) old.subscribed = false;
        if (cluster.hasLrsServer()) {
            Key key = new Key(origin, cluster);
            Counter value = counters.computeIfAbsent(key, unused -> new Counter(key, origin));
            value.subscribed = true;
            current.put(cluster.getName(), value);
            contexts.put(new ResourceIdentity(cluster, releasedResources), value);
        }
        prune();
        changeListener.run();
    }

    public synchronized void retired(String cluster) {
        Counter value = current.remove(cluster);
        if (value != null) value.subscribed = false;
        prune();
        changeListener.run();
    }

    private Counter current(Cluster cluster) {
        releaseContexts();
        Counter context = contexts.get(new ResourceIdentity(cluster, null));
        if (context == null) return null;
        Counter value = counters.get(context.key);
        if (value == null) {
            counters.put(context.key, context);
            changeListener.run();
            value = context;
        }
        return value;
    }

    public synchronized Attempt started(Cluster cluster, Locality locality) {
        Counter value = closed ? null : current(cluster);
        LocalityCounter load =
                value == null ? null : value.localities.computeIfAbsent(locality, unused -> new LocalityCounter());
        if (load != null) {
            value.inProgress++;
            load.inProgress++;
            load.issued++;
        }
        return new Attempt(value, load);
    }

    /** Empty category is an uncategorized admission drop, never an invented EDS category. */
    public synchronized void dropped(Cluster cluster, String category) {
        Counter value = closed ? null : current(cluster);
        if (value != null) {
            value.dropped++;
            if (!category.isEmpty()) value.categories.merge(category, 1L, Long::sum);
        }
    }

    public synchronized Set<ServerInfo> servers() {
        Set<ServerInfo> result = new LinkedHashSet<>();
        counters.values().forEach(value -> result.add(value.origin));
        return Collections.unmodifiableSet(result);
    }

    /** Prepare without resetting: a synchronous send failure must leave the window pending. */
    public synchronized Batch prepare(ServerInfo server, Set<String> requested, boolean all) {
        List<Window> windows = new ArrayList<>();
        List<ClusterStats> stats = new ArrayList<>();
        long now = System.nanoTime();
        for (Counter value : counters.values()) {
            if (!value.key.server.equals(server) || (!all && !requested.contains(value.key.cluster))) continue;
            Window window = new Window(value, now);
            windows.add(window);
            ClusterStats.Builder report = ClusterStats.newBuilder()
                    .setClusterName(value.key.cluster)
                    .setClusterServiceName(value.key.service)
                    .setTotalDroppedRequests(value.dropped - value.reportedDropped)
                    .setLoadReportInterval(Durations.fromNanos(Math.max(0, now - value.since)));
            value.categories.forEach((category, count) -> {
                long delta = count - value.reportedCategories.getOrDefault(category, 0L);
                if (delta > 0)
                    report.addDroppedRequests(ClusterStats.DroppedRequests.newBuilder()
                            .setCategory(category)
                            .setDroppedCount(delta));
            });
            value.localities.forEach(
                    (locality, count) -> report.addUpstreamLocalityStats(UpstreamLocalityStats.newBuilder()
                            .setLocality(locality)
                            .setTotalIssuedRequests(count.issued - count.reportedIssued)
                            .setTotalSuccessfulRequests(count.successful - count.reportedSuccessful)
                            .setTotalErrorRequests(count.errors - count.reportedErrors)
                            .setTotalRequestsInProgress(count.inProgress)));
            stats.add(report.build());
        }
        return new Batch(Collections.unmodifiableList(stats), windows);
    }

    private void releaseContexts() {
        ResourceIdentity identity;
        while ((identity = (ResourceIdentity) releasedResources.poll()) != null) contexts.remove(identity);
    }

    private void prune() {
        releaseContexts();
        counters.values().removeIf(value -> !value.subscribed && value.drained());
    }

    @Override
    public synchronized void close() {
        closed = true;
        changeListener = () -> {};
        current.clear();
        contexts.clear();
        counters.clear();
    }

    public final class Attempt {
        private final Counter counter;
        private final LocalityCounter locality;
        private final AtomicBoolean finished = new AtomicBoolean();

        private Attempt(Counter counter, LocalityCounter locality) {
            this.counter = counter;
            this.locality = locality;
        }

        public void finished(Throwable error) {
            if (counter != null && finished.compareAndSet(false, true)) {
                synchronized (XdsLoadReports.this) {
                    counter.inProgress--;
                    locality.inProgress--;
                    if (error == null) locality.successful++;
                    else locality.errors++;
                }
            }
        }
    }

    public final class Batch {
        private final List<ClusterStats> stats;
        private final List<Window> windows;
        private final AtomicBoolean committed = new AtomicBoolean();

        private Batch(List<ClusterStats> stats, List<Window> windows) {
            this.stats = stats;
            this.windows = windows;
        }

        public List<ClusterStats> getStats() {
            return stats;
        }

        public void commit() {
            if (!committed.compareAndSet(false, true)) return;
            synchronized (XdsLoadReports.this) {
                if (closed) return;
                for (Window window : windows) {
                    Counter value = window.counter;
                    if (value.version != window.version) continue;
                    value.version++;
                    value.since = window.end;
                    value.reportedDropped = window.dropped;
                    value.reportedCategories.putAll(window.categories);
                    Iterator<Map.Entry<String, Long>> categories =
                            value.categories.entrySet().iterator();
                    while (categories.hasNext()) {
                        Map.Entry<String, Long> entry = categories.next();
                        if (entry.getValue().equals(value.reportedCategories.get(entry.getKey()))) {
                            value.reportedCategories.remove(entry.getKey());
                            categories.remove();
                        }
                    }
                    window.localities.forEach((locality, checkpoint) -> {
                        LocalityCounter count = value.localities.get(locality);
                        count.reportedIssued = checkpoint[0];
                        count.reportedSuccessful = checkpoint[1];
                        count.reportedErrors = checkpoint[2];
                    });
                    value.localities.values().removeIf(count -> count.inProgress == 0 && count.drained());
                }
                prune();
                changeListener.run();
            }
        }
    }

    private static final class Window {
        final Counter counter;
        final long version;
        final long end;
        final long dropped;
        final Map<String, Long> categories;
        final Map<Locality, long[]> localities = new LinkedHashMap<>();

        Window(Counter counter, long end) {
            this.counter = counter;
            this.end = end;
            version = counter.version;
            dropped = counter.dropped;
            categories = new LinkedHashMap<>(counter.categories);
            counter.localities.forEach((locality, count) ->
                    localities.put(locality, new long[] {count.issued, count.successful, count.errors}));
        }
    }

    private static final class Counter {
        final Key key;
        final ServerInfo origin;
        final Map<Locality, LocalityCounter> localities = new LinkedHashMap<>();
        final Map<String, Long> categories = new LinkedHashMap<>();
        final Map<String, Long> reportedCategories = new LinkedHashMap<>();
        boolean subscribed;
        long inProgress;
        long dropped;
        long reportedDropped;
        long since = System.nanoTime();
        long version;

        Counter(Key key, ServerInfo origin) {
            this.key = key;
            this.origin = origin;
        }

        boolean drained() {
            return inProgress == 0
                    && dropped == reportedDropped
                    && localities.values().stream().allMatch(LocalityCounter::drained);
        }
    }

    private static final class LocalityCounter {
        long issued;
        long successful;
        long errors;
        long inProgress;
        long reportedIssued;
        long reportedSuccessful;
        long reportedErrors;

        boolean drained() {
            return issued == reportedIssued && successful == reportedSuccessful && errors == reportedErrors;
        }
    }

    /** Graph snapshots may lag a CDS callback; preserve the origin of their exact immutable resource. */
    private static final class ResourceIdentity extends WeakReference<Cluster> {
        private final int hash;

        ResourceIdentity(Cluster cluster, ReferenceQueue<Cluster> queue) {
            super(cluster, queue);
            hash = System.identityHashCode(cluster);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object other) {
            return this == other
                    || (other instanceof ResourceIdentity
                            && get() != null
                            && get() == ((ResourceIdentity) other).get());
        }
    }

    private static final class Key {
        final ServerInfo server;
        final String cluster;
        final String service;

        Key(ServerInfo server, Cluster cluster) {
            this.server = server;
            this.cluster = cluster.getName();
            service = cluster.getEdsClusterConfig().getServiceName();
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Key)) return false;
            Key value = (Key) other;
            return server.equals(value.server) && cluster.equals(value.cluster) && service.equals(value.service);
        }

        @Override
        public int hashCode() {
            return Objects.hash(server, cluster, service);
        }
    }
}
