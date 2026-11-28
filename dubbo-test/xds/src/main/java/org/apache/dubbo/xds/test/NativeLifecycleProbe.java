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

import org.apache.dubbo.remoting.api.connection.AbstractConnectionClient;
import org.apache.dubbo.remoting.api.connection.ConnectionManager;
import org.apache.dubbo.remoting.api.connection.ConnectionStateProvider;
import org.apache.dubbo.remoting.api.connection.MultiplexProtocolConnectionManager;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.xds.client.XdsClient;
import org.apache.dubbo.xds.client.XdsResourceType;
import org.apache.dubbo.xds.client.XdsRuntimePool;
import org.apache.dubbo.xds.registry.XdsEndpoint;
import org.apache.dubbo.xds.transport.XdsTransport;

import javax.net.ssl.SSLSession;

import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import io.netty.channel.Channel;
import io.netty.handler.ssl.SslHandler;

/** Test-only physical connection loss; public TLS fingerprints never include private-key material. */
final class NativeLifecycleProbe {
    private NativeLifecycleProbe() {}

    static Map<String, Object> reconnect(ApplicationModel application, String kind, String cluster) throws Exception {
        if ("ads".equals(kind)) return ads();
        if ("tri".equals(kind)) return business(application, cluster);
        throw new IllegalArgumentException("Reconnect probe requires ads or tri");
    }

    private static Map<String, Object> ads() throws Exception {
        try (XdsRuntimePool.Lease lease = XdsRuntimePool.acquire(NativeXdsApplication.diagnosticBootstrap())) {
            XdsClient client = lease.getClient();
            if (!"READY".equals(client.getConnectionState())) throw new IllegalStateException("ADS is not ready");
            Map<String, XdsClient.ResourceMetadata> before = client.getResourceMetadata();
            if (before.isEmpty()) throw new IllegalStateException("No active ADS resources");
            // Only the optional test application accesses the private transport to inject real UDS channel loss.
            // Production XdsClient has no externally callable disconnect/reset hook.
            Field field = XdsClient.class.getDeclaredField("transport");
            field.setAccessible(true);
            client.snapshot(XdsResourceType.CDS).get(2, TimeUnit.SECONDS);
            XdsTransport previous = (XdsTransport) field.get(client);
            previous.close();
            long start = System.nanoTime();
            long deadline = start + TimeUnit.SECONDS.toNanos(30);
            List<String> states = new ArrayList<>();
            boolean replaced = false;
            while (System.nanoTime() < deadline) {
                client.snapshot(XdsResourceType.CDS).get(2, TimeUnit.SECONDS);
                String state = client.getConnectionState();
                if (states.isEmpty() || !states.get(states.size() - 1).equals(state)) states.add(state);
                replaced = field.get(client) != null && field.get(client) != previous;
                if (replaced && "READY".equals(state)) break;
                Thread.sleep(20);
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "OBSERVED");
            result.put("kind", "ads");
            result.put("injection", "close SDK ADS transport; agent and Istiod stay running");
            result.put("previous_channel_shutdown", previous.getChannel().isShutdown());
            result.put("transport_replaced", replaced);
            result.put("connection", client.getConnectionState());
            result.put("states", states);
            result.put("runtime_count", XdsRuntimePool.activeRuntimeCount());
            result.put(
                    "subscription_keys_retained",
                    client.getResourceMetadata().keySet().containsAll(before.keySet()));
            result.put("elapsed_ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
            return result;
        }
    }

    private static Map<String, Object> business(ApplicationModel application, String cluster) throws Exception {
        ConnectionManager manager = application
                .getFrameworkModel()
                .getExtensionLoader(ConnectionManager.class)
                .getExtension(MultiplexProtocolConnectionManager.NAME);
        List<AbstractConnectionClient> clients = new ArrayList<>();
        manager.forEachConnection(client -> {
            XdsEndpoint endpoint = (XdsEndpoint) client.getUrl().getAttribute(XdsEndpoint.ATTRIBUTE);
            if (endpoint != null
                    && endpoint.getCluster().getName().equals(cluster)
                    && "tri".equals(endpoint.getTarget().getProtocol())
                    && client.getConnectivityState() == ConnectionStateProvider.State.READY) clients.add(client);
        });
        if (clients.isEmpty()) throw new IllegalStateException("No ready Triple connections in the requested cluster");
        List<Map<String, Object>> connections = new ArrayList<>();
        List<AutoCloseable> watches = new ArrayList<>();
        long start = System.nanoTime();
        try {
            for (AbstractConnectionClient client : clients) {
                Channel previous = client.getChannel(true);
                Map<String, Object> connection = new LinkedHashMap<>();
                connections.add(connection);
                connection.put("before", tls(previous));
                List<String> states = new CopyOnWriteArrayList<>();
                watches.add(client.addConnectivityListener(state -> states.add(state.name())));
                previous.close().await(3, TimeUnit.SECONDS);
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
                Channel current;
                while (System.nanoTime() < deadline) {
                    current = client.getChannel(true);
                    if (current != null
                            && current != previous
                            && client.getConnectivityState() == ConnectionStateProvider.State.READY) break;
                    Thread.sleep(20);
                }
                current = client.getChannel(true);
                connection.put("previous_channel_closed", !previous.isOpen());
                connection.put("channel_replaced", current != null && current != previous);
                connection.put("connectivity", client.getConnectivityState());
                connection.put("client_closed", client.isClosed());
                connection.put("states", states);
                connection.put("after", tls(current));
            }
        } finally {
            for (AutoCloseable watch : watches) watch.close();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "OBSERVED");
        result.put("kind", "tri");
        result.put("cluster", cluster);
        result.put("injection", "close actual native HTTP/2 channels; clients and references stay alive");
        result.put("connections", connections);
        result.put("elapsed_ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
        return result;
    }

    static Map<String, Object> tls(Channel channel) throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();
        if (channel == null) {
            result.put("ready", false);
            return result;
        }
        SslHandler handler = channel.pipeline().get(SslHandler.class);
        if (handler == null || !handler.handshakeFuture().isSuccess()) {
            result.put("ready", false);
            return result;
        }
        SSLSession session = handler.engine().getSession();
        Certificate[] local = session.getLocalCertificates();
        Certificate[] peer = session.getPeerCertificates();
        result.put("ready", channel.isActive());
        result.put("channel_id", channel.id().asLongText());
        result.put("protocol", session.getProtocol());
        result.put("alpn", handler.applicationProtocol());
        result.put("local_leaf_sha256", local == null || local.length == 0 ? null : fingerprint(local[0]));
        result.put("peer_leaf_sha256", peer.length == 0 ? null : fingerprint(peer[0]));
        return result;
    }

    static String fingerprint(Certificate certificate) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded());
        StringBuilder result = new StringBuilder();
        for (byte value : digest) result.append(String.format("%02x", value & 0xff));
        return result.toString();
    }
}
