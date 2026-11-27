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
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.remoting.RemotingServer;
import org.apache.dubbo.remoting.api.connection.AbstractConnectionClient;
import org.apache.dubbo.remoting.api.connection.ConnectionStateProvider;
import org.apache.dubbo.remoting.api.connection.ConnectionStateProvider.State;
import org.apache.dubbo.remoting.exchange.PortUnificationExchanger;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.xds.client.XdsResourceType;
import org.apache.dubbo.xds.registry.XdsRetryTest.Business;
import org.apache.dubbo.xds.registry.XdsRetryTest.Fixture;
import org.apache.dubbo.xds.registry.XdsRetryTest.Request;
import org.apache.dubbo.xds.security.NativeTlsAdapter;
import org.apache.dubbo.xds.traffic.XdsCircuitBreaker;
import org.apache.dubbo.xds.traffic.XdsLoadBalance;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

import com.google.protobuf.UInt32Value;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.cluster.v3.Cluster;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.HealthStatus;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.Locality;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.ClusterLoadAssignment;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.LbEndpoint;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.endpoint.v3.LocalityLbEndpoints;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RouteConfiguration;
import io.netty.channel.Channel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class XdsPriorityTest {
    @Test
    void connectingCallsConsumeBudgetAndCancelWithoutDispatchOrAdmission() throws Exception {
        try (Fixture f = new Fixture()) {
            XdsEndpoint primary = primary(f);
            noRetry(f, primary);
            XdsLoadBalance lb = XdsLeastRequestTest.loadBalance(f.consumer);
            try (PendingConnection pending = new PendingConnection(f, primary)) {
                CompletableFuture<String> deadline = callWithTimeout(f, "deadline", 180);
                XdsFaultExecutionTest.status(TriRpcStatus.Code.DEADLINE_EXCEEDED, deadline);
                assertEquals(State.CONNECTING, pending.connection.getConnectivityState());
                CompletableFuture<String> cancelled = callWithTimeout(f, "cancelled", 6000);
                assertTrue(cancelled.cancel(false));
                assertTrue(cancelled.isCancelled());
                XdsFaultExecutionTest.Recording stream = new XdsFaultExecutionTest.Recording();
                RpcContext.getClientAttachment().setAttachment("timeout", 180);
                try {
                    f.remote.watch("waiting-stream", stream);
                } finally {
                    RpcContext.removeClientAttachment();
                }
                assertTrue(stream.done.await(1, TimeUnit.SECONDS));
                assertEquals(1, stream.before.get());
                assertEquals(1, stream.terminals.get());
                assertEquals(TriRpcStatus.Code.DEADLINE_EXCEEDED, TriRpcStatus.getStatus(stream.error).code);
                XdsFaultExecutionTest.Recording cancelledStream = new XdsFaultExecutionTest.Recording();
                RpcContext.getClientAttachment().setAttachment("timeout", 6000);
                try {
                    f.remote.chat(cancelledStream).onNext("queued-message");
                } finally {
                    RpcContext.removeClientAttachment();
                }
                cancelledStream.writer.onError(TriRpcStatus.CANCELLED.asException());
                assertTrue(cancelledStream.done.await(1, TimeUnit.SECONDS));
                assertEquals(TriRpcStatus.Code.CANCELLED, TriRpcStatus.getStatus(cancelledStream.error).code);
                assertEquals(1, cancelledStream.terminals.get());
                XdsFaultExecutionTest.Recording destroyedStream = new XdsFaultExecutionTest.Recording();
                RpcContext.getClientAttachment().setAttachment("timeout", 6000);
                try {
                    f.remote.watch("destroyed-stream", destroyedStream);
                } finally {
                    RpcContext.removeClientAttachment();
                }
                CompletableFuture<String> destroyed = callWithTimeout(f, "destroyed", 6000);
                long start = System.nanoTime();
                f.consumer.destroy();
                assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < 800);
                XdsFaultExecutionTest.status(TriRpcStatus.Code.CANCELLED, destroyed);
                assertTrue(destroyedStream.done.await(1, TimeUnit.SECONDS));
                assertEquals(1, destroyedStream.terminals.get());
                assertEquals(TriRpcStatus.Code.CANCELLED, TriRpcStatus.getStatus(destroyedStream.error).code);
                assertEquals(0, lb.active(primary));
                assertEquals(0, XdsCircuitBreaker.active(f.workloadFingerprint, primary.getCluster()));
                assertEquals(0, f.blue.calls.get());
                assertEquals(0, f.green.calls.get());
                assertEquals(0, f.replacement.calls.get());
            }
        }
    }

    @Test
    void deletingAConnectingPriorityDispatchesItsWaitingCallToTheSelectedClusterFallback() throws Exception {
        try (Fixture f = new Fixture()) {
            XdsEndpoint primary = primary(f);
            noRetry(f, primary);
            LocalityLbEndpoints fallback = locality(f, f.replacementPort, "fallback", 1, 1, 1, HealthStatus.HEALTHY);
            publish(f, locality(f, primary.getPort(), "primary", 0, 1, 1, HealthStatus.HEALTHY), fallback);
            ports(f, primary.getPort(), f.replacementPort);
            try (PendingConnection pending = new PendingConnection(f, primary)) {
                CompletableFuture<String> waiting = callWithTimeout(f, "removed", 6000);
                Thread.sleep(100);
                assertFalse(waiting.isDone());
                assertEquals(0, f.replacement.calls.get());
                assertEquals(0, XdsCircuitBreaker.active(f.workloadFingerprint, primary.getCluster()));
                publish(
                        f,
                        locality(f, primary.getPort(), "primary", 0, 1, 1, HealthStatus.HEALTHY).toBuilder()
                                .clearLbEndpoints()
                                .build(),
                        fallback);
                assertEquals("replacement:removed", waiting.get(2, TimeUnit.SECONDS));
                assertEquals(0, f.blue.calls.get());
                assertEquals(0, f.green.calls.get());
                assertEquals(1, f.replacement.calls.get());
                assertTrue(f.replacement.timeouts.get(0) < 6000, "Waiting cannot restart the call budget");
            }
        }
    }

    @Test
    void connectingTransportLossFailsUnavailableBeforeRpcDeadlineWithoutDispatch() throws Exception {
        try (Fixture f = new Fixture()) {
            XdsEndpoint primary = primary(f);
            noRetry(f, primary);
            try (PendingConnection pending = new PendingConnection(f, primary)) {
                List<State> transitions = new CopyOnWriteArrayList<>();
                try (AutoCloseable watch = pending.connection.addConnectivityListener(transitions::add)) {
                    long start = System.nanoTime();
                    CompletableFuture<String> waiting = callWithTimeout(f, "transport-lost", 6000);
                    assertFalse(waiting.isDone());
                    pending.socket.close();
                    XdsFaultExecutionTest.status(TriRpcStatus.Code.UNAVAILABLE, waiting);
                    assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < 6000);
                    assertTrue(transitions.contains(State.TRANSIENT_FAILURE));
                    assertEquals(0, XdsCircuitBreaker.active(f.workloadFingerprint, primary.getCluster()));
                    assertEquals(0, XdsLeastRequestTest.loadBalance(f.consumer).active(primary));
                    assertEquals(0, f.blue.calls.get());
                    assertEquals(0, f.green.calls.get());
                    assertEquals(0, f.replacement.calls.get());
                }
            }
        }
    }

    private static CompletableFuture<String> callWithTimeout(Fixture f, String value, int timeout) {
        RpcContext.getClientAttachment().setAttachment("timeout", timeout);
        try {
            return f.remote.call(new Request(value));
        } finally {
            RpcContext.removeClientAttachment();
        }
    }

    private static void noRetry(Fixture f, XdsEndpoint endpoint) throws Exception {
        RouteConfiguration base = f.config(100, 6000);
        RouteConfiguration routes = base.toBuilder()
                .setVirtualHosts(
                        0,
                        base.getVirtualHosts(0).toBuilder()
                                .setRoutes(
                                        0,
                                        base.getVirtualHosts(0).getRoutes(0).toBuilder()
                                                .setRoute(base.getVirtualHosts(0).getRoutes(0).getRoute().toBuilder()
                                                        .clearRetryPolicy())))
                .build();
        f.ads.routeOverride = routes;
        f.ads.send(XdsResourceType.LDS.getTypeUrl());
        await(() -> routes.equals(endpoint.getSnapshot().getRoutes().get("routes")));
    }

    private static final class PendingConnection implements AutoCloseable {
        final AbstractConnectionClient connection;
        final ServerSocket silent = new ServerSocket();
        final Socket socket;
        final List<Socket> acceptedSockets = new ArrayList<>();

        PendingConnection(Fixture f, XdsEndpoint endpoint) throws Exception {
            try {
                Invoker<?> invoker = f.directory.getAllInvokers().stream()
                        .filter(i -> i.getUrl().getPort() == endpoint.getPort())
                        .findFirst()
                        .get();
                connection =
                        (AbstractConnectionClient) invoker.getUrl().getAttribute(ConnectionStateProvider.ATTRIBUTE);
                // Hold transport negotiation independently from the short RPC deadlines below.
                // A default 3s preface expiry can legitimately produce UNAVAILABLE before a later RPC.
                connection.reset(
                        URL.valueOf("tri://127.0.0.1:" + endpoint.getPort()).addParameter("connect.timeout", 15000));
                RemotingServer server = PortUnificationExchanger.getServers().values().stream()
                        .filter(candidate -> candidate.getUrl().getPort() == endpoint.getPort())
                        .findFirst()
                        .get();
                assertTrue(PortUnificationExchanger.getServers()
                        .remove(server.getUrl().getAddress(), server));
                server.close();
                await(() -> connection.getConnectivityState() == State.TRANSIENT_FAILURE);
                silent.setReuseAddress(true);
                silent.bind(new InetSocketAddress("0.0.0.0", endpoint.getPort()));
                silent.setSoTimeout(6000);
                await(connection::isConnected);
                Channel targetChannel = connection.getChannel(true);
                assertNotNull(targetChannel);
                InetSocketAddress local = (InetSocketAddress) targetChannel.localAddress();
                // Metadata and retired connections can reconnect to the same port. Match the actual
                // business client's local address before controlling its peer socket.
                Socket matched = null;
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(6);
                while (matched == null && System.nanoTime() < deadline) {
                    silent.setSoTimeout((int) Math.max(1, TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime())));
                    Socket candidate;
                    try {
                        candidate = silent.accept();
                    } catch (SocketTimeoutException error) {
                        throw new AssertionError(
                                "Missing business peer: channel=" + targetChannel.id()
                                        + ", local=" + local + ", current=" + connection.getLocalAddress()
                                        + ", state=" + connection.getConnectivityState()
                                        + ", accepted="
                                        + acceptedSockets.stream()
                                                .map(Socket::getRemoteSocketAddress)
                                                .collect(Collectors.toList()),
                                error);
                    }
                    acceptedSockets.add(candidate);
                    if (candidate.getRemoteSocketAddress().equals(local)) matched = candidate;
                }
                assertNotNull(matched, "The controlled socket must belong to the selected business connection");
                socket = matched;
                assertSame(
                        targetChannel,
                        connection.getChannel(true),
                        "Control must use the same active physical channel");
                assertEquals(State.CONNECTING, connection.getConnectivityState());
            } catch (Exception | AssertionError error) {
                try {
                    close();
                } catch (Exception cleanup) {
                    error.addSuppressed(cleanup);
                }
                throw error;
            }
        }

        @Override
        public void close() throws Exception {
            try {
                for (Socket accepted : acceptedSockets) accepted.close();
            } finally {
                silent.close();
            }
        }
    }

    @Test
    void tcpConnectionWithoutHttp2PrefaceIsNotReadyAndNegotiationFailureClosesIt() throws Exception {
        try (Fixture f = new Fixture()) {
            XdsEndpoint endpoint = primary(f);
            Invoker<?> original = f.directory.getAllInvokers().stream()
                    .filter(i -> i.getUrl().getPort() == endpoint.getPort())
                    .findFirst()
                    .get();
            AbstractConnectionClient connection =
                    (AbstractConnectionClient) original.getUrl().getAttribute(ConnectionStateProvider.ATTRIBUTE);
            assertNotNull(connection);
            RemotingServer server = PortUnificationExchanger.getServers().values().stream()
                    .filter(candidate -> candidate.getUrl().getPort() == endpoint.getPort())
                    .findFirst()
                    .get();
            assertTrue(
                    PortUnificationExchanger.getServers().remove(server.getUrl().getAddress(), server));
            server.close();
            await(() -> connection.getConnectivityState() == State.TRANSIENT_FAILURE);
            List<State> transitions = new CopyOnWriteArrayList<>();
            try (AutoCloseable subscription = connection.addConnectivityListener(transitions::add);
                    ServerSocket silent = new ServerSocket()) {
                silent.setReuseAddress(true);
                silent.bind(new InetSocketAddress("0.0.0.0", endpoint.getPort()));
                silent.setSoTimeout(6000);
                try (Socket socket = silent.accept()) {
                    await(connection::isConnected);
                    assertEquals(
                            State.CONNECTING,
                            connection.getConnectivityState(),
                            "TCP alone cannot make a Triple endpoint READY");
                    await(() -> connection.getConnectivityState() == State.TRANSIENT_FAILURE);
                    await(() -> !connection.isConnected());
                    assertTrue(transitions.contains(State.CONNECTING));
                    assertFalse(
                            transitions.contains(State.READY),
                            "An incomplete HTTP/2 handshake must never publish READY");
                }
            }
        }
    }

    @Test
    void healthRemovalEmptyAndZeroLocalitiesFailOverAndRecoveryPromotesPrimary() throws Exception {
        try (Fixture f = new Fixture()) {
            XdsEndpoint primary = primary(f);
            LocalityLbEndpoints first = locality(f, primary.getPort(), "primary", 0, 1, 1, HealthStatus.HEALTHY);
            LocalityLbEndpoints fallback = locality(f, f.replacementPort, "fallback", 1, 1, 1, HealthStatus.UNKNOWN);
            publish(f, first, fallback);
            ports(f, primary.getPort(), f.replacementPort);
            assertEquals("blue:primary", call(f, "primary"));
            publish(
                    f,
                    first.toBuilder()
                            .setLbEndpoints(
                                    0, first.getLbEndpoints(0).toBuilder().setHealthStatus(HealthStatus.UNHEALTHY))
                            .build(),
                    fallback);
            ports(f, f.replacementPort);
            assertEquals("replacement:unhealthy", call(f, "unhealthy"));
            publish(f, first.toBuilder().clearLbEndpoints().build(), fallback);
            ports(f, f.replacementPort);
            assertEquals("replacement:empty", call(f, "empty"));
            publish(
                    f,
                    first.toBuilder().setLoadBalancingWeight(UInt32Value.of(0)).build(),
                    fallback);
            ports(f, f.replacementPort);
            assertEquals("replacement:zero", call(f, "zero"));
            publish(f, first, fallback);
            ports(f, primary.getPort(), f.replacementPort);
            assertEquals("blue:recovered", call(f, "recovered"));
            assertEquals(0, f.green.calls.get(), "Priority failover stays inside the chosen route cluster");
        }
    }

    @Test
    void leastRequestZeroEndpointWeightRemainsPublishedAcrossPolicyUpdates() throws Exception {
        try (Fixture f = new Fixture()) {
            XdsEndpoint primary = primary(f);
            LocalityLbEndpoints first = locality(f, primary.getPort(), "primary", 0, 1, 0, HealthStatus.HEALTHY);
            LocalityLbEndpoints fallback = locality(f, f.replacementPort, "fallback", 1, 1, 1, HealthStatus.HEALTHY);
            publish(f, first, fallback);
            ports(f, f.replacementPort);
            assertEquals("replacement:rr", call(f, "rr"));
            Cluster least = primary.getCluster().toBuilder()
                    .setLbPolicy(Cluster.LbPolicy.LEAST_REQUEST)
                    .build();
            f.ads.clusterOverrides.put(least.getName(), least);
            f.ads.send(XdsResourceType.CDS.getTypeUrl());
            ports(f, primary.getPort(), f.replacementPort);
            assertEquals("blue:least", call(f, "least"));
            f.ads.clusterOverrides.put(primary.getCluster().getName(), primary.getCluster());
            f.ads.send(XdsResourceType.CDS.getTypeUrl());
            ports(f, f.replacementPort);
            assertEquals("replacement:rr-again", call(f, "rr-again"));
        }
    }

    @Test
    void transportFailureAndSameSocketRecoveryDoNotCancelAnActiveFallbackCall() throws Exception {
        try (Fixture f = new Fixture()) {
            XdsEndpoint primary = primary(f);
            publish(
                    f,
                    locality(f, primary.getPort(), "primary", 0, 1, 1, HealthStatus.HEALTHY),
                    locality(f, f.replacementPort, "fallback", 1, 1, 1, HealthStatus.HEALTHY));
            ports(f, primary.getPort(), f.replacementPort);
            Invoker<?> original = f.directory.getAllInvokers().stream()
                    .filter(i -> i.getUrl().getPort() == primary.getPort())
                    .findFirst()
                    .get();
            ConnectionStateProvider connection =
                    (ConnectionStateProvider) original.getUrl().getAttribute(ConnectionStateProvider.ATTRIBUTE);
            assertNotNull(connection, "State must survive the normal Directory/Protocol wrappers");
            assertEquals(State.READY, connection.getConnectivityState());
            assertEquals(Boolean.TRUE, original.getUrl().getAttribute(ConnectionStateProvider.BACKOFF_ATTRIBUTE));
            List<State> transitions = new CopyOnWriteArrayList<>();
            connection.addConnectivityListener(transitions::add);
            RemotingServer server = PortUnificationExchanger.getServers().values().stream()
                    .filter(candidate -> candidate.getUrl().getPort() == primary.getPort())
                    .findFirst()
                    .get();
            assertEquals(primary.getPort(), server.getUrl().getPort());
            assertTrue(
                    PortUnificationExchanger.getServers().remove(server.getUrl().getAddress(), server));
            server.close();
            await(() -> !original.isAvailable());
            assertEquals("replacement:failover", call(f, "failover"));
            f.routes(100, 6000);
            CompletableFuture<String> response = new CompletableFuture<>();
            f.replacement.responses.put("held", response);
            RpcContext.getClientAttachment().setAttachment("timeout", 6000);
            CompletableFuture<String> held = f.remote.call(new Request("held"));
            RpcContext.removeClientAttachment();
            await(() -> f.replacement.values.contains("held"));
            FrameworkModel restarted = new FrameworkModel();
            f.extraFrameworks.add(restarted);
            ApplicationModel application = restarted.newApplication();
            application.getApplicationConfigManager().setApplication(new ApplicationConfig("provider-app"));
            Protocol protocol = application.getExtensionLoader(Protocol.class).getExtension("tri");
            f.extraProtocols.add(protocol);
            ProxyFactory proxy =
                    application.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
            f.exporters.add(XdsRoutingTest.export(
                    application,
                    protocol,
                    proxy,
                    MetadataService.class,
                    f.metadataServices.get(0),
                    URL.valueOf("tri://127.0.0.1:" + primary.getPort() + "/" + MetadataService.class.getName()
                            + "?filter=-default&group=provider-app&version=1.0.0")));
            f.exporters.add(XdsRoutingTest.export(
                    application,
                    protocol,
                    proxy,
                    Business.class,
                    f.blue,
                    URL.valueOf("tri://127.0.0.1:" + primary.getPort() + "/" + Business.class.getName())));
            await(original::isAvailable);
            await(() -> connection.getConnectivityState() == State.READY);
            assertTrue(transitions.contains(State.TRANSIENT_FAILURE));
            assertTrue(transitions.contains(State.CONNECTING));
            assertEquals(State.READY, transitions.get(transitions.size() - 1));
            assertEquals("blue:restored", call(f, "restored"));
            assertFalse(held.isDone(), "Recovery cannot cancel a call already sent to the fallback");
            ExecutorService completion = Executors.newSingleThreadExecutor();
            try {
                completion
                        .submit(() -> {
                            try {
                                response.complete("replacement:held");
                            } finally {
                                RpcContext.removeContext();
                            }
                        })
                        .get(1, TimeUnit.SECONDS);
                assertEquals("replacement:held", held.get(1, TimeUnit.SECONDS));
            } finally {
                completion.shutdownNow();
                assertTrue(completion.awaitTermination(2, TimeUnit.SECONDS));
            }
            assertTrue(f.blue.timeouts.get(f.blue.timeouts.size() - 1) <= 2000);
            assertEquals(0, f.green.calls.get());
        }
    }

    @Test
    void invalidPriorityUpdatesNackAndKeepTheLastUsableEndpoints() throws Exception {
        try (Fixture f = new Fixture()) {
            XdsEndpoint primary = primary(f);
            LocalityLbEndpoints first = locality(f, primary.getPort(), "primary", 0, 1, 1, HealthStatus.HEALTHY);
            LocalityLbEndpoints fallback = locality(f, f.replacementPort, "fallback", 1, 1, 1, HealthStatus.HEALTHY);
            publish(f, first, fallback);
            ports(f, primary.getPort(), f.replacementPort);
            ClusterLoadAssignment valid =
                    f.ads.clusterAssignments.get(primary.getCluster().getName());
            for (LocalityLbEndpoints invalid : Arrays.asList(
                    first,
                    fallback.toBuilder().setPriority(2).build(),
                    fallback.toBuilder().setPriority(-1).build())) {
                f.ads.lastRejection = null;
                publish(f, first, invalid);
                await(() -> f.ads.lastRejection != null);
                assertEquals(
                        valid,
                        primary.getSnapshot()
                                .getEndpoints()
                                .get(primary.getCluster().getName()));
                assertEquals("blue:retained", call(f, "retained"));
            }
        }
    }

    private static String call(Fixture f, String value) throws Exception {
        return f.remote.call(new Request(value)).get(2, TimeUnit.SECONDS);
    }

    private static XdsEndpoint primary(Fixture f) {
        return f.directory.getAllInvokers().stream()
                .map(i -> NativeTlsAdapter.endpoint(i.getUrl()))
                .filter(e -> e.getCluster().getName().equals(f.ads.clusters.get(0)))
                .findFirst()
                .get();
    }

    private static LocalityLbEndpoints locality(
            Fixture f,
            int port,
            String zone,
            int priority,
            int localityWeight,
            int endpointWeight,
            HealthStatus health) {
        LocalityLbEndpoints template = f.ads.endpoints.getEndpoints(0);
        LbEndpoint endpoint = template.getLbEndpoints(0).toBuilder()
                .setHealthStatus(health)
                .setLoadBalancingWeight(UInt32Value.of(endpointWeight))
                .setEndpoint(template.getLbEndpoints(0).getEndpoint().toBuilder()
                        .setAddress(template.getLbEndpoints(0).getEndpoint().getAddress().toBuilder()
                                .setSocketAddress(
                                        template
                                                .getLbEndpoints(0)
                                                .getEndpoint()
                                                .getAddress()
                                                .getSocketAddress()
                                                .toBuilder()
                                                .setPortValue(port))))
                .build();
        return template.toBuilder()
                .clearLbEndpoints()
                .setPriority(priority)
                .setLocality(Locality.newBuilder().setZone(zone))
                .setLoadBalancingWeight(UInt32Value.of(localityWeight))
                .addLbEndpoints(endpoint)
                .build();
    }

    private static void publish(Fixture f, LocalityLbEndpoints... localities) {
        String cluster = f.ads.clusters.get(0);
        f.ads.clusterPorts.remove(cluster);
        f.ads.clusterAssignments.put(
                cluster,
                ClusterLoadAssignment.newBuilder()
                        .setClusterName(cluster)
                        .addAllEndpoints(Arrays.asList(localities))
                        .build());
        f.ads.send(XdsResourceType.EDS.getTypeUrl());
    }

    private static void ports(Fixture f, int... expected) throws Exception {
        Set<Integer> wanted = Arrays.stream(expected).boxed().collect(Collectors.toSet());
        String cluster = f.ads.clusters.get(0);
        await(() -> f.directory.getAllInvokers().stream()
                .filter(i -> NativeTlsAdapter.endpoint(i.getUrl())
                        .getCluster()
                        .getName()
                        .equals(cluster))
                .map(i -> i.getUrl().getPort())
                .collect(Collectors.toSet())
                .equals(wanted));
        await(() -> f.directory.getAllInvokers().stream().allMatch(Invoker::isAvailable));
    }

    private static void await(BooleanSupplier condition) throws Exception {
        long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > until) fail("Priority condition did not become true");
            Thread.sleep(10);
        }
    }
}
