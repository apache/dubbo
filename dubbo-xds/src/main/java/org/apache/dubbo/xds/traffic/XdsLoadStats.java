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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import io.grpc.xds.shaded.io.envoyproxy.envoy.config.cluster.v3.Cluster;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.Locality;

/** Cumulative client diagnostics owned by one shared ADS runtime, independent of admission counters. */
public final class XdsLoadStats implements AutoCloseable {
    private static final int RETIRED_HISTORY = 128;
    private final Map<Key, Counter> counters = new LinkedHashMap<>();
    private final Map<String, Key> current = new LinkedHashMap<>();
    private boolean closed;
    private final XdsLoadReports reports = new XdsLoadReports();

    public XdsLoadReports getReports() {
        return reports;
    }

    public synchronized void accepted(Cluster cluster, ServerInfo origin) {
        if (closed) return;
        XdsLoadReports.validate(cluster);
        accepted(cluster);
        reports.accepted(cluster, origin);
    }

    /** Called only for accepted subscribed CDS resources; NACK does not change accounting identity. */
    public synchronized void accepted(Cluster cluster) {
        if (closed) return;
        Key key = new Key(cluster);
        Key old = current.put(cluster.getName(), key);
        if (old != null && !old.equals(key)) counters.get(old).subscribed = false;
        counter(key).subscribed = true;
        prune();
    }

    public synchronized void retired(String cluster) {
        reports.retired(cluster);
        Key key = current.remove(cluster);
        Counter value = key == null ? null : counters.get(key);
        if (value != null) value.subscribed = false;
        prune();
    }

    public synchronized Attempt started(Cluster cluster) {
        return started(cluster, Locality.getDefaultInstance());
    }

    public synchronized Attempt started(Cluster cluster, Locality locality) {
        if (closed) return new Attempt(null, null);
        Counter value = counter(new Key(cluster));
        value.issued++;
        value.inProgress++;
        return new Attempt(value, reports.started(cluster, locality));
    }

    public synchronized void circuitDropped(Cluster cluster) {
        if (closed) return;
        counter(new Key(cluster)).circuitDrops++;
        reports.dropped(cluster, "");
        prune();
    }

    public synchronized void edsDropped(Cluster cluster) {
        edsDropped(cluster, "");
    }

    public synchronized void edsDropped(Cluster cluster, String category) {
        if (closed) return;
        counter(new Key(cluster)).edsDrops++;
        reports.dropped(cluster, category);
        prune();
    }

    /** Immutable snapshots contain counts and resource names only, never RPC payloads or credentials. */
    public synchronized List<Snapshot> snapshot() {
        List<Snapshot> result = new ArrayList<>();
        counters.forEach((key, value) -> result.add(new Snapshot(key, value)));
        return Collections.unmodifiableList(result);
    }

    private Counter counter(Key key) {
        return counters.computeIfAbsent(key, unused -> new Counter());
    }

    private void prune() {
        int idle = 0;
        for (Counter value : counters.values()) if (!value.subscribed && value.inProgress == 0) idle++;
        Iterator<Counter> iterator = counters.values().iterator();
        while (idle > RETIRED_HISTORY && iterator.hasNext()) {
            Counter value = iterator.next();
            if (!value.subscribed && value.inProgress == 0) {
                iterator.remove();
                idle--;
            }
        }
    }

    @Override
    public synchronized void close() {
        closed = true;
        reports.close();
        counters.clear();
        current.clear();
    }

    /** An actual transport attempt, including each retry, terminates exactly once. */
    public final class Attempt {
        private final Counter counter;
        private final XdsLoadReports.Attempt reporting;
        private final AtomicBoolean finished = new AtomicBoolean();

        private Attempt(Counter counter, XdsLoadReports.Attempt reporting) {
            this.counter = counter;
            this.reporting = reporting;
        }

        public void finished(Throwable error) {
            if (counter != null && finished.compareAndSet(false, true)) {
                synchronized (XdsLoadStats.this) {
                    reporting.finished(error);
                    counter.inProgress--;
                    if (error == null) counter.successful++;
                    else counter.errors++;
                    prune();
                }
            }
        }
    }

    private static final class Counter {
        boolean subscribed;
        long issued;
        long inProgress;
        long successful;
        long errors;
        long circuitDrops;
        long edsDrops;
    }

    private static final class Key {
        final String cluster;
        final String service;

        Key(Cluster cluster) {
            this.cluster = cluster.getName();
            service = cluster.getEdsClusterConfig().getServiceName();
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Key)) return false;
            Key value = (Key) other;
            return cluster.equals(value.cluster) && service.equals(value.service);
        }

        @Override
        public int hashCode() {
            return Objects.hash(cluster, service);
        }
    }

    public static final class Snapshot {
        private final String cluster;
        private final String service;
        private final boolean subscribed;
        private final long issued;
        private final long inProgress;
        private final long successful;
        private final long errors;
        private final long circuitDrops;
        private final long edsDrops;

        private Snapshot(Key key, Counter value) {
            cluster = key.cluster;
            service = key.service;
            subscribed = value.subscribed;
            issued = value.issued;
            inProgress = value.inProgress;
            successful = value.successful;
            errors = value.errors;
            circuitDrops = value.circuitDrops;
            edsDrops = value.edsDrops;
        }

        public String getCluster() {
            return cluster;
        }

        public String getService() {
            return service;
        }

        public boolean isSubscribed() {
            return subscribed;
        }

        public long getIssued() {
            return issued;
        }

        public long getInProgress() {
            return inProgress;
        }

        public long getSuccessful() {
            return successful;
        }

        public long getErrors() {
            return errors;
        }

        public long getCircuitDrops() {
            return circuitDrops;
        }

        public long getEdsDrops() {
            return edsDrops;
        }
    }
}
