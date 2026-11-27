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
package org.apache.dubbo.xds.security;

import org.apache.dubbo.xds.bootstrap.Bootstrapper;
import org.apache.dubbo.xds.bootstrap.Bootstrapper.BootstrapInfo;
import org.apache.dubbo.xds.bootstrap.Bootstrapper.CertificateProviderInfo;
import org.apache.dubbo.xds.client.XdsClient;
import org.apache.dubbo.xds.client.XdsResourceType;
import org.apache.dubbo.xds.traffic.XdsFaultFilters;
import org.apache.dubbo.xds.traffic.XdsLoadStats;
import org.apache.dubbo.xds.traffic.XdsRouteTable;
import org.apache.dubbo.xds.traffic.XdsTimeouts;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import io.grpc.xds.shaded.io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager;

/** Agent-owned certificate providers and policy validators are shared with the ADS runtime. */
public final class XdsSecurityRuntime implements AutoCloseable {
    private final XdsLoadStats loadStats;
    private final BootstrapInfo bootstrap;
    private final Map<String, CertificateSlot> providers = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<Runnable> listeners = new CopyOnWriteArrayList<>();
    private final AtomicLong epoch = new AtomicLong();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final ScheduledExecutorService monitor = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "dubbo-xds-certificates");
        thread.setDaemon(true);
        return thread;
    });

    public XdsSecurityRuntime(BootstrapInfo bootstrap, XdsClient client) {
        this.bootstrap = bootstrap;
        loadStats = client.getLoadStats();
        client.addValidator(XdsResourceType.CDS, cluster -> {
            TlsPolicy.client(cluster, bootstrap);
        });
        client.addValidator(XdsResourceType.RDS, XdsRouteTable::new);
        client.addValidator(XdsResourceType.LDS, listener -> {
            if (!listener.hasApiListener()) {
                InboundPolicy.parse(listener, bootstrap);
            } else {
                try {
                    HttpConnectionManager hcm =
                            listener.getApiListener().getApiListener().unpack(HttpConnectionManager.class);
                    XdsTimeouts.listener(hcm);
                    new XdsFaultFilters(hcm);
                    if (hcm.hasRouteConfig()) {
                        new XdsRouteTable(hcm.getRouteConfig());
                    }
                } catch (IOException error) {
                    throw new IllegalArgumentException("Invalid outbound route listener", error);
                }
            }
        });
        // File access is lazy: subscribing to xDS alone does not read private keys.
        monitor.scheduleWithFixedDelay(this::poll, 100, 100, TimeUnit.MILLISECONDS);
    }

    public XdsLoadStats getLoadStats() {
        return loadStats;
    }

    public BootstrapInfo getBootstrap() {
        return bootstrap;
    }

    public long getEpoch() {
        return epoch.get();
    }

    /** Configured provider states without initializing providers or reading agent-owned files. */
    public Map<String, CertificateFiles.Status> inspectCertificateProviders() {
        if (closed.get()) return Collections.emptyMap();
        Map<String, CertificateFiles.Status> statuses = new LinkedHashMap<>();
        bootstrap.getCertificateProviders().forEach((name, ignored) -> {
            CertificateSlot slot = providers.get(name);
            statuses.put(name, slot == null ? CertificateFiles.Status.notLoaded() : slot.files.inspect());
        });
        return Collections.unmodifiableMap(statuses);
    }

    public AutoCloseable watchCertificates(Runnable listener) {
        if (closed.get()) {
            throw new IllegalStateException("Security runtime is closed");
        }
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    public NativeCertificate certificate(TlsPolicy policy) {
        if (closed.get()) {
            throw new IllegalStateException("Security runtime is closed");
        }
        if (!policy.isTls()) {
            return NativeCertificate.plaintext();
        }
        CertificateSlot identity = provider(policy.getIdentityProvider());
        CertificateSlot roots = provider(policy.getRootProvider());
        CertificateFiles.Material identityMaterial = identity.files.current();
        return new NativeCertificate(
                policy, identityMaterial, roots == identity ? identityMaterial : roots.files.current());
    }

    private CertificateSlot provider(String name) {
        return providers.computeIfAbsent(name, key -> {
            CertificateProviderInfo info = bootstrap.getCertificateProviders().get(key);
            if (info == null) {
                throw new IllegalArgumentException("Unknown certificate provider");
            }
            CertificateFiles files = new CertificateFiles(info);
            files.refresh();
            long interval = info.getConfig().containsFields("refresh_interval")
                    ? Bootstrapper.parseDuration(info.getConfig()
                                    .getFieldsOrThrow("refresh_interval")
                                    .getStringValue())
                            .toMillis()
                    : 600_000L;
            return new CertificateSlot(files, Math.max(100L, interval));
        });
    }

    private void poll() {
        if (closed.get()) {
            return;
        }
        long now = System.nanoTime();
        boolean changed = false;
        for (CertificateSlot slot : providers.values()) {
            if (now >= slot.nextPoll) {
                changed |= slot.files.refresh();
                slot.nextPoll = now + TimeUnit.MILLISECONDS.toNanos(slot.nextInterval());
            }
        }
        if (changed) {
            epoch.incrementAndGet();
            for (Runnable listener : listeners) {
                try {
                    listener.run();
                } catch (RuntimeException ignored) {
                    /* One owner cannot stop rotation for other owners. */
                }
            }
        }
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            monitor.shutdownNow();
            listeners.clear();
            providers.clear();
        }
    }

    private static final class CertificateSlot {
        final CertificateFiles files;
        final long intervalMillis;
        volatile long nextPoll;

        CertificateSlot(CertificateFiles files, long intervalMillis) {
            this.files = files;
            this.intervalMillis = intervalMillis;
            nextPoll = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(nextInterval());
        }

        long nextInterval() {
            // An agent may not have written its first files yet, or may be between two file updates.
            return files.getLastError() == null ? intervalMillis : Math.min(intervalMillis, 1000L);
        }
    }
}
