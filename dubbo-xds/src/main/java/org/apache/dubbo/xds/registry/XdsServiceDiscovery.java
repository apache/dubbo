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
package org.apache.dubbo.xds.registry;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.client.AbstractServiceDiscovery;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.xds.bootstrap.Bootstrapper;
import org.apache.dubbo.xds.bootstrap.Bootstrapper.BootstrapInfo;
import org.apache.dubbo.xds.client.XdsResourceGraph;
import org.apache.dubbo.xds.client.XdsResourceGraph.Snapshot;
import org.apache.dubbo.xds.client.XdsRuntimePool;
import org.apache.dubbo.xds.security.CertificateFiles;
import org.apache.dubbo.xds.traffic.XdsLoadStats;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import com.google.protobuf.Value;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.cluster.v3.Cluster;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.HealthStatus;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.ClusterLoadAssignment;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.LbEndpoint;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.LocalityLbEndpoints;

import static org.apache.dubbo.common.constants.CommonConstants.REGISTRY_LOCAL_FILE_CACHE_ENABLED;
import static org.apache.dubbo.common.constants.RegistryConstants.PROVIDED_BY;
import static org.apache.dubbo.common.constants.RegistryConstants.PROVIDER_PORT;

/** Workload addresses come from xDS; each instance supplies its own revision-free metadata. */
public final class XdsServiceDiscovery extends AbstractServiceDiscovery {
    private final BootstrapInfo bootstrap;
    private final XdsRuntimePool.Lease runtime;
    private final AutoCloseable certificateWatch;
    private final ScheduledExecutorService state;
    private final ThreadPoolExecutor workers;
    private final MetadataFetcher metadataFetcher;
    private final Map<String, Binding> bindings = new ConcurrentHashMap<>();
    private final long ttlMillis;
    private final long refreshMillis;
    private final int maxEndpoints;
    private final AtomicLong discardedMetadataResults = new AtomicLong();

    public XdsServiceDiscovery(ApplicationModel applicationModel, URL registryURL) {
        this(applicationModel, registryURL, null);
    }

    XdsServiceDiscovery(ApplicationModel applicationModel, URL registryURL, MetadataFetcher metadataFetcher) {
        this(applicationModel, registryURL, metadataFetcher, loadBootstrap(registryURL));
    }

    private XdsServiceDiscovery(
            ApplicationModel applicationModel,
            URL registryURL,
            MetadataFetcher metadataFetcher,
            BootstrapInfo bootstrap) {
        super(applicationModel, checkedRegistryURL(registryURL));
        this.bootstrap = bootstrap;
        ttlMillis = registryURL.getParameter("xds.metadata.ttl", 60_000L);
        refreshMillis = registryURL.getParameter("xds.metadata.refresh", 30_000L);
        maxEndpoints = registryURL.getParameter("xds.metadata.max-endpoints", 2048);
        int concurrency = registryURL.getParameter("xds.metadata.concurrency", 4);
        try {
            runtime = XdsRuntimePool.acquire(bootstrap);
        } catch (RuntimeException error) {
            refreshCacheFuture.cancel(true);
            metaCacheManager.destroy();
            throw error;
        }
        state = Executors.newSingleThreadScheduledExecutor(task -> daemon(task, "dubbo-xds-discovery"));
        workers = new ThreadPoolExecutor(
                concurrency,
                concurrency,
                0,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(256),
                task -> daemon(task, "dubbo-xds-metadata"));
        this.metadataFetcher =
                metadataFetcher == null ? new PointToPointMetadataFetcher(applicationModel) : metadataFetcher;
        state.scheduleWithFixedDelay(this::refresh, 500, 500, TimeUnit.MILLISECONDS);
        certificateWatch = runtime.getSecurity()
                .watchCertificates(() -> execute(() -> {
                    for (Binding binding : bindings.values()) {
                        if (binding.snapshot != null) {
                            update(binding, binding.snapshot);
                        }
                    }
                }));
    }

