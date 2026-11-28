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
import org.apache.dubbo.xds.client.XdsClient.ResourceWatcher;
import org.apache.dubbo.xds.client.XdsResourceGraph;
import org.apache.dubbo.xds.client.XdsResourceGraph.Snapshot;
import org.apache.dubbo.xds.client.XdsResourceType;
import org.apache.dubbo.xds.client.XdsRuntimePool;
import org.apache.dubbo.xds.registry.DiscoveryTarget;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import io.grpc.xds.shaded.io.envoyproxy.envoy.config.listener.v3.Listener;

/** Minimal control-plane diagnostic; does not export a business server or read private key contents. */
public final class XdsProbe {
    private XdsProbe() {}

    public static void main(String[] args) throws Exception {
        int inputs = args.length;
        long holdSeconds = 0;
        if (inputs > 0 && args[inputs - 1].startsWith("--hold-seconds=")) {
            holdSeconds = Long.parseLong(args[--inputs].substring("--hold-seconds=".length()));
            if (holdSeconds < 1 || holdSeconds > 600) {
                throw new IllegalArgumentException("Probe hold must be between 1 and 600 seconds");
            }
        }
        if (inputs < 2 || inputs > 3) {
            throw new IllegalArgumentException(
                    "Usage: XdsProbe <service-fqdn> <service-port> [inbound-bind-address:port] [--hold-seconds=1..600]");
        }
        BootstrapInfo bootstrap = Bootstrapper.load();
        DiscoveryTarget target = new DiscoveryTarget(args[0], Integer.parseInt(args[1]), "tri");
        CompletableFuture<Snapshot> resolved = new CompletableFuture<>();
        CompletableFuture<Listener> inbound = new CompletableFuture<>();
        try (XdsRuntimePool.Lease lease = XdsRuntimePool.acquire(bootstrap);
                XdsResourceGraph graph = new XdsResourceGraph(
                        lease.getClient(),
                        bootstrap,
                        target,
                        snapshot -> {
                            if (snapshot.isReady()) {
                                resolved.complete(snapshot);
                            }
                        },
                        error -> System.err.println(
                                "xDS update unavailable: " + error.getClass().getSimpleName()))) {
            AutoCloseable serverWatch = null;
            try {
                if (inputs == 3) {
                    serverWatch = lease.getClient()
                            .watch(
                                    XdsResourceType.LDS,
                                    bootstrap.serverListenerName(args[2]),
                                    new ResourceWatcher<Listener>() {
                                        @Override
                                        public void onChanged(Listener value) {
                                            inbound.complete(value);
                                        }

                                        @Override
                                        public void onAbsent() {
                                            inbound.completeExceptionally(
                                                    new IllegalStateException("Inbound listener not found"));
                                        }

                                        @Override
                                        public void onError(Throwable error) {
                                            System.err.println("Inbound update unavailable: "
                                                    + error.getClass().getSimpleName());
                                        }
                                    });
                }
                Snapshot snapshot = resolved.get(45, TimeUnit.SECONDS);
                if (serverWatch != null) {
                    inbound.get(45, TimeUnit.SECONDS);
                }
                System.out.println("XDS_PROBE_READY target=" + target + " clusters="
                        + snapshot.getClusters().size()
                        + " endpoint_resources=" + snapshot.getEndpoints().size() + " inbound=" + (serverWatch != null)
                        + " runtimes=" + XdsRuntimePool.activeRuntimeCount());
                if (holdSeconds > 0) {
                    TimeUnit.SECONDS.sleep(holdSeconds);
                }
            } finally {
                if (serverWatch != null) {
                    serverWatch.close();
                }
            }
        }
    }
}
