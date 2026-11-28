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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.remoting.api.connection.ConnectionManager;
import org.apache.dubbo.remoting.api.connection.ConnectionStateProvider;
import org.apache.dubbo.remoting.api.connection.MultiplexProtocolConnectionManager;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ConsumerModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.xds.registry.XdsEndpoint;
import org.apache.dubbo.xds.security.NativeTlsAdapter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import io.netty.channel.Channel;

/** Test-only direct peer retaining a coherent pre-rotation endpoint and its actual TLS connections. */
final class NativeRootPeerProbe {
    private static Peer peer;

    private NativeRootPeerProbe() {}

    static synchronized Map<String, Object> execute(Properties command, ApplicationModel original) throws Exception {
        String method = command.getProperty("method");
        if ("root-peer-close".equals(method)) return close();
        if ("root-peer-pin".equals(method)) {
            if (peer != null) throw new IllegalStateException("Root peer already pinned");
            String service = command.getProperty("service", "");
            List<XdsEndpoint> candidates = new ArrayList<>();
            manager(original).forEachConnection(client -> {
                XdsEndpoint endpoint = NativeTlsAdapter.endpoint(client.getUrl());
                if (endpoint != null
                        && endpoint.getTarget().getHostname().equals(service)
                        && endpoint.getTarget().getServicePort() == 7070
                        && "tri".equals(endpoint.getTarget().getProtocol())
                        && endpoint.getCluster()
                                .getName()
                                .equals(endpoint.getTarget().defaultCluster())
                        && client.getConnectivityState() == ConnectionStateProvider.State.READY)
                    candidates.add(endpoint);
            });
            if (candidates.isEmpty()) throw new IllegalStateException("No ready native Triple endpoint to pin");
            XdsEndpoint endpoint = candidates.get(0);
            endpoint.getCertificate(); // Freeze the endpoint's immutable certificate before any root update.
            Peer next = new Peer(endpoint);
            try {
                next.business = next.reference(NativeEcho.class, "native-xds", "1.0.0");
                next.metadata = next.reference(MetadataService.class, "native-xds-provider", MetadataService.VERSION);
                next.awaitTlsReady();
                peer = next;
            } catch (Exception error) {
                next.destroy();
                throw error;
            }
        } else if (!"root-peer-check".equals(method)) throw new IllegalArgumentException("Unknown root peer command");
        if (peer == null) throw new IllegalStateException("Root peer is not pinned");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "OBSERVED");
        result.put("connections_before", peer.connections());
        boolean retired = "retired".equals(command.getProperty("key"));
        String token = command.getProperty("message", "");
        if (retired && !token.matches("[A-Za-z0-9_-]{1,64}"))
            throw new IllegalArgumentException("Invalid root probe token");
        result.put(
                "business",
                call(() -> retired
                        ? peer.business.asyncEcho("hold:" + token).get(5, TimeUnit.SECONDS)
                        : peer.business.echo("native-root-peer")));
        result.put("metadata", call(() -> peer.metadata.getMetadataInfos().size()));
        result.put("connections_after", peer.connections());
        return result;
    }

    private static Map<String, Object> call(Callable<Object> action) {
        Map<String, Object> result = new LinkedHashMap<>();
        RpcContext.getClientAttachment().setAttachment("timeout", 3000);
        try {
            result.put("value", action.call());
            result.put("status", "OK");
        } catch (Exception error) {
            Throwable cause = error;
            while ((cause instanceof ExecutionException || cause instanceof CompletionException)
                    && cause.getCause() != null) cause = cause.getCause();
            result.put("status", TriRpcStatus.getStatus(cause).code.name());
            result.put("error_type", cause.getClass().getSimpleName());
        } finally {
            RpcContext.removeContext();
        }
        return result;
    }

    static synchronized Map<String, Object> close() throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();
        if (peer == null) {
            result.put("status", "CLOSED");
            result.put("remaining_open_channels", 0);
            return result;
        }
        List<Channel> channels = peer.channels();
        Peer closing = peer;
        peer = null;
        closing.destroy();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (channels.stream().anyMatch(Channel::isOpen) && System.nanoTime() < deadline) Thread.sleep(10);
        result.put("status", "CLOSED");
        result.put("retired_channel_count", channels.size());
        result.put(
                "remaining_open_channels",
                channels.stream().filter(Channel::isOpen).count());
        return result;
    }

    private static ConnectionManager manager(ApplicationModel application) {
        return application
                .getFrameworkModel()
                .getExtensionLoader(ConnectionManager.class)
                .getExtension(MultiplexProtocolConnectionManager.NAME);
    }

    private static final class Peer {
        final FrameworkModel framework = new FrameworkModel();
        final ApplicationModel application = framework.newApplication();
        final XdsEndpoint endpoint;
        final List<Invoker<?>> invokers = new ArrayList<>();
        NativeEcho business;
        MetadataService metadata;

        Peer(XdsEndpoint endpoint) {
            this.endpoint = endpoint;
            application.getApplicationConfigManager().setApplication(new ApplicationConfig("native-root-peer"));
        }

        <T> T reference(Class<T> type, String group, String version) throws Exception {
            Map<String, String> parameters = new LinkedHashMap<>();
            parameters.put("interface", type.getName());
            parameters.put("group", group);
            parameters.put("version", version);
            parameters.put("timeout", "3000");
            parameters.put("retries", "0");
            parameters.put("side", "consumer");
            parameters.put("reference.filter", "-default");
            URL url = NativeTlsAdapter.decorate(
                    new URL("tri", endpoint.getHost(), endpoint.getPort(), type.getName(), parameters)
                            .setScopeModel(application.getInternalModule())
                            .putAttribute(XdsEndpoint.ATTRIBUTE, endpoint));
            ConsumerModel model = application
                    .getInternalModule()
                    .registerInternalConsumer(
                            type,
                            url,
                            application
                                    .getInternalModule()
                                    .getServiceRepository()
                                    .registerService(type));
            Invoker<T> invoker = application
                    .getExtensionLoader(Protocol.class)
                    .getExtension("tri")
                    .refer(type, url.setServiceModel(model));
            invokers.add(invoker);
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
            while (!invoker.isAvailable() && System.nanoTime() < deadline) Thread.sleep(20);
            if (!invoker.isAvailable()) throw new IllegalStateException("Pinned peer TLS connection is not ready");
            return application
                    .getExtensionLoader(ProxyFactory.class)
                    .getAdaptiveExtension()
                    .getProxy(invoker);
        }

        List<Channel> channels() {
            List<Channel> channels = new ArrayList<>();
            manager(application).forEachConnection(client -> {
                if (NativeTlsAdapter.endpoint(client.getUrl()) == endpoint) channels.add(client.getChannel(true));
            });
            if (channels.isEmpty()) throw new IllegalStateException("Pinned peer connection was lost");
            return channels;
        }

        List<Map<String, Object>> connections() throws Exception {
            List<Map<String, Object>> values = new ArrayList<>();
            for (Channel channel : channels()) values.add(NativeLifecycleProbe.tls(channel));
            return values;
        }

        void awaitTlsReady() throws Exception {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
            while (System.nanoTime() < deadline) {
                List<Map<String, Object>> observed = connections();
                if (!observed.isEmpty()
                        && observed.stream()
                                .allMatch(value ->
                                        Boolean.TRUE.equals(value.get("ready")) && "h2".equals(value.get("alpn"))))
                    return;
                Thread.sleep(10);
            }
            throw new IllegalStateException("Pinned peer TLS/h2 handshake is not ready");
        }

        void destroy() {
            try {
                for (Invoker<?> invoker : invokers) invoker.destroy();
            } finally {
                framework.destroy();
            }
        }
    }
}
