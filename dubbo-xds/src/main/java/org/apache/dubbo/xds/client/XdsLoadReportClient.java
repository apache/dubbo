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
import org.apache.dubbo.xds.bootstrap.Bootstrapper.ServerInfo;
import org.apache.dubbo.xds.traffic.XdsLoadReports;
import org.apache.dubbo.xds.traffic.XdsLoadReports.Batch;
import org.apache.dubbo.xds.transport.XdsTransport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.util.Durations;
import io.grpc.Status;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.Node;
import io.grpc.xds.shaded.io.envoyproxy.envoy.service.load_stats.v3.LoadReportingServiceGrpc;
import io.grpc.xds.shaded.io.envoyproxy.envoy.service.load_stats.v3.LoadStatsRequest;
import io.grpc.xds.shaded.io.envoyproxy.envoy.service.load_stats.v3.LoadStatsResponse;

/** Opt-in SELF LRS streams share ADS's protocol executor, but failures never restart ADS. */
final class XdsLoadReportClient implements AutoCloseable {
    private final BootstrapInfo bootstrap;
    private final Node node;
    private final XdsLoadReports reports;
    private final ScheduledExecutorService executor;
    private final Map<ServerInfo, Reporter> reporters = new LinkedHashMap<>();
    private volatile Map<String, String> states = Collections.emptyMap();
    private volatile boolean closed;

    XdsLoadReportClient(BootstrapInfo bootstrap, XdsLoadReports reports, ScheduledExecutorService executor) {
        this.bootstrap = bootstrap;
        this.reports = reports;
        this.executor = executor;
        Node.Builder node = bootstrap.getNode().toBuilder();
        if (!node.getClientFeaturesList().contains("envoy.lrs.supports_send_all_clusters"))
            node.addClientFeatures("envoy.lrs.supports_send_all_clusters");
        this.node = node.build();
    }

    Map<String, String> getStates() {
        return states;
    }

    void refresh() {
        if (closed) return;
        Set<ServerInfo> required = reports.servers();
        for (ServerInfo key : new ArrayList<>(reporters.keySet())) {
            if (!required.contains(key)) reporters.remove(key).close();
        }
        required.forEach(server -> {
            if (!reporters.containsKey(server)) {
                Reporter reporter = new Reporter(server);
                reporters.put(server, reporter);
                reporter.start();
            }
        });
        publish();
    }

    private void enqueue(Runnable task) {
        if (!closed) {
            try {
                executor.execute(() -> {
                    if (!closed) task.run();
                });
            } catch (RejectedExecutionException ignored) {
                /* Runtime is shutting down. */
            }
        }
    }

    private void publish() {
        Map<String, String> snapshot = new LinkedHashMap<>();
        reporters
                .values()
                .forEach(reporter ->
                        snapshot.put("server-" + bootstrap.getServers().indexOf(reporter.server), reporter.state));
        states = Collections.unmodifiableMap(snapshot);
    }

    @Override
    public void close() {
        closed = true;
        reporters.values().forEach(Reporter::close);
        reporters.clear();
        states = Collections.emptyMap();
    }

    private final class Reporter implements AutoCloseable {
        final ServerInfo server;
        XdsTransport transport;
        ClientCallStreamObserver<LoadStatsRequest> stream;
        ScheduledFuture<?> timer;
        long generation;
        long interval;
        int failures;
        boolean stopped;
        boolean all;
        Set<String> clusters = Collections.emptySet();
        String state = "CONNECTING";

        Reporter(ServerInfo server) {
            this.server = server;
        }

        void start() {
            if (stopped || closed) return;
            state = "CONNECTING";
            long expected = ++generation;
            try {
                if (transport == null) transport = XdsTransport.connect(server);
                LoadReportingServiceGrpc.newStub(transport.getChannel())
                        .streamLoadStats(new ClientResponseObserver<LoadStatsRequest, LoadStatsResponse>() {
                            @Override
                            public void beforeStart(ClientCallStreamObserver<LoadStatsRequest> request) {
                                stream = request;
                            }

                            @Override
                            public void onNext(LoadStatsResponse response) {
                                enqueue(() -> {
                                    if (active(expected)) response(response, expected);
                                });
                            }

                            @Override
                            public void onError(Throwable error) {
                                enqueue(() -> failed(expected, Status.fromThrowable(error)));
                            }

                            @Override
                            public void onCompleted() {
                                enqueue(() -> failed(expected, Status.UNAVAILABLE));
                            }
                        });
                stream.onNext(LoadStatsRequest.newBuilder().setNode(node).build());
                publish();
            } catch (RuntimeException | LinkageError error) {
                failed(expected, Status.fromThrowable(error));
            }
        }

        boolean active(long expected) {
            return !closed && !stopped && generation == expected && stream != null;
        }

        void response(LoadStatsResponse response, long expected) {
            try {
                if (!response.hasLoadReportingInterval() || !Durations.isValid(response.getLoadReportingInterval()))
                    throw new IllegalArgumentException();
                interval = Durations.toNanos(response.getLoadReportingInterval());
                if (interval < 0) throw new IllegalArgumentException();
            } catch (RuntimeException invalid) {
                failed(expected, Status.INVALID_ARGUMENT);
                return;
            }
            clusters = new LinkedHashSet<>(response.getClustersList());
            all = response.getSendAllClusters();
            failures = 0;
            state = interval == 0 ? "PAUSED" : "READY";
            schedule(expected);
            publish();
        }

        void schedule(long expected) {
            if (timer != null) timer.cancel(false);
            timer = interval > 0 && active(expected)
                    ? executor.schedule(() -> send(expected), interval, TimeUnit.NANOSECONDS)
                    : null;
        }

        void send(long expected) {
            timer = null;
            if (!active(expected)) return;
            if (!stream.isReady()) {
                schedule(expected);
                return;
            }
            Batch batch = reports.prepare(server, clusters, all);
            try {
                stream.onNext(LoadStatsRequest.newBuilder()
                        .setNode(node)
                        .addAllClusterStats(batch.getStats())
                        .build());
                batch.commit();
                refresh();
                schedule(expected);
            } catch (RuntimeException error) {
                failed(expected, Status.fromThrowable(error));
            }
        }

        void failed(long expected, Status status) {
            if (stopped || closed || generation != expected) return;
            ++generation;
            if (timer != null) timer.cancel(false);
            ClientCallStreamObserver<LoadStatsRequest> old = stream;
            stream = null;
            if (old != null) old.cancel("LRS stream ended", null);
            state = "BACKOFF:" + status.getCode().name();
            publish();
            failures = Math.min(failures + 1, 10);
            long delay = Math.min(30_000L, 200L << Math.min(failures - 1, 7));
            long jitter = ThreadLocalRandom.current().nextLong(Math.max(1, delay / 5));
            timer = executor.schedule(
                    () -> {
                        timer = null;
                        start();
                    },
                    delay + jitter,
                    TimeUnit.MILLISECONDS);
        }

        @Override
        public void close() {
            stopped = true;
            ++generation;
            if (timer != null) timer.cancel(false);
            if (stream != null) stream.cancel("LRS reporting released", null);
            stream = null;
            if (transport != null) transport.close();
            transport = null;
        }
    }
}