    private static URL checkedRegistryURL(URL url) {
        long ttl = url.getParameter("xds.metadata.ttl", 60_000L);
        long refresh = url.getParameter("xds.metadata.refresh", 30_000L);
        int max = url.getParameter("xds.metadata.max-endpoints", 2048);
        int concurrency = url.getParameter("xds.metadata.concurrency", 4);
        if (ttl < 1 || refresh < 1 || refresh > ttl || max < 1 || concurrency < 1 || concurrency > 64) {
            throw new IllegalArgumentException("Invalid xDS metadata cache or concurrency configuration");
        }
        return url.addParameter(REGISTRY_LOCAL_FILE_CACHE_ENABLED, "false");
    }

    private static BootstrapInfo loadBootstrap(URL registryURL) {
        checkedRegistryURL(registryURL);
        try {
            String file = registryURL.getParameter("xds.bootstrap.file");
            String config = registryURL.getParameter("xds.bootstrap.config");
            long wait = registryURL.getParameter("xds.bootstrap.wait", 30_000L);
            if ((file == null || file.trim().isEmpty())
                    && (config == null || config.trim().isEmpty())) {
                return Bootstrapper.loadDefault(wait);
            }
            return Bootstrapper.load(System.getenv(), file, config, wait);
        } catch (IOException error) {
            throw new IllegalStateException("Unable to load xDS bootstrap", error);
        }
    }

    private static Thread daemon(Runnable task, String name) {
        Thread thread = new Thread(task, name);
        thread.setDaemon(true);
        return thread;
    }

    URL referenceTargets(URL reference) {
        String services = reference.getParameter(PROVIDED_BY);
        if (services == null || services.trim().isEmpty()) {
            throw new IllegalArgumentException("xDS references require explicit provided-by Kubernetes Services");
        }
        String namespace = MetadataEntryOptions.parameter(
                reference,
                registryURL,
                "provider-namespace",
                bootstrap
                        .getNode()
                        .getMetadata()
                        .getFieldsOrDefault("NAMESPACE", Value.getDefaultInstance())
                        .getStringValue());
        String domain = MetadataEntryOptions.parameter(reference, registryURL, "xds.cluster-domain", "cluster.local");
        String protocol = reference.getParameter("protocol", "tri");
        int port = reference.getParameter(PROVIDER_PORT, registryURL.getParameter(PROVIDER_PORT, 0));
        Set<String> targets = new LinkedHashSet<>();
        for (String service : services.split(",")) {
            DiscoveryTarget target = service.contains("://")
                    ? DiscoveryTarget.parse(service.trim())
                    : DiscoveryTarget.kubernetes(service.trim(), namespace, domain, port, protocol);
            if (!protocol.equals(target.getProtocol())) {
                throw new IllegalArgumentException("Explicit xDS target protocol must match the reference protocol");
            }
            targets.add(target.toString());
        }
        return reference.addParameter(PROVIDED_BY, String.join(",", targets)).addParameter("protocol", protocol);
    }

    URL bindReference(URL reference) {
        return bindReference(reference, null);
    }

    synchronized URL bindReference(URL reference, NotifyListener owner) {
        URL transformed = referenceTargets(reference);
        MetadataEntryConfig config = new MetadataEntryConfig(reference, registryURL);
        Map<String, DiscoveryTarget> targets = new LinkedHashMap<>();
        for (String key : transformed.getParameter(PROVIDED_BY).split(",")) {
            DiscoveryTarget target = DiscoveryTarget.parse(key);
            new MetadataEntryOptions(config, target);
            Binding existing = bindings.get(key);
            if (existing != null && !existing.config.equals(config)) {
                throw new IllegalArgumentException(
                        "References sharing an xDS target must use the same metadata configuration");
            }
            targets.put(key, target);
        }
        targets.forEach((key, target) -> {
            Binding binding = bindings.computeIfAbsent(key, ignored -> new Binding(target, config));
            if (owner != null) binding.owners.add(owner);
            execute(() -> start(binding));
        });
        return transformed;
    }

