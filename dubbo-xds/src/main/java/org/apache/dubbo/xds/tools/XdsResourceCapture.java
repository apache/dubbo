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
package org.apache.dubbo.xds.tools;

import org.apache.dubbo.xds.bootstrap.Bootstrapper;
import org.apache.dubbo.xds.bootstrap.Bootstrapper.BootstrapInfo;
import org.apache.dubbo.xds.client.XdsClient;
import org.apache.dubbo.xds.client.XdsClient.ResourceWatcher;
import org.apache.dubbo.xds.client.XdsResourceType;
import org.apache.dubbo.xds.registry.DiscoveryTarget;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.TypeRegistry;
import com.google.protobuf.util.JsonFormat;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.filters.http.fault.v3.HTTPFault;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.filters.http.router.v3.Router;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager;
import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.transport_sockets.tls.v3.UpstreamTlsContext;

/** Captures test traffic resources through the native ADS transport without asserting policy execution. */
public final class XdsResourceCapture {
    private final XdsClient client;
    private final Path output;
    private final List<AutoCloseable> watches = new ArrayList<>();
    private boolean closed;
    private final CompletableFuture<Void> lds = new CompletableFuture<>();
    private final CompletableFuture<Void> rds = new CompletableFuture<>();
    private final CompletableFuture<Void> cds = new CompletableFuture<>();
    private final CompletableFuture<Void> eds = new CompletableFuture<>();
    private final JsonFormat.Printer printer = JsonFormat.printer()
            .usingTypeRegistry(TypeRegistry.newBuilder()
                    .add(HttpConnectionManager.getDescriptor())
                    .add(HTTPFault.getDescriptor())
                    .add(Router.getDescriptor())
                    .add(UpstreamTlsContext.getDescriptor())
                    .build())
            .preservingProtoFieldNames();

    private XdsResourceCapture(XdsClient client, Path output) {
        this.client = client;
        this.output = output;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3 && args.length != 4)
            throw new IllegalArgumentException(
                    "Usage: XdsResourceCapture <service-fqdn> <service-port> <output-directory> [tri|dubbo]");
        BootstrapInfo bootstrap = Bootstrapper.load();
        DiscoveryTarget target =
                new DiscoveryTarget(args[0], Integer.parseInt(args[1]), args.length == 4 ? args[3] : "tri");
        Path output = Paths.get(args[2]);
        Files.createDirectories(output);
        try (XdsClient client = new XdsClient(bootstrap)) {
            XdsResourceCapture capture = new XdsResourceCapture(client, output);
            try {
                if ("tri".equals(target.getProtocol())) {
                    capture.watch(
                            XdsResourceType.LDS,
                            bootstrap.clientListenerName(target.authority()),
                            "lds",
                            capture.lds,
                            listener -> {
                                try {
                                    HttpConnectionManager hcm = listener.getApiListener()
                                            .getApiListener()
                                            .unpack(HttpConnectionManager.class);
                                    if (hcm.hasRouteConfig()) capture.save("rds", hcm.getRouteConfig(), capture.rds);
                                    else
                                        capture.watch(
                                                XdsResourceType.RDS,
                                                hcm.getRds().getRouteConfigName(),
                                                "rds",
                                                capture.rds,
                                                ignored -> {});
                                } catch (Exception error) {
                                    capture.rds.completeExceptionally(error);
                                }
                            });
                }
                capture.watch(
                        XdsResourceType.CDS,
                        target.defaultCluster(),
                        "cds",
                        capture.cds,
                        cluster -> capture.watch(
                                XdsResourceType.EDS,
                                cluster.getEdsClusterConfig().getServiceName().isEmpty()
                                        ? cluster.getName()
                                        : cluster.getEdsClusterConfig().getServiceName(),
                                "eds",
                                capture.eds,
                                ignored -> {}));
                boolean triple = "tri".equals(target.getProtocol());
                CompletableFuture<?>[] resources = triple
                        ? new CompletableFuture<?>[] {capture.lds, capture.rds, capture.cds, capture.eds}
                        : new CompletableFuture<?>[] {capture.cds, capture.eds};
                CompletableFuture.allOf(resources).get(45, TimeUnit.SECONDS);
                System.out.println("XDS_CAPTURE_READY target=" + target + " resources=" + resources.length);
            } finally {
                capture.closeWatches();
            }
        }
    }

    private synchronized <T extends Message> void watch(
            XdsResourceType<T> type, String name, String file, CompletableFuture<Void> ready, Consumer<T> received) {
        if (closed || ready.isDone()) return;
        watches.add(client.watch(type, name, new ResourceWatcher<T>() {
            public void onChanged(T resource) {
                received.accept(resource);
                save(file, resource, ready);
            }

            public void onAbsent() {
                ready.completeExceptionally(new IllegalStateException(file + " resource is absent"));
            }

            public void onError(Throwable error) {
                ready.completeExceptionally(error);
            }
        }));
    }

    private synchronized void save(String file, Message resource, CompletableFuture<Void> ready) {
        if (closed || ready.isDone()) return;
        try {
            Files.write(output.resolve(file + ".pb"), resource.toByteArray());
            String json;
            try {
                json = printer.print(resource);
            } catch (InvalidProtocolBufferException unknownExtension) {
                json = "{\"encoding\":\"protobuf-base64\",\"message_type\":\""
                        + resource.getDescriptorForType().getFullName()
                        + "\",\"value\":\"" + Base64.getEncoder().encodeToString(resource.toByteArray()) + "\"}";
            }
            Files.write(output.resolve(file + ".json"), json.getBytes(StandardCharsets.UTF_8));
            ready.complete(null);
        } catch (Exception error) {
            ready.completeExceptionally(error);
        }
    }

    private void closeWatches() throws Exception {
        List<AutoCloseable> copy;
        synchronized (this) {
            closed = true;
            copy = new ArrayList<>(watches);
            watches.clear();
        }
        for (AutoCloseable watch : copy) watch.close();
    }
}
