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
package org.apache.dubbo.xds.test;

import org.apache.dubbo.common.stream.ClientCallStreamObserver;
import org.apache.dubbo.common.stream.ClientResponseObserver;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.client.ServiceDiscoveryRegistry;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.support.RegistryManager;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.xds.bootstrap.Bootstrapper;
import org.apache.dubbo.xds.bootstrap.Bootstrapper.BootstrapInfo;
import org.apache.dubbo.xds.client.XdsClient;
import org.apache.dubbo.xds.client.XdsResourceType;
import org.apache.dubbo.xds.client.XdsRuntimePool;
import org.apache.dubbo.xds.registry.XdsServiceDiscovery;
import org.apache.dubbo.xds.registry.XdsServiceInstance;
import org.apache.dubbo.xds.security.TlsPolicy;
import org.apache.dubbo.xds.test.stream.NativeStreams;
import org.apache.dubbo.xds.traffic.XdsCircuitBreaker;

import java.io.IOException;
import java.io.Reader;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import com.alibaba.fastjson2.JSON;
import com.google.protobuf.Duration;
import com.google.protobuf.Message;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.cluster.v3.Cluster;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.ClusterLoadAssignment;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.LocalityLbEndpoints;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RouteAction;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RouteConfiguration;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.transport_sockets.tls.v3.SubjectAltNameMatcher;
import io.grpc.xds.shaded.io.envoyproxy.envoy.type.matcher.v3.StringMatcher;

/** Agent-only provider/consumer started entirely through normal Dubbo configuration APIs. */
public final class NativeXdsApplication {
    private static final String GROUP = "native-xds";
    private static final String VERSION = "1.0.0";
    private static final int TRIPLE_PORT = 17070;
    private static final int BINARY_PORT = 20880;
    private static final String PROVIDER_APP = "native-xds-provider";

    private NativeXdsApplication() {}