    void releaseReference(URL targets, NotifyListener owner) {
        for (String key : targets.getParameter(PROVIDED_BY).split(",")) {
            Binding binding = bindings.get(key);
            if (binding != null) {
                binding.owners.remove(owner);
            }
        }
        execute(this::removeUnusedBindings);
    }

    private void removeUnusedBindings() {
        for (Map.Entry<String, Binding> item : bindings.entrySet()) {
            String key = item.getKey();
            boolean used = instanceListeners.stream()
                    .anyMatch(other -> other.getServiceNames().contains(key));
            if (!used && item.getValue().owners.isEmpty()) {
                close(bindings.remove(key));
            }
        }
    }

    private void start(Binding binding) {
        if (binding.graph != null || bindings.get(binding.target.toString()) != binding || isDestroy()) {
            return;
        }
        binding.graph = new XdsResourceGraph(
                runtime.getClient(),
                bootstrap,
                binding.target,
                snapshot -> execute(() -> update(binding, snapshot)),
                error -> binding.lastError = error.getClass().getSimpleName());
    }

    private MetadataContext metadataContext(Binding binding, DiscoveryTarget target) {
        MetadataContext existing = binding.metadataContexts.get(target);
        if (existing != null) return existing;
        MetadataContext context = new MetadataContext();
        binding.metadataContexts.put(target, context);
        context.graph = XdsResourceGraph.forMetadata(
                runtime.getClient(),
                bootstrap,
                target,
                snapshot -> execute(() -> {
                    if (bindings.get(binding.target.toString()) != binding
                            || binding.metadataContexts.get(target) != context
                            || isDestroy()) return;
                    context.snapshot = snapshot;
                    if (binding.snapshot != null) update(binding, binding.snapshot);
                }),
                error -> binding.lastError = "MetadataGraph" + error.getClass().getSimpleName());
        return context;
    }

    private static DiscoveryTarget destination(DiscoveryTarget source, String cluster) {
        if (!cluster.startsWith("outbound|")) return source;
        String[] parts = cluster.split("\\|", -1);
        if (parts.length != 4) throw new IllegalArgumentException("Invalid Istio outbound cluster identity");
        return new DiscoveryTarget(parts[3], Integer.parseInt(parts[1]), source.getProtocol());
    }

