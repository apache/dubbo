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

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.xds.bootstrap.Bootstrapper.BootstrapInfo;
import org.apache.dubbo.xds.bootstrap.Bootstrapper.ServerInfo;
import org.apache.dubbo.xds.traffic.XdsLoadBalance.UnsupportedPolicyException;
import org.apache.dubbo.xds.traffic.XdsLoadStats;
import org.apache.dubbo.xds.transport.XdsTransport;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.cluster.v3.Cluster;
import io.grpc.xds.shaded.io.envoyproxy.envoy.service.discovery.v3.AggregatedDiscoveryServiceGrpc;
import io.grpc.xds.shaded.io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest;
import io.grpc.xds.shaded.io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse;

/** A shared SotW ADS session. All protocol state is confined to one executor; user callbacks run separately. */
public final class XdsClient implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(XdsClient.class);
    private final XdsLoadStats loadStats = new XdsLoadStats();
    private final BootstrapInfo bootstrap;
    private final long fetchTimeoutMillis;
    private final XdsLoadReportClient loadReporting;
    private final ScheduledExecutorService stateExecutor =
            Executors.newSingleThreadScheduledExecutor(r -> daemon(r, "Dubbo-xds-state"));
    private final ExecutorService callbacks = Executors.newSingleThreadExecutor(r -> daemon(r, "Dubbo-xds-callback"));
    private final Map<String, TypeState<?>> types = new LinkedHashMap<>();
    private final AtomicBoolean closed = new AtomicBoolean();
    private XdsTransport transport;
    private StreamObserver<DiscoveryRequest> stream;
    private ScheduledFuture<?> reconnect;
    private long streamGeneration;
    private int serverIndex;
    private int failures;
    private volatile String connectionState = "IDLE";
    private volatile Map<String, ResourceMetadata> resourceMetadata = Collections.emptyMap();

    public XdsClient(BootstrapInfo bootstrap) {
        this(bootstrap, Duration.ofSeconds(15));
    }

    public XdsClient(BootstrapInfo bootstrap, Duration fetchTimeout) {
        this.bootstrap = Objects.requireNonNull(bootstrap, "bootstrap");
        fetchTimeoutMillis = fetchTimeout.toMillis();
        if (fetchTimeoutMillis <= 0) {
            throw new IllegalArgumentException("Initial resource fetch timeout must be positive");
        }
        loadReporting = new XdsLoadReportClient(bootstrap, loadStats.getReports(), stateExecutor);
        loadStats.getReports().setChangeListener(() -> enqueue(loadReporting::refresh));
    }

    public Map<String, String> getLoadReportingStates() {
        return loadReporting.getStates();
    }

    public XdsLoadStats getLoadStats() {
        return loadStats;
    }

    public interface ResourceWatcher<T> {
        void onChanged(T resource);

        void onAbsent();

        void onError(Throwable error);
    }

    /** Validators run on the protocol executor before accepting or ACKing a response. */
    public <T extends Message> void addValidator(XdsResourceType<T> type, Consumer<T> validator) {
        submit(() -> state(type).validators.add(Objects.requireNonNull(validator, "validator")));
    }

    public <T extends Message> AutoCloseable watch(XdsResourceType<T> type, String name, ResourceWatcher<T> watcher) {
        return watch(type, name, watcher, ignored -> {});
    }

    /** A subscription may require execution semantics that a diagnostic/discovery watch does not. */
    public <T extends Message> AutoCloseable watch(
            XdsResourceType<T> type, String name, ResourceWatcher<T> watcher, Consumer<T> validator) {
        Objects.requireNonNull(watcher, "watcher");
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Resource name is required");
        }
        Registration<T> registration = new Registration<>(watcher, Objects.requireNonNull(validator, "validator"));
        submit(() -> {
            if (registration.cancelled.get()) {
                return;
            }
            TypeState<T> state = state(type);
            Subscription<T> subscription = state.subscriptions.computeIfAbsent(name, key -> new Subscription<>());
            subscription.registrations.add(registration);
            if (state.resources.containsKey(name)) {
                T resource = state.resources.get(name);
                try {
                    registration.validator.accept(resource);
                    dispatch(registration, () -> watcher.onChanged(resource));
                } catch (RuntimeException error) {
                    dispatch(registration, () -> watcher.onError(error));
                }
            } else if (subscription.absent) {
                dispatch(registration, watcher::onAbsent);
            } else {
                armTimer(state, name, subscription);
            }
            refreshMetadata();
            if (stream == null) {
                if (reconnect == null) {
                    connect();
                }
            } else {
                request(state, null);
            }
        });
        return () -> {
            if (registration.cancelled.compareAndSet(false, true) && !closed.get()) {
                enqueue(() -> {
                    TypeState<T> state = state(type);
                    Subscription<T> subscription = state.subscriptions.get(name);
                    if (subscription == null) {
                        return;
                    }
                    subscription.registrations.remove(registration);
                    if (subscription.registrations.isEmpty()) {
                        cancelTimer(subscription);
                        state.subscriptions.remove(name);
                        state.resources.remove(name);
                        state.origins.remove(name);
                        if (type == XdsResourceType.CDS) loadStats.retired(name);
                        state.acceptedAt.remove(name);
                        if (stream != null) {
                            // An empty list cancels explicit watches; do not open an idle ADS connection for it.
                            request(state, null);
                        }
                        refreshMetadata();
                    }
                    if (!hasSubscriptions()) {
                        stopStream();
                        connectionState = "IDLE";
                    }
                });
            }
        };
    }

    public String getConnectionState() {
        return connectionState;
    }

    public Map<String, ResourceMetadata> getResourceMetadata() {
        return resourceMetadata;
    }

    public <T extends Message> CompletableFuture<Map<String, T>> snapshot(XdsResourceType<T> type) {
        CompletableFuture<Map<String, T>> result = new CompletableFuture<>();
        try {
            submit(() -> result.complete(Collections.unmodifiableMap(new LinkedHashMap<>(state(type).resources))));
        } catch (IllegalStateException e) {
            result.completeExceptionally(e);
        }
        return result;
    }

    private void connect() {
        if (closed.get() || !hasSubscriptions()) {
            return;
        }
        connectionState = "CONNECTING";
        long generation = ++streamGeneration;
        try {
            transport = XdsTransport.connect(bootstrap.getServers().get(serverIndex));
            stream = AggregatedDiscoveryServiceGrpc.newStub(transport.getChannel())
                    .streamAggregatedResources(new StreamObserver<DiscoveryResponse>() {
                        @Override
                        public void onNext(DiscoveryResponse value) {
                            enqueue(() -> {
                                if (generation == streamGeneration && stream != null) {
                                    response(value);
                                }
                            });
                        }

                        @Override
                        public void onError(Throwable error) {
                            enqueue(() -> failed(generation, error));
                        }

                        @Override
                        public void onCompleted() {
                            enqueue(() -> failed(
                                    generation,
                                    Status.UNAVAILABLE
                                            .withDescription("ADS stream closed")
                                            .asRuntimeException()));
                        }
                    });
            for (TypeState<?> type : types.values()) {
                type.nonce = "";
                if (!type.subscriptions.isEmpty()) {
                    request(type, null);
                }
            }
        } catch (RuntimeException | LinkageError error) {
            failed(generation, error);
        }
    }

    private void failed(long generation, Throwable error) {
        if (closed.get() || generation != streamGeneration) {
            return;
        }
        stopStream();
        connectionState = "BACKOFF";
        for (TypeState<?> type : types.values()) {
            notifyError(type, error);
        }
        if (hasSubscriptions()) {
            failures = Math.min(failures + 1, 10);
            long delay = Math.min(30_000L, 200L << Math.min(failures - 1, 7));
            // Only one server is active. Switching servers must not reuse the previous server's versions.
            if (bootstrap.getServers().size() > 1) {
                serverIndex = (serverIndex + 1) % bootstrap.getServers().size();
                types.values().forEach(type -> type.version = "");
            }
            reconnect = stateExecutor.schedule(
                    () -> {
                        reconnect = null;
                        connect();
                    },
                    delay,
                    TimeUnit.MILLISECONDS);
        }
    }

    private void response(DiscoveryResponse response) {
        TypeState<?> type = types.get(response.getTypeUrl());
        if (type == null || type.subscriptions.isEmpty()) {
            return;
        }
        connectionState = "READY";
        failures = 0;
        receive(type, response);
    }

    private <T extends Message> void receive(TypeState<T> state, DiscoveryResponse response) {
        state.nonce = response.getNonce();
        Map<String, T> parsed = new LinkedHashMap<>();
        try {
            for (Any raw : response.getResourcesList()) {
                T resource = state.type.parse(raw);
                String name = state.type.name(resource);
                if (parsed.put(name, resource) != null) {
                    throw new IllegalArgumentException("Duplicate resource name");
                }
                for (Consumer<T> validator : state.validators) {
                    validator.accept(resource);
                }
                Subscription<T> subscription = state.subscriptions.get(name);
                if (subscription != null) {
                    for (Registration<T> registration : subscription.registrations) {
                        if (!registration.cancelled.get()) registration.validator.accept(resource);
                    }
                }
            }
        } catch (Exception error) {
            state.rejection = "Invalid " + state.type.getTypeUrl() + " resource: "
                    + error.getClass().getSimpleName()
                    + (error instanceof UnsupportedPolicyException ? ": " + error.getMessage() : "");
            request(
                    state,
                    com.google.rpc.Status.newBuilder()
                            .setCode(Status.Code.INVALID_ARGUMENT.value())
                            .setMessage(state.rejection)
                            .build());
            notifyError(state, new IllegalArgumentException(state.rejection, error));
            refreshMetadata();
            return;
        }
        state.version = response.getVersionInfo();
        state.rejection = null;
        ServerInfo server = bootstrap.getServers().get(serverIndex);
        for (Map.Entry<String, Subscription<T>> entry : state.subscriptions.entrySet()) {
            String name = entry.getKey();
            Subscription<T> subscription = entry.getValue();
            T resource = parsed.get(name);
            if (resource != null) {
                T old = state.resources.put(name, resource);
                ServerInfo previousOrigin = state.origins.put(name, server);
                if (resource instanceof Cluster) loadStats.accepted((Cluster) resource, server);
                state.acceptedAt.put(name, System.currentTimeMillis());
                subscription.absent = false;
                cancelTimer(subscription);
                // SELF LRS belongs to the resource's control plane, even if a fallback returns identical contents.
                if (!resource.equals(old) || (state.type == XdsResourceType.CDS && previousOrigin != server)) {
                    for (Registration<T> registration : subscription.registrations) {
                        dispatch(registration, () -> registration.watcher.onChanged(resource));
                    }
                }
            } else if (state.type.isFullState()
                    && !server.ignoresResourceDeletion()
                    && state.resources.containsKey(name)) {
                state.resources.remove(name);
                state.origins.remove(name);
                if (state.type == XdsResourceType.CDS) loadStats.retired(name);
                state.acceptedAt.remove(name);
                cancelTimer(subscription);
                if (!subscription.absent) {
                    subscription.absent = true;
                    for (Registration<T> registration : subscription.registrations) {
                        dispatch(registration, registration.watcher::onAbsent);
                    }
                }
            }
        }
        request(state, null);
        refreshMetadata();
    }

    private void request(TypeState<?> state, com.google.rpc.Status error) {
        if (stream == null) {
            return;
        }
        DiscoveryRequest.Builder request = DiscoveryRequest.newBuilder()
                .setNode(bootstrap.getNode())
                .setTypeUrl(state.type.getTypeUrl())
                .setVersionInfo(state.version)
                .setResponseNonce(state.nonce)
                .addAllResourceNames(state.subscriptions.keySet());
        if (error != null) {
            request.setErrorDetail(error);
        }
        try {
            stream.onNext(request.build());
        } catch (RuntimeException e) {
            failed(streamGeneration, e);
        }
    }

    private <T extends Message> void armTimer(TypeState<T> state, String name, Subscription<T> subscription) {
        if (subscription.timer == null) {
            subscription.timer = stateExecutor.schedule(
                    () -> {
                        subscription.timer = null;
                        if (!state.resources.containsKey(name)) {
                            subscription.absent = true;
                            for (Registration<T> registration : subscription.registrations) {
                                dispatch(registration, registration.watcher::onAbsent);
                            }
                            refreshMetadata();
                        }
                    },
                    fetchTimeoutMillis,
                    TimeUnit.MILLISECONDS);
        }
    }

    private static void cancelTimer(Subscription<?> subscription) {
        if (subscription.timer != null) {
            subscription.timer.cancel(false);
            subscription.timer = null;
        }
    }

    private void refreshMetadata() {
        Map<String, ResourceMetadata> result = new LinkedHashMap<>();
        for (TypeState<?> type : types.values()) {
            for (Map.Entry<String, ? extends Subscription<?>> entry : type.subscriptions.entrySet()) {
                String name = entry.getKey();
                result.put(
                        type.type.getTypeUrl() + ":" + name,
                        new ResourceMetadata(
                                type.version,
                                type.nonce,
                                type.resources.containsKey(name),
                                entry.getValue().absent,
                                type.acceptedAt.getOrDefault(name, 0L),
                                type.rejection));
            }
        }
        resourceMetadata = Collections.unmodifiableMap(result);
    }

    private <T extends Message> void notifyError(TypeState<T> type, Throwable error) {
        for (Subscription<T> subscription : type.subscriptions.values()) {
            for (Registration<T> registration : subscription.registrations) {
                dispatch(registration, () -> registration.watcher.onError(error));
            }
        }
    }

    private <T extends Message> void dispatch(Registration<T> registration, Runnable callback) {
        try {
            callbacks.execute(() -> {
                if (!registration.cancelled.get() && !closed.get()) {
                    try {
                        callback.run();
                    } catch (Throwable error) {
                        LOGGER.warn("xDS resource watcher failed", error);
                    }
                }
            });
        } catch (RejectedExecutionException ignored) {
            // Closing the shared runtime also cancels callbacks.
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends Message> TypeState<T> state(XdsResourceType<T> type) {
        return (TypeState<T>) types.computeIfAbsent(type.getTypeUrl(), key -> new TypeState<>(type));
    }

    private boolean hasSubscriptions() {
        return types.values().stream().anyMatch(type -> !type.subscriptions.isEmpty());
    }

    private void stopStream() {
        ++streamGeneration;
        if (reconnect != null) {
            reconnect.cancel(false);
            reconnect = null;
        }
        StreamObserver<DiscoveryRequest> old = stream;
        stream = null;
        if (old != null) {
            try {
                old.onError(
                        Status.CANCELLED.withDescription("ADS session released").asRuntimeException());
            } catch (RuntimeException ignored) {
                // The transport may have already terminated the stream.
            }
        }
        if (transport != null) {
            transport.close();
            transport = null;
        }
    }

    private void submit(Runnable task) {
        if (closed.get()) {
            throw new IllegalStateException("xDS client is closed");
        }
        try {
            stateExecutor.execute(task);
        } catch (RejectedExecutionException e) {
            throw new IllegalStateException("xDS client is closed", e);
        }
    }

    private void enqueue(Runnable task) {
        if (!closed.get()) {
            try {
                stateExecutor.execute(task);
            } catch (RejectedExecutionException ignored) {
                // A channel callback raced with shutdown.
            }
        }
    }

    private static Thread daemon(Runnable task, String name) {
        Thread thread = new Thread(task, name);
        thread.setDaemon(true);
        return thread;
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            stateExecutor.execute(() -> {
                loadReporting.close();
                stopStream();
                for (TypeState<?> type : types.values()) {
                    type.subscriptions.values().forEach(XdsClient::cancelTimer);
                }
                types.clear();
                loadStats.close();
                resourceMetadata = Collections.emptyMap();
                connectionState = "CLOSED";
            });
            stateExecutor.shutdown();
            callbacks.shutdownNow();
        }
    }

    private static final class TypeState<T extends Message> {
        final XdsResourceType<T> type;
        final Map<String, T> resources = new LinkedHashMap<>();
        final Map<String, ServerInfo> origins = new LinkedHashMap<>();
        final Map<String, Long> acceptedAt = new LinkedHashMap<>();
        final Map<String, Subscription<T>> subscriptions = new LinkedHashMap<>();
        final List<Consumer<T>> validators = new ArrayList<>();
        String version = "";
        String nonce = "";
        String rejection;

        TypeState(XdsResourceType<T> type) {
            this.type = type;
        }
    }

    private static final class Subscription<T extends Message> {
        final Set<Registration<T>> registrations = new LinkedHashSet<>();
        ScheduledFuture<?> timer;
        boolean absent;
    }

    private static final class Registration<T extends Message> {
        final ResourceWatcher<T> watcher;
        final Consumer<T> validator;
        final AtomicBoolean cancelled = new AtomicBoolean();

        Registration(ResourceWatcher<T> watcher, Consumer<T> validator) {
            this.watcher = watcher;
            this.validator = validator;
        }
    }

    public enum ResourceStatus {
        REQUESTED,
        ACKED,
        ABSENT,
        NACKED_WITH_LAST_GOOD,
        NACKED_WITHOUT_RESOURCE
    }

    public static final class ResourceMetadata {
        private final String version;
        private final String nonce;
        private final boolean present;
        private final boolean absent;
        private final long acceptedAt;
        private final String rejection;

        ResourceMetadata(
                String version, String nonce, boolean present, boolean absent, long acceptedAt, String rejection) {
            this.version = version;
            this.nonce = nonce;
            this.present = present;
            this.absent = absent;
            this.acceptedAt = acceptedAt;
            this.rejection = rejection;
        }

        /** Distinguishes a rejected update with a usable prior resource from an unresolved subscription. */
        public ResourceStatus getStatus() {
            if (rejection != null) {
                return present ? ResourceStatus.NACKED_WITH_LAST_GOOD : ResourceStatus.NACKED_WITHOUT_RESOURCE;
            }
            if (present) return ResourceStatus.ACKED;
            return absent ? ResourceStatus.ABSENT : ResourceStatus.REQUESTED;
        }

        public String getVersion() {
            return version;
        }

        public String getNonce() {
            return nonce;
        }

        public boolean isPresent() {
            return present;
        }

        public boolean isAbsent() {
            return absent;
        }

        public long getAcceptedAt() {
            return acceptedAt;
        }

        public String getRejection() {
            return rejection;
        }
    }
}