    /** Diagnostics use the same process default as normal Registry initialization. */
    static BootstrapInfo diagnosticBootstrap() throws IOException {
        return Bootstrapper.loadDefault(30_000);
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2 || args.length > 3) {
            throw new IllegalArgumentException(
                    "Usage: NativeXdsApplication provider|rotation-provider <identity> [runtime-dir] | consumer|security <service-fqdn> [runtime-dir] | prepare-rotation|prepare-root-rotation unused [runtime-dir]");
        }
        Path runtime = Paths.get(args.length == 3 ? args[2] : "/runtime");
        Files.createDirectories(runtime);
        if ("prepare-rotation".equals(args[0])) NativeRotationProbe.prepare(runtime);
        else if ("prepare-root-rotation".equals(args[0])) NativeRotationProbe.prepareRoot(runtime);
        else if ("security".equals(args[0])) NativeSecurityProbe.run(args[1], runtime);
        else if ("provider".equals(args[0]) || "rotation-provider".equals(args[0]))
            provider(args[1], runtime, "rotation-provider".equals(args[0]));
        else if ("consumer".equals(args[0])) consumer(args[1], runtime);
        else throw new IllegalArgumentException("Unknown application role");
    }

    private static String environment(String name, String fallback) {
        String value = System.getenv(name);
        return value == null ? fallback : value;
    }

    private static int port(String name, int fallback) {
        int value = Integer.parseInt(environment(name, Integer.toString(fallback)));
        if (value < 1 || value > 65535) throw new IllegalArgumentException("Invalid native test port: " + name);
        return value;
    }

    private static RegistryConfig registry() {
        RegistryConfig registry = new RegistryConfig("xds://localhost");
        registry.setRegisterMode("instance");
        registry.setUseAsConfigCenter(false);
        registry.setUseAsMetadataCenter(false);
        Map<String, String> parameters = new HashMap<>();
        parameters.put("registry-type", "service");
        parameters.put("file-cache", "false");
        String metadataTtl = System.getenv("NATIVE_XDS_METADATA_TTL");
        String metadataRefresh = System.getenv("NATIVE_XDS_METADATA_REFRESH");
        if (metadataTtl != null) parameters.put("xds.metadata.ttl", metadataTtl);
        if (metadataRefresh != null) parameters.put("xds.metadata.refresh", metadataRefresh);
        registry.setParameters(parameters);
        return registry;
    }

    private static MetricsConfig metrics() {
        MetricsConfig metrics = new MetricsConfig();
        metrics.setExportMetricsService(false);
        return metrics;
    }

    private static void provider(String identity, Path runtime, boolean observeRotation) throws Exception {
        ApplicationConfig application = new ApplicationConfig(environment("NATIVE_XDS_PROVIDER_APP", PROVIDER_APP));
        int triplePort = port("NATIVE_XDS_TRIPLE_PORT", TRIPLE_PORT);
        int binaryPort = port("NATIVE_XDS_BINARY_PORT", BINARY_PORT);
        int metadataPort = port("NATIVE_XDS_METADATA_PORT", triplePort);
        application.setOnlyUseMetadataV2(false);
        application.setMetadataServiceProtocol("tri");
        application.setMetadataServicePort(metadataPort);
        ServiceConfig<NativeEcho> service = new ServiceConfig<>();
        service.setInterface(NativeEcho.class);
        service.setGroup(GROUP);
        service.setVersion(VERSION);
        service.setRef(new NativeEchoProvider(identity, runtime));
        ProtocolConfig tripleProtocol = new ProtocolConfig("tri", triplePort);
        DubboBootstrap bootstrap = DubboBootstrap.newInstance()
                .application(application)
                .registry(registry())
                .metrics(metrics())
                .protocol(tripleProtocol)
                .protocol(new ProtocolConfig("dubbo", binaryPort))
                .service(service);
        if (Boolean.parseBoolean(environment("NATIVE_XDS_STREAMS", "false"))) {
            ServiceConfig<NativeStreams> streams = new ServiceConfig<>();
            streams.setInterface(NativeStreams.class);
            streams.setGroup(GROUP);
            streams.setVersion(VERSION);
            streams.setRef(new NativeStreamsProvider(identity, runtime));
            streams.setProtocols(Collections.singletonList(tripleProtocol));
            bootstrap.service(streams);
        }
        bootstrap.start();
        if (Boolean.parseBoolean(environment("NATIVE_XDS_LISTENER_CONTROL", "false")))
            NativeListenerProbe.watch(runtime, triplePort, metadataPort);
        if (observeRotation) NativeRotationProbe.watchProvider(runtime);
        Files.write(runtime.resolve("provider-ready"), Collections.singletonList(identity), StandardCharsets.UTF_8);
        System.out.println("NATIVE_XDS_PROVIDER_READY identity=" + identity);
        bootstrap.await();
    }

    private static ReferenceConfig<NativeEcho> reference(String service, String protocol, int port) {
        return reference(NativeEcho.class, service, protocol, port);
    }

    private static <T> ReferenceConfig<T> reference(Class<T> type, String service, String protocol, int port) {
        ReferenceConfig<T> reference = new ReferenceConfig<>();
        reference.setInterface(type);
        reference.setGroup(GROUP);
        reference.setVersion(VERSION);
        reference.setProtocol(protocol);
        reference.setProvidedBy(service);
        reference.setInjvm(false);
        reference.setCheck(false);
        reference.setTimeout(3000);
        reference.setRetries(0);
        Map<String, String> parameters = new HashMap<>();
        parameters.put("provider-port", Integer.toString(port));
        parameters.put("xds.metadata.application", environment("NATIVE_XDS_PROVIDER_APP", PROVIDER_APP));
        String metadataPort = System.getenv("NATIVE_XDS_METADATA_SERVICE_PORT");
        if (metadataPort != null) parameters.put("xds.metadata.service-port", metadataPort);
        String targets = System.getenv("NATIVE_XDS_METADATA_TARGETS");
        if (targets != null) parameters.put("xds.metadata.targets", targets);
        if ("dubbo".equals(protocol)) {
            parameters.put("xds.metadata.protocol", "tri");
            parameters.put("xds.metadata.service-port", metadataPort == null ? "7070" : metadataPort);
        }
        reference.setParameters(parameters);
        return reference;
    }

    private static void consumer(String service, Path runtime) throws Exception {
        ReferenceConfig<NativeEcho> triple = reference(service, "tri", 7070);
        ReferenceConfig<NativeEcho> binary = reference(service, "dubbo", 7071);
        DubboBootstrap bootstrap = DubboBootstrap.newInstance()
                .application(new ApplicationConfig("native-xds-consumer"))
                .registry(registry())
                .metrics(metrics())
                .reference(triple)
                .reference(binary);
        ReferenceConfig<NativeStreams> streams = null;
        if (Boolean.parseBoolean(environment("NATIVE_XDS_STREAMS", "false"))) {
            streams = reference(NativeStreams.class, service, "tri", 7070);
            bootstrap.reference(streams);
        }
        bootstrap.start();
        Path commands = runtime.resolve("commands");
        Files.createDirectories(commands);
        Files.write(runtime.resolve("consumer-ready"), Collections.singletonList("ready"), StandardCharsets.UTF_8);
        System.out.println("NATIVE_XDS_CONSUMER_READY");
        try {
            while (!Thread.currentThread().isInterrupted()) {
                try (DirectoryStream<Path> files = Files.newDirectoryStream(commands, "*.request")) {
                    for (Path request : files) {
                        String name = request.getFileName().toString().replaceFirst("\\.request$", "");
                        if (!name.matches("[A-Za-z0-9_-]{1,64}")) continue;
                        Map<String, Object> result;
                        try {
                            if (Files.size(request) > 65536) throw new IllegalArgumentException("Request is too large");
                            Properties properties = new Properties();
                            try (Reader reader = Files.newBufferedReader(request, StandardCharsets.UTF_8)) {
                                properties.load(reader);
                            }
                            result = execute(
                                    properties,
                                    triple.get(),
                                    binary.get(),
                                    streams == null ? null : streams.get(),
                                    bootstrap.getApplicationModel());
                        } catch (Exception error) {
                            result = new LinkedHashMap<>();
                            result.put(
                                    "status", TriRpcStatus.getStatus(error).code.name());
                            result.put("error_type", error.getClass().getSimpleName());
                        }
                        Path temporary = commands.resolve(name + ".result.tmp");
                        Files.write(temporary, JSON.toJSONString(result).getBytes(StandardCharsets.UTF_8));
                        Files.move(temporary, commands.resolve(name + ".json"), StandardCopyOption.REPLACE_EXISTING);
                        Files.delete(request);
                    }
                }
                Thread.sleep(50);
            }
        } finally {
            NativeInFlightProbe.close();
            try {
                NativeRootPeerProbe.close();
            } finally {
                bootstrap.destroy();
            }
        }
    }

    private static Map<String, Object> execute(
            Properties command,
            NativeEcho triple,
            NativeEcho binary,
            NativeStreams streams,
            ApplicationModel application)
            throws Exception {
        String method = command.getProperty("method", "echo");
        if (method.startsWith("hold-")) return NativeInFlightProbe.execute(command, triple, streams);
        if (method.startsWith("root-peer-")) return NativeRootPeerProbe.execute(command, application);
        if ("inspect".equals(method)) return inspect(application);
        if ("new-registry".equals(method)) return newRegistry(command, application);
        if ("certificates".equals(method)) return NativeRotationProbe.snapshot(application);
        if ("tls-peer".equals(method)) return NativeSecurityProbe.peer(command);
        if ("reconnect".equals(method))
            return NativeLifecycleProbe.reconnect(
                    application, command.getProperty("protocol", "ads"), command.getProperty("key", ""));
        if ("admission".equals(method)) return admission(command, triple);
        String protocol = command.getProperty("protocol", "tri");
        if (!Arrays.asList("tri", "dubbo").contains(protocol)) throw new IllegalArgumentException("Invalid protocol");
        if (!Arrays.asList("echo", "async", "stream").contains(method))
            throw new IllegalArgumentException("Invalid method");
        if ("stream".equals(method) && !"tri".equals(protocol))
            throw new IllegalArgumentException("Streaming requires Triple");
        int count = Integer.parseInt(command.getProperty("count", "1"));
        int timeout = Integer.parseInt(command.getProperty("timeout", "3000"));
        if (count < 1 || count > 1000 || timeout < 1 || timeout > 30000)
            throw new IllegalArgumentException("Invalid count or timeout");
        String message = command.getProperty("message", "hello");
        String key = command.getProperty("key", "native-test");
        NativeEcho remote = "tri".equals(protocol) ? triple : binary;
        List<String> values = new ArrayList<>();
        Map<String, Integer> errors = new LinkedHashMap<>();
        List<String> errorTypes = new ArrayList<>();
        long start = System.nanoTime();
        for (int i = 0; i < count; i++) {
            RpcContext.getClientAttachment().setAttachment("x-user", key);
            RpcContext.getClientAttachment().setAttachment("timeout", timeout);
            try {
                if ("echo".equals(method)) values.add(remote.echo(message));
                else if ("async".equals(method))
                    values.add(remote.asyncEcho(message).get(timeout + 1000L, TimeUnit.MILLISECONDS));
                else values.addAll(stream(remote, message, timeout));
            } catch (Exception error) {
                Throwable cause = error;
                while ((cause instanceof ExecutionException || cause instanceof CompletionException)
                        && cause.getCause() != null) {
                    cause = cause.getCause();
                }
                errors.merge(TriRpcStatus.getStatus(cause).code.name(), 1, Integer::sum);
                errorTypes.add(error.getClass().getSimpleName() + ":"
                        + cause.getClass().getSimpleName());
            } finally {
                RpcContext.removeContext();
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", errors.isEmpty() ? "OK" : "RPC_ERROR");
        result.put("protocol", protocol);
        result.put("method", method);
        result.put("calls", count);
        result.put("values", values);
        result.put("errors", errors);
        if (!errorTypes.isEmpty()) result.put("error_types", errorTypes);
        result.put("elapsed_ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
        return result;
    }

    private static List<String> stream(NativeEcho remote, String message, int timeout) throws Exception {
        List<String> values = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch done = new CountDownLatch(1);
        AtomicInteger terminals = new AtomicInteger();
        CompletableFuture<Void> result = new CompletableFuture<>();
        remote.watch(message, new ClientResponseObserver<String, String>() {
            public void beforeStart(ClientCallStreamObserver<String> writer) {}

            public void onNext(String value) {
                values.add(value);
            }

            public void onError(Throwable error) {
                terminals.incrementAndGet();
                result.completeExceptionally(error);
                done.countDown();
            }

            public void onCompleted() {
                terminals.incrementAndGet();
                result.complete(null);
                done.countDown();
            }
        });
        if (!done.await(timeout + 1000L, TimeUnit.MILLISECONDS)) throw TriRpcStatus.DEADLINE_EXCEEDED.asException();
        result.get();
        if (terminals.get() != 1) throw new IllegalStateException("Stream has multiple terminal events");
        return new ArrayList<>(values);
    }

    private static Map<String, Object> newRegistry(Properties command, ApplicationModel original) throws Exception {
        String service = command.getProperty("service");
        if (service == null || service.isEmpty()) throw new IllegalArgumentException("Missing test Service FQDN");
        FrameworkModel framework = new FrameworkModel();
        DubboBootstrap fresh = DubboBootstrap.newInstance(framework);
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            ReferenceConfig<NativeEcho> triple = reference(service, "tri", 7070);
            ReferenceConfig<NativeEcho> binary = reference(service, "dubbo", 7071);
            fresh.application(new ApplicationConfig("native-xds-new-registry"))
                    .registry(registry())
                    .metrics(metrics())
                    .reference(triple)
                    .reference(binary)
                    .start();
            NativeEcho tri = triple.get();
            NativeEcho dubbo = binary.get();
            await(
                    () -> {
                        try {
                            Map<?, ?> targets = (Map<?, ?>)
                                    inspect(fresh.getApplicationModel()).get("targets");
                            return targets.size() >= 2
                                    && targets.values().stream()
                                            .allMatch(value ->
                                                    ((Number) ((Map<?, ?>) value).get("available_instances")).intValue()
                                                            > 0);
                        } catch (Exception error) {
                            return false;
                        }
                    },
                    30_000);
            result.put("application_model_distinct", fresh.getApplicationModel() != original);
            result.put("framework_model_distinct", framework != original.getFrameworkModel());
            result.put("runtime_count_during", XdsRuntimePool.activeRuntimeCount());
            result.put("triple_echo", tri.echo("native-new-registry"));
            result.put("triple_async", tri.asyncEcho("native-new-registry").get(4, TimeUnit.SECONDS));
            result.put("triple_stream", stream(tri, "native-new-registry", 3000));
            result.put("binary_echo", dubbo.echo("native-new-registry"));
        } finally {
            RpcContext.removeContext();
            fresh.destroy();
            framework.destroy();
        }
        result.put("runtime_count_after", XdsRuntimePool.activeRuntimeCount());
        result.put("status", "OBSERVED");
        return result;
    }

    private static Map<String, Object> inspect(ApplicationModel application) throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("process_start_ms", ManagementFactory.getRuntimeMXBean().getStartTime());
        BootstrapInfo bootstrap = diagnosticBootstrap();
        try (XdsRuntimePool.Lease lease = XdsRuntimePool.acquire(bootstrap)) {
            result.put("connection", lease.getClient().getConnectionState());
            result.put("runtime_count", XdsRuntimePool.activeRuntimeCount());
            Map<String, Object> resources = new LinkedHashMap<>();
            lease.getClient().getResourceMetadata().forEach((name, metadata) -> {
                Map<String, Object> state = new LinkedHashMap<>();
                state.put("status", metadata.getStatus().name());
                state.put("version", metadata.getVersion());
                state.put("accepted_at", metadata.getAcceptedAt());
                state.put("rejection", metadata.getRejection());
                resources.put(name, state);
            });
            result.put("resources", resources);
            Map<String, Object> clusters = new LinkedHashMap<>();
            String prefix = XdsResourceType.CDS.getTypeUrl() + ":";
            for (String resource : resources.keySet()) {
                if (!resource.startsWith(prefix)) continue;
                String name = resource.substring(prefix.length());
                Cluster cluster = accepted(lease.getClient(), XdsResourceType.CDS, name);
                if (cluster == null) continue;
                Map<String, Object> policy = new LinkedHashMap<>();
                policy.put("lb_policy", cluster.getLbPolicy().name());
                policy.put("max_requests", XdsCircuitBreaker.limit(cluster));
                policy.put("active_requests", XdsCircuitBreaker.active(bootstrap.getFingerprint(), cluster));
                policy.put("tls", tlsDescription(TlsPolicy.client(cluster, bootstrap)));
                if (cluster.hasRingHashLbConfig()) {
                    if (cluster.getRingHashLbConfig().hasMinimumRingSize())
                        policy.put(
                                "ring_minimum",
                                cluster.getRingHashLbConfig()
                                        .getMinimumRingSize()
                                        .getValue());
                    if (cluster.getRingHashLbConfig().hasMaximumRingSize())
                        policy.put(
                                "ring_maximum",
                                cluster.getRingHashLbConfig()
                                        .getMaximumRingSize()
                                        .getValue());
                }
                clusters.put(name, policy);
            }
            result.put("accepted_clusters", clusters);
            Map<String, Object> endpoints = new LinkedHashMap<>();
            prefix = XdsResourceType.EDS.getTypeUrl() + ":";
            for (String resource : resources.keySet()) {
                if (!resource.startsWith(prefix)) continue;
                String name = resource.substring(prefix.length());
                ClusterLoadAssignment assignment = accepted(lease.getClient(), XdsResourceType.EDS, name);
                if (assignment == null) continue;
                int count = assignment.getEndpointsList().stream()
                        .mapToInt(locality -> locality.getLbEndpointsCount())
                        .sum();
                Map<String, Object> state = new LinkedHashMap<>();
                state.put("endpoint_count", count);
                List<Map<String, Object>> localities = new ArrayList<>();
                for (LocalityLbEndpoints locality : assignment.getEndpointsList()) {
                    Map<String, Object> group = new LinkedHashMap<>();
                    group.put("region", locality.getLocality().getRegion());
                    group.put("zone", locality.getLocality().getZone());
                    group.put("sub_zone", locality.getLocality().getSubZone());
                    group.put("priority", Integer.toUnsignedLong(locality.getPriority()));
                    group.put("endpoint_count", locality.getLbEndpointsCount());
                    group.put(
                            "weight",
                            locality.hasLoadBalancingWeight()
                                    ? Integer.toUnsignedLong(
                                            locality.getLoadBalancingWeight().getValue())
                                    : null);
                    Map<String, Integer> health = new LinkedHashMap<>();
                    locality.getLbEndpointsList()
                            .forEach(endpoint ->
                                    health.merge(endpoint.getHealthStatus().name(), 1, Integer::sum));
                    group.put("health_counts", health);
                    localities.add(group);
                }
                state.put("localities", localities);
                endpoints.put(name, state);
            }
            result.put("accepted_endpoints", endpoints);
            Map<String, Object> routes = new LinkedHashMap<>();
            prefix = XdsResourceType.RDS.getTypeUrl() + ":";
            for (String resource : resources.keySet()) {
                if (!resource.startsWith(prefix)) continue;
                String name = resource.substring(prefix.length());
                RouteConfiguration configuration = accepted(lease.getClient(), XdsResourceType.RDS, name);
                if (configuration == null) continue;
                List<Map<String, Object>> policies = new ArrayList<>();
                configuration.getVirtualHostsList().forEach(host -> host.getRoutesList()
                        .forEach(route -> {
                            if (!route.hasRoute()) return;
                            RouteAction action = route.getRoute();
                            Map<String, Object> policy = new LinkedHashMap<>();
                            policy.put("name", route.getName());
                            if (action.hasTimeout()) policy.put("timeout_ms", millis(action.getTimeout()));
                            if (action.hasMaxStreamDuration()) {
                                if (action.getMaxStreamDuration().hasMaxStreamDuration())
                                    policy.put(
                                            "max_stream_duration_ms",
                                            millis(action.getMaxStreamDuration().getMaxStreamDuration()));
                                if (action.getMaxStreamDuration().hasGrpcTimeoutHeaderMax())
                                    policy.put(
                                            "grpc_timeout_header_max_ms",
                                            millis(action.getMaxStreamDuration().getGrpcTimeoutHeaderMax()));
                            }
                            if (action.hasRetryPolicy()) {
                                policy.put("retry_on", action.getRetryPolicy().getRetryOn());
                                policy.put(
                                        "num_retries",
                                        action.getRetryPolicy().getNumRetries().getValue());
                            }
                            policy.put(
                                    "has_fault_config",
                                    route.getTypedPerFilterConfigMap().containsKey("envoy.filters.http.fault"));
                            policies.add(policy);
                        }));
                routes.put(name, policies);
            }
            result.put("accepted_routes", routes);
        }
        Map<String, Object> targets = new LinkedHashMap<>();
        RegistryManager manager = application.getBeanFactory().getBean(RegistryManager.class);
        for (Registry registry : manager.getRegistries()) {
            if (!(registry instanceof ServiceDiscoveryRegistry)) continue;
            if (!(((ServiceDiscoveryRegistry) registry).getServiceDiscovery() instanceof XdsServiceDiscovery)) continue;
            XdsServiceDiscovery discovery =
                    (XdsServiceDiscovery) ((ServiceDiscoveryRegistry) registry).getServiceDiscovery();
            List<Map<String, Object>> loadStats = new ArrayList<>();
            discovery.inspectLoadStats().forEach(stats -> {
                Map<String, Object> value = new LinkedHashMap<>();
                value.put("cluster", stats.getCluster());
                value.put("eds_service", stats.getService());
                value.put("subscribed", stats.isSubscribed());
                value.put("issued", stats.getIssued());
                value.put("in_progress", stats.getInProgress());
                value.put("successful", stats.getSuccessful());
                value.put("errors", stats.getErrors());
                value.put("circuit_drops", stats.getCircuitDrops());
                value.put("eds_drops", stats.getEdsDrops());
                loadStats.add(value);
            });
            result.put("load_stats", loadStats);
            result.put("load_reporting", discovery.inspectLoadReporting());
            Map<String, Object> certificates = new LinkedHashMap<>();
            discovery.inspectCertificateProviders().forEach((name, status) -> {
                Map<String, Object> value = new LinkedHashMap<>();
                value.put("state", status.getState().name());
                value.put("observed_at_ms", status.getObservedAtMillis());
                value.put("identity_not_before_ms", status.getIdentityNotBeforeMillis());
                value.put("identity_not_after_ms", status.getIdentityNotAfterMillis());
                value.put("last_successful_refresh_ms", status.getLastSuccessfulRefreshMillis());
                value.put("last_refresh_error", status.getLastRefreshError());
                certificates.put(name, value);
            });
            result.put("certificate_providers", certificates);
            discovery.inspectTargets().get(2, TimeUnit.SECONDS).forEach((name, status) -> {
                Map<String, Object> state = new LinkedHashMap<>();
                state.put("discovered_endpoints", status.getDiscoveredEndpoints());
                state.put("available_instances", status.getAvailableInstances());
                state.put("graph_ready", status.isGraphReady());
                state.put("last_error", status.getLastError());
                state.put("metadata_successes", status.getMetadataSuccesses());
                state.put("metadata_failures", status.getMetadataFailures());
                state.put("metadata_failure_statuses", status.getMetadataFailureStatuses());
                state.put("metadata_failure_causes", status.getMetadataFailureCauses());
                List<Map<String, Object>> instances = new ArrayList<>();
                for (ServiceInstance instance : discovery.getInstances(name)) {
                    Map<String, Object> value = new LinkedHashMap<>();
                    value.put("host", instance.getHost());
                    value.put("workload_port", instance.getPort());
                    value.put("application", instance.getServiceMetadata().getApp());
                    value.put(
                            "cluster",
                            ((XdsServiceInstance) instance)
                                    .getEndpoint()
                                    .getCluster()
                                    .getName());
                    instances.add(value);
                }
                state.put("instances", instances);
                targets.put(name, state);
            });
        }
        result.put("targets", targets);
        return result;
    }

    private static Map<String, Object> admission(Properties command, NativeEcho remote) throws Exception {
        String token = command.getProperty("message", "");
        String name = command.getProperty("key", "");
        if (!token.matches("[A-Za-z0-9_-]{1,55}")) throw new IllegalArgumentException("Invalid admission token");
        BootstrapInfo bootstrap = diagnosticBootstrap();
        Map<String, Object> result = new LinkedHashMap<>();
        List<CompletableFuture<String>> calls = new ArrayList<>();
        try (XdsRuntimePool.Lease lease = XdsRuntimePool.acquire(bootstrap)) {
            if (!lease.getClient().getResourceMetadata().containsKey(XdsResourceType.CDS.getTypeUrl() + ":" + name))
                throw new IllegalArgumentException("Admission cluster must already be subscribed");
            Cluster cluster = accepted(lease.getClient(), XdsResourceType.CDS, name);
            if (cluster == null || XdsCircuitBreaker.limit(cluster) != 2)
                throw new IllegalStateException("Admission fixture requires max_requests=2");
            String workload = bootstrap.getFingerprint();
            await(() -> XdsCircuitBreaker.active(workload, cluster) == 0, 1000);
            for (int i = 0; i < 2; i++) calls.add(startProbe(remote, token, 1500));
            await(() -> XdsCircuitBreaker.active(workload, cluster) == 2, 500);
            result.put("active_at_saturation", XdsCircuitBreaker.active(workload, cluster));
            long before = XdsCircuitBreaker.dropped(workload, cluster);
            long start = System.nanoTime();
            result.put("overflow", outcome(startProbe(remote, token + "-overflow", 0), 1000));
            result.put("overflow_elapsed_ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
            result.put("drop_delta", XdsCircuitBreaker.dropped(workload, cluster) - before);
            result.put("active_after_drop", XdsCircuitBreaker.active(workload, cluster));
            List<Map<String, Object>> completed = new ArrayList<>();
            for (CompletableFuture<String> call : calls) completed.add(outcome(call, 5000));
            result.put("admitted", completed);
            await(() -> XdsCircuitBreaker.active(workload, cluster) == 0, 500);
            result.put("active_after_completion", XdsCircuitBreaker.active(workload, cluster));
            result.put("recovery", outcome(startProbe(remote, token, 0), 1000));
            await(() -> XdsCircuitBreaker.active(workload, cluster) == 0, 500);
            result.put("active_after_recovery", XdsCircuitBreaker.active(workload, cluster));
            result.put("status", "OBSERVED");
        } finally {
            calls.forEach(call -> {
                if (!call.isDone()) call.cancel(false);
            });
            RpcContext.removeContext();
        }
        return result;
    }

    private static CompletableFuture<String> startProbe(NativeEcho remote, String token, int delay) {
        RpcContext.getClientAttachment().setAttachment("x-user", "native-admission");
        RpcContext.getClientAttachment().setAttachment("timeout", 5000);
        try {
            return remote.asyncEcho("probe:" + token + ":" + delay + ":0");
        } catch (Exception error) {
            CompletableFuture<String> failed = new CompletableFuture<>();
            failed.completeExceptionally(error);
            return failed;
        } finally {
            RpcContext.removeContext();
        }
    }

    private static Map<String, Object> outcome(CompletableFuture<String> call, int timeout) throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            result.put("value", call.get(timeout, TimeUnit.MILLISECONDS));
            result.put("status", "OK");
        } catch (ExecutionException error) {
            Throwable cause = error.getCause();
            while (cause instanceof CompletionException && cause.getCause() != null) cause = cause.getCause();
            result.put("status", TriRpcStatus.getStatus(cause).code.name());
            result.put("error_type", cause.getClass().getSimpleName());
        }
        return result;
    }

    private static void await(BooleanSupplier ready, int timeout) throws Exception {
        long end = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeout);
        while (!ready.getAsBoolean()) {
            if (System.nanoTime() >= end) throw new IllegalStateException("Admission state did not converge");
            Thread.sleep(5);
        }
    }

    private static long millis(Duration duration) {
        return TimeUnit.SECONDS.toMillis(duration.getSeconds()) + TimeUnit.NANOSECONDS.toMillis(duration.getNanos());
    }

    static Map<String, Object> tlsDescription(TlsPolicy policy) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("enabled", policy.isTls());
        result.put("server_name", policy.getServerName());
        List<Map<String, Object>> matchers = new ArrayList<>();
        for (StringMatcher matcher : policy.getValidation().getMatchSubjectAltNamesList()) {
            matchers.add(sanMatcher("ANY", matcher));
        }
        for (SubjectAltNameMatcher matcher : policy.getValidation().getMatchTypedSubjectAltNamesList()) {
            matchers.add(sanMatcher(matcher.getSanType().name(), matcher.getMatcher()));
        }
        result.put("san_matchers", matchers);
        return result;
    }

    private static Map<String, Object> sanMatcher(String type, StringMatcher matcher) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("san_type", type);
        result.put("pattern_type", matcher.getMatchPatternCase().name());
        result.put("ignore_case", matcher.getIgnoreCase());
        switch (matcher.getMatchPatternCase()) {
            case EXACT:
                result.put("pattern", matcher.getExact());
                break;
            case PREFIX:
                result.put("pattern", matcher.getPrefix());
                break;
            case SUFFIX:
                result.put("pattern", matcher.getSuffix());
                break;
            case CONTAINS:
                result.put("pattern", matcher.getContains());
                break;
            case SAFE_REGEX:
                result.put("pattern", matcher.getSafeRegex().getRegex());
                break;
            default:
                throw new IllegalArgumentException("Unsupported accepted SAN matcher");
        }
        return result;
    }

    private static <T extends Message> T accepted(XdsClient client, XdsResourceType<T> type, String name)
            throws Exception {
        CompletableFuture<T> accepted = new CompletableFuture<>();
        try (AutoCloseable watch = client.watch(type, name, new XdsClient.ResourceWatcher<T>() {
            public void onChanged(T resource) {
                accepted.complete(resource);
            }

            public void onAbsent() {
                accepted.complete(null);
            }

            public void onError(Throwable error) {
                // The separate resource status retains the controlled NACK reason.
            }
        })) {
            return accepted.get(2, TimeUnit.SECONDS);
        }
    }
}