    private void update(Binding binding, Snapshot snapshot) {
        if (bindings.get(binding.target.toString()) != binding || isDestroy()) {
            return;
        }
        binding.snapshot = snapshot;
        Map<String, XdsEndpoint> desired = addresses(snapshot, () -> binding.snapshot);
        Map<String, XdsEndpoint> metadata = new LinkedHashMap<>();
        Map<String, MetadataEntryOptions> options = new LinkedHashMap<>();
        Set<DiscoveryTarget> requiredMetadata = new LinkedHashSet<>();
        Map<DiscoveryTarget, Map<String, List<XdsEndpoint>>> metadataByHost = new LinkedHashMap<>();
        for (Cluster cluster : snapshot.getClusters().values()) {
            DiscoveryTarget destination = destination(binding.target, cluster.getName());
            MetadataEntryOptions entry = new MetadataEntryOptions(binding.config, destination);
            if (!destination.equals(binding.target) || !entry.target.equals(binding.target)) {
                requiredMetadata.add(entry.target);
                MetadataContext context = metadataContext(binding, entry.target);
                Map<String, List<XdsEndpoint>> byHost = new LinkedHashMap<>();
                if (context.snapshot != null && context.snapshot.isReady()) {
                    addresses(context.snapshot, () -> context.snapshot)
                            .values()
                            .forEach(
                                    endpoint -> byHost.computeIfAbsent(endpoint.getHost(), ignored -> new ArrayList<>())
                                            .add(endpoint));
                }
                metadataByHost.put(entry.target, byHost);
            }
        }
        for (DiscoveryTarget target : new ArrayList<>(binding.metadataContexts.keySet())) {
            if (!requiredMetadata.contains(target))
                binding.metadataContexts.remove(target).graph.close();
        }
        desired.forEach((identity, endpoint) -> {
            DiscoveryTarget destination =
                    destination(binding.target, endpoint.getCluster().getName());
            MetadataEntryOptions entry = new MetadataEntryOptions(binding.config, destination);
            options.put(identity, entry);
            if (destination.equals(binding.target) && entry.target.equals(binding.target)) {
                metadata.put(identity, endpoint);
            } else {
                List<XdsEndpoint> candidates =
                        metadataByHost.get(entry.target).getOrDefault(endpoint.getHost(), Collections.emptyList());
                if (candidates.size() == 1) metadata.put(identity, candidates.get(0));
                else
                    binding.lastError =
                            candidates.isEmpty() ? "MetadataEndpointUnavailable" : "MetadataEndpointAmbiguous";
            }
        });
        for (String identity : new ArrayList<>(binding.entries.keySet())) {
            Entry old = binding.entries.get(identity);
            if (!desired.containsKey(identity)
                    || !old.endpoint.hasSameMetadataContext(desired.get(identity))
                    || (old.metadataEndpoint == null
                            ? metadata.get(identity) != null
                            : !old.metadataEndpoint.hasSameMetadataContext(metadata.get(identity)))) {
                if (old.task != null) {
                    old.task.cancel(true);
                }
                binding.entries.remove(identity);
            }
        }
        for (Map.Entry<String, XdsEndpoint> item : desired.entrySet()) {
            Entry entry = binding.entries.get(item.getKey());
            if (entry == null && binding.entries.size() < maxEndpoints) {
                entry = new Entry(item.getValue(), metadata.get(item.getKey()), options.get(item.getKey()));
                binding.entries.put(item.getKey(), entry);
                fetch(binding, entry);
            } else if (entry != null) {
                entry.endpoint = item.getValue();
                entry.metadataEndpoint = metadata.get(item.getKey());
            } else {
                binding.lastError = "MetadataEndpointLimit";
            }
        }
        publish(binding);
    }

    private Map<String, XdsEndpoint> addresses(Snapshot snapshot, Supplier<Snapshot> current) {
        Map<String, XdsEndpoint> desired = new LinkedHashMap<>();
        for (Cluster cluster : snapshot.getClusters().values()) {
            String eds = cluster.getEdsClusterConfig().getServiceName();
            ClusterLoadAssignment assignment = snapshot.getEndpoints().get(eds.isEmpty() ? cluster.getName() : eds);
            if (assignment == null) {
                continue;
            }
            for (LocalityLbEndpoints locality : assignment.getEndpointsList()) {
                if (locality.hasLoadBalancingWeight()
                        && locality.getLoadBalancingWeight().getValue() == 0) {
                    continue;
                }
                for (LbEndpoint endpoint : locality.getLbEndpointsList()) {
                    if ((endpoint.getHealthStatus() != HealthStatus.UNKNOWN
                                    && endpoint.getHealthStatus() != HealthStatus.HEALTHY)
                            || (!("tri".equals(snapshot.getTarget().getProtocol())
                                            && cluster.getLbPolicy() == Cluster.LbPolicy.LEAST_REQUEST)
                                    && endpoint.hasLoadBalancingWeight()
                                    && endpoint.getLoadBalancingWeight().getValue() == 0)) {
                        continue;
                    }
                    XdsEndpoint address =
                            new XdsEndpoint(snapshot, cluster, locality, endpoint, runtime.getSecurity(), current);
                    desired.put(address.identity(), address);
                }
            }
        }
        return desired;
    }

