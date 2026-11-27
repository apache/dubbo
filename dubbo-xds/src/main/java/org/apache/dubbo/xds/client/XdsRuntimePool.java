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
import org.apache.dubbo.xds.security.XdsSecurityRuntime;

import java.net.URI;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/** Process-wide ownership prevents ApplicationModels/FrameworkModels from competing for one pilot-agent. */
public final class XdsRuntimePool {
    private static final Map<String, Entry> ENDPOINTS = new LinkedHashMap<>();

    private XdsRuntimePool() {}

    public static synchronized Lease acquire(BootstrapInfo bootstrap) {
        List<String> endpoints = new ArrayList<>();
        Entry existing = null;
        for (ServerInfo server : bootstrap.getServers()) {
            String endpoint = identity(server.getUri());
            endpoints.add(endpoint);
            Entry candidate = ENDPOINTS.get(endpoint);
            if (candidate != null) {
                if (!candidate.bootstrap.getFingerprint().equals(bootstrap.getFingerprint())) {
                    throw new IllegalStateException("Conflicting bootstrap configuration for a shared ADS endpoint");
                }
                if (existing != null && candidate != existing) {
                    throw new IllegalStateException("ADS endpoint ownership conflict");
                }
                existing = candidate;
            }
        }
        if (existing == null) {
            existing = new Entry(bootstrap, endpoints);
            for (String endpoint : endpoints) {
                ENDPOINTS.put(endpoint, existing);
            }
        }
        existing.references++;
        return new Lease(existing);
    }

    public static synchronized int activeRuntimeCount() {
        return new HashSet<>(ENDPOINTS.values()).size();
    }

    private static String identity(String uri) {
        if (uri.startsWith("unix:")) {
            URI address = URI.create(uri);
            if (address.getAuthority() != null
                    || address.getPath() == null
                    || !address.getPath().startsWith("/")) {
                throw new IllegalArgumentException("Invalid ADS Unix socket URI");
            }
            return "unix:" + Paths.get(address.getPath()).normalize();
        }
        return uri;
    }

    private static final class Entry {
        final BootstrapInfo bootstrap;
        final List<String> endpoints;
        final XdsClient client;
        final XdsSecurityRuntime security;
        int references;

        Entry(BootstrapInfo bootstrap, List<String> endpoints) {
            this.bootstrap = bootstrap;
            this.endpoints = endpoints;
            client = new XdsClient(bootstrap);
            security = new XdsSecurityRuntime(bootstrap, client);
        }
    }

    public static final class Lease implements AutoCloseable {
        private final Entry entry;
        private final AtomicBoolean closed = new AtomicBoolean();

        private Lease(Entry entry) {
            this.entry = entry;
        }

        public XdsClient getClient() {
            if (closed.get()) {
                throw new IllegalStateException("xDS runtime lease is closed");
            }
            return entry.client;
        }

        public BootstrapInfo getBootstrap() {
            return entry.bootstrap;
        }

        public XdsSecurityRuntime getSecurity() {
            if (closed.get()) {
                throw new IllegalStateException("xDS runtime lease is closed");
            }
            return entry.security;
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                synchronized (XdsRuntimePool.class) {
                    if (--entry.references == 0) {
                        entry.security.close();
                        entry.client.close();
                        for (String endpoint : entry.endpoints) {
                            ENDPOINTS.remove(endpoint, entry);
                        }
                    }
                }
            }
        }
    }
}