    private void fetch(Binding binding, Entry entry) {
        if (entry.task != null || entry.metadataEndpoint == null || isDestroy()) {
            return;
        }
        XdsEndpoint endpoint = entry.endpoint;
        XdsEndpoint metadataEndpoint = entry.metadataEndpoint;
        try {
            entry.task = workers.submit(() -> {
                MetadataInfo metadata = null;
                Throwable failure = null;
                try {
                    metadata = metadataFetcher.fetch(endpoint, metadataEndpoint, entry.options);
                } catch (Exception error) {
                    failure = error;
                }
                MetadataInfo value = metadata;
                Throwable error = failure;
                execute(() -> complete(binding, entry, value, error));
            });
        } catch (RejectedExecutionException rejected) {
            entry.nextAttempt = System.currentTimeMillis() + 1000;
            binding.lastError = "MetadataQueueFull";
        }
    }

    private void complete(Binding binding, Entry entry, MetadataInfo metadata, Throwable failure) {
        if (isDestroy()
                || bindings.get(binding.target.toString()) != binding
                || binding.entries.get(entry.endpoint.identity()) != entry) {
            discardedMetadataResults.incrementAndGet();
            return;
        }
        entry.task = null;
        long now = System.currentTimeMillis();
        if (failure == null && metadata != null) {
            binding.metadataSuccesses++;
            entry.lastFailure = null;
            entry.failureCauses = Collections.emptyList();
            entry.metadata = metadata;
            entry.expires = now + ttlMillis;
            entry.nextAttempt = now + refreshMillis;
            entry.failures = 0;
            if (binding.entries.values().stream().allMatch(item -> item.metadataEndpoint != null)) {
                binding.lastError = null;
            }
        } else {
            binding.metadataFailures++;
            entry.lastFailure = failure == null
                    ? "EMPTY_METADATA"
                    : TriRpcStatus.getStatus(failure).code.name();
            entry.failureCauses = failureCauses(failure);
            entry.failures = Math.min(entry.failures + 1, 5);
            entry.nextAttempt = now + Math.min(30_000, 1000L << (entry.failures - 1));
            binding.lastError =
                    failure == null ? "EmptyMetadata" : failure.getClass().getSimpleName();
        }
        publish(binding);
    }

    private void refresh() {
        if (isDestroy()) {
            return;
        }
        long now = System.currentTimeMillis();
        for (Binding binding : bindings.values()) {
            boolean expired = false;
            for (Entry entry : binding.entries.values()) {
                if (entry.metadata != null && now >= entry.expires) {
                    entry.metadata = null;
                    expired = true;
                }
                if (now >= entry.nextAttempt) {
                    fetch(binding, entry);
                }
            }
            if (expired) {
                publish(binding);
            }
        }
    }

    private void publish(Binding binding) {
        List<ServiceInstance> instances = new ArrayList<>();
        for (Entry entry : binding.entries.values()) {
            if (entry.metadata != null && entry.expires > System.currentTimeMillis()) {
                instances.add(new XdsServiceInstance(entry.endpoint, entry.metadata, applicationModel));
            }
        }
        binding.instances = Collections.unmodifiableList(instances);
        ServiceInstancesChangedEvent event = new ServiceInstancesChangedEvent(binding.target.toString(), instances);
        for (ServiceInstancesChangedListener listener : instanceListeners) {
            if (listener.getServiceNames().contains(binding.target.toString())) {
                try {
                    listener.onEvent(event);
                } catch (RuntimeException error) {
                    binding.lastError = "InstanceListenerFailure";
                }
            }
        }
    }

    private void execute(Runnable task) {
        if (!isDestroy()) {
            try {
                state.execute(task);
            } catch (RejectedExecutionException shutdown) {
                if (!isDestroy()) {
                    throw shutdown;
                }
            }
        }
    }

    @Override
    public Set<String> getServices() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(bindings.keySet()));
    }

    /** LRS states use bootstrap server indexes and never expose channel credentials or addresses. */
    public Map<String, String> inspectLoadReporting() {
        return isDestroy() ? Collections.emptyMap() : runtime.getClient().getLoadReportingStates();
    }

    /** Shared certificate state excludes bootstrap configuration and never initiates file loading. */
    public Map<String, CertificateFiles.Status> inspectCertificateProviders() {
        return isDestroy() ? Collections.emptyMap() : runtime.getSecurity().inspectCertificateProviders();
    }

    /** Shared workload totals include business and metadata attempts across all references and registries. */
    public List<XdsLoadStats.Snapshot> inspectLoadStats() {
        return isDestroy()
                ? Collections.emptyList()
                : runtime.getClient().getLoadStats().snapshot();
    }

    /** A serialized diagnostic snapshot; it excludes bootstrap contents, credentials and metadata payloads. */
    public CompletableFuture<Map<String, TargetStatus>> inspectTargets() {
        if (isDestroy()) {
            return CompletableFuture.completedFuture(Collections.emptyMap());
        }
        CompletableFuture<Map<String, TargetStatus>> result = new CompletableFuture<>();
        try {
            state.execute(() -> {
                Map<String, TargetStatus> statuses = new LinkedHashMap<>();
                bindings.forEach((name, binding) -> statuses.put(
                        name,
                        new TargetStatus(
                                binding.entries.size(),
                                binding.instances.size(),
                                binding.snapshot != null
                                        && binding.snapshot.isReady()
                                        && binding.metadataContexts.values().stream()
                                                .allMatch(context ->
                                                        context.snapshot != null && context.snapshot.isReady()),
                                binding.lastError,
                                binding.metadataSuccesses,
                                binding.metadataFailures,
                                metadataFailures(binding),
                                metadataFailureCauses(binding))));
                result.complete(Collections.unmodifiableMap(statuses));
            });
        } catch (RejectedExecutionException closed) {
            result.complete(Collections.emptyMap());
        }
        return result;
    }

    public long getDiscardedMetadataResults() {
        return discardedMetadataResults.get();
    }

    @Override
    public List<ServiceInstance> getInstances(String serviceName) {
        Binding binding = bindings.get(serviceName);
        return binding == null ? Collections.emptyList() : binding.instances;
    }

    @Override
    public ServiceInstancesChangedListener createListener(Set<String> serviceNames) {
        return new XdsServiceInstancesChangedListener(serviceNames, this);
    }

    @Override
    public void addServiceInstancesChangedListener(ServiceInstancesChangedListener listener) {
        instanceListeners.add(listener);
    }

    @Override
    public void removeServiceInstancesChangedListener(ServiceInstancesChangedListener listener) {
        instanceListeners.remove(listener);
        execute(this::removeUnusedBindings);
    }

    @Override
    public long getDelay() {
        return 0;
    }

    @Override
    protected void doRegister(ServiceInstance instance) {
        /* Kubernetes owns remote workload registration. */
    }

    @Override
    protected void doUnregister(ServiceInstance instance) {
        /* Preserve the local metadata lifecycle in the superclass. */
    }

    private static void close(Binding binding) {
        if (binding == null) {
            return;
        }
        binding.snapshot = null;
        if (binding.graph != null) {
            binding.graph.close();
        }
        binding.metadataContexts.values().forEach(context -> context.graph.close());
        binding.metadataContexts.clear();
        binding.entries.values().forEach(entry -> {
            if (entry.task != null) {
                entry.task.cancel(true);
            }
        });
        binding.entries.clear();
    }

    @Override
    protected void doDestroy() throws Exception {
        certificateWatch.close();
        state.shutdownNow();
        try {
            state.awaitTermination(5, TimeUnit.SECONDS);
        } finally {
            bindings.values().forEach(XdsServiceDiscovery::close);
            bindings.clear();
            instanceListeners.clear();
            workers.shutdownNow();
            runtime.close();
        }
    }

    @Override
    public String toString() {
        return "Istio xDS ServiceDiscovery[targets=" + bindings.size() + "]";
    }

    private static Map<String, String> metadataFailures(Binding binding) {
        Map<String, String> failures = new LinkedHashMap<>();
        binding.entries.forEach((identity, entry) -> {
            if (entry.lastFailure != null) failures.put(identity, entry.lastFailure);
        });
        return Collections.unmodifiableMap(failures);
    }

    private static Map<String, List<String>> metadataFailureCauses(Binding binding) {
        Map<String, List<String>> causes = new LinkedHashMap<>();
        binding.entries.forEach((identity, entry) -> {
            if (!entry.failureCauses.isEmpty()) causes.put(identity, entry.failureCauses);
        });
        return Collections.unmodifiableMap(causes);
    }

    /** Type names only: messages and stack traces may contain credentials or metadata payloads. */
    static List<String> failureCauses(Throwable failure) {
        List<String> causes = new ArrayList<>();
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        while (failure != null && causes.size() < 8 && seen.add(failure)) {
            causes.add(failure.getClass().getName());
            failure = failure.getCause();
        }
        return Collections.unmodifiableList(causes);
    }

    private static final class Binding {
        final DiscoveryTarget target;
        final MetadataEntryConfig config;
        final Map<DiscoveryTarget, MetadataContext> metadataContexts = new LinkedHashMap<>();
        final Map<String, Entry> entries = new LinkedHashMap<>();
        final Set<NotifyListener> owners = ConcurrentHashMap.newKeySet();
        XdsResourceGraph graph;
        volatile Snapshot snapshot;
        volatile String lastError;
        long metadataSuccesses;
        long metadataFailures;
        volatile List<ServiceInstance> instances = Collections.emptyList();

        Binding(DiscoveryTarget target, MetadataEntryConfig config) {
            this.target = target;
            this.config = config;
        }
    }

    private static final class MetadataContext {
        XdsResourceGraph graph;
        volatile Snapshot snapshot;
    }

    private static final class Entry {
        final MetadataEntryOptions options;
        XdsEndpoint endpoint;
        XdsEndpoint metadataEndpoint;
        MetadataInfo metadata;
        Future<?> task;
        long expires;
        long nextAttempt;
        int failures;
        String lastFailure;
        List<String> failureCauses = Collections.emptyList();

        Entry(XdsEndpoint endpoint, XdsEndpoint metadataEndpoint, MetadataEntryOptions options) {
            this.endpoint = endpoint;
            this.metadataEndpoint = metadataEndpoint;
            this.options = options;
        }
    }

    public static final class TargetStatus {
        private final int discoveredEndpoints;
        private final int availableInstances;
        private final boolean graphReady;
        private final String lastError;
        private final long metadataSuccesses;
        private final long metadataFailures;
        private final Map<String, String> metadataFailureStatuses;
        private final Map<String, List<String>> metadataFailureCauses;

        TargetStatus(
                int discoveredEndpoints,
                int availableInstances,
                boolean graphReady,
                String lastError,
                long metadataSuccesses,
                long metadataFailures,
                Map<String, String> metadataFailureStatuses,
                Map<String, List<String>> metadataFailureCauses) {
            this.discoveredEndpoints = discoveredEndpoints;
            this.availableInstances = availableInstances;
            this.graphReady = graphReady;
            this.lastError = lastError;
            this.metadataSuccesses = metadataSuccesses;
            this.metadataFailures = metadataFailures;
            this.metadataFailureStatuses = metadataFailureStatuses;
            this.metadataFailureCauses = metadataFailureCauses;
        }

        public long getMetadataSuccesses() {
            return metadataSuccesses;
        }

        public long getMetadataFailures() {
            return metadataFailures;
        }

        public Map<String, String> getMetadataFailureStatuses() {
            return metadataFailureStatuses;
        }

        public Map<String, List<String>> getMetadataFailureCauses() {
            return metadataFailureCauses;
        }

        public int getDiscoveredEndpoints() {
            return discoveredEndpoints;
        }

        public int getAvailableInstances() {
            return availableInstances;
        }

        public boolean isGraphReady() {
            return graphReady;
        }

        public String getLastError() {
            return lastError;
        }
    }
}
