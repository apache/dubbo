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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Wrapper;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProtocolServer;
import org.apache.dubbo.xds.bootstrap.Bootstrapper;
import org.apache.dubbo.xds.bootstrap.Bootstrapper.BootstrapInfo;
import org.apache.dubbo.xds.registry.XdsEndpoint;
import org.apache.dubbo.xds.security.XdsServerBindings.ServerLease;
import org.apache.dubbo.xds.traffic.XdsAdmissionInvoker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.dubbo.common.constants.CommonConstants.ANYHOST_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.ANYHOST_VALUE;
import static org.apache.dubbo.rpc.cluster.Constants.EXPORT_KEY;

/** Initializes native inbound policy before any metadata/business socket can be exported. */
@Wrapper(
        matches = {"tri", "dubbo", "registry", "service-discovery-registry"},
        order = 1000)
public final class NativeSecurityProtocol implements Protocol {
    private final Protocol delegate;
    private final Set<OwnedExporter<?>> exporters = ConcurrentHashMap.newKeySet();
    private final Set<Integer> ports = ConcurrentHashMap.newKeySet();

    public NativeSecurityProtocol(Protocol delegate) {
        this.delegate = delegate;
    }

    @Override
    public int getDefaultPort() {
        return delegate.getDefaultPort();
    }

    @Override
    public List<ProtocolServer> getServers() {
        return delegate.getServers();
    }

    @Override
    public <T> Exporter<T> export(Invoker<T> invoker) {
        URL origin = invoker.getUrl();
        URL provider = origin;
        List<URL> sources = new ArrayList<>();
        if ("registry".equals(origin.getProtocol()) || "service-discovery-registry".equals(origin.getProtocol())) {
            if (!"xds".equals(origin.getParameter("registry"))) {
                return delegate.export(invoker);
            }
            Object value = origin.getAttribute(EXPORT_KEY);
            if (!(value instanceof URL)) {
                throw new IllegalArgumentException("Missing native registry provider URL");
            }
            provider = (URL) value;
            sources.add(origin);
        } else if ("tri".equals(provider.getProtocol())) {
            if (provider.getParameter("xds.enabled", false)) {
                sources.add(provider);
            }
            for (RegistryConfig registry : provider.getOrDefaultApplicationModel()
                    .getApplicationConfigManager()
                    .getRegistries()) {
                if ("xds".equals(registry.getProtocol())
                        || (registry.getAddress() != null
                                && registry.getAddress().startsWith("xds://"))) {
                    URL registryUrl = registry.getAddress() == null
                            ? URL.valueOf("xds://localhost")
                            : URL.valueOf(registry.getAddress());
                    if (registry.getParameters() != null) {
                        registryUrl = registryUrl.addParameters(registry.getParameters());
                    }
                    sources.add(registryUrl);
                }
            }
        }
        if (!"tri".equals(provider.getProtocol()) || sources.isEmpty()) {
            return delegate.export(invoker);
        }
        BootstrapInfo bootstrap = null;
        long wait = 30000;
        try {
            for (URL source : sources) {
                long sourceWait = source.getParameter("xds.bootstrap.wait", 30000L);
                BootstrapInfo candidate = Bootstrapper.load(
                        System.getenv(),
                        source.getParameter("xds.bootstrap.file"),
                        source.getParameter("xds.bootstrap.config"),
                        sourceWait);
                if (bootstrap != null && !bootstrap.getFingerprint().equals(candidate.getFingerprint())) {
                    throw new IllegalStateException("Conflicting native bootstrap sources for one provider");
                }
                bootstrap = candidate;
                wait = Math.min(wait, sourceWait);
            }
        } catch (IOException error) {
            throw new IllegalStateException("Unable to initialize provider xDS bootstrap", error);
        }
        int port = provider.getParameter("bind.port", provider.getPort());
        String host = provider.getParameter("bind.ip", "0.0.0.0");
        // Normal ServiceConfig publishes a concrete bind.ip while ANYHOST makes transport listen on wildcard.
        if (provider.getParameter(ANYHOST_KEY, false) || NetUtils.isInvalidLocalHost(host)) {
            host = ANYHOST_VALUE;
        }
        String address = (host.contains(":") ? "[" + host + "]" : host) + ":" + port;
        ServerLease lease = XdsServerBindings.acquire(bootstrap, address, port);
        ports.add(port);
        try {
            lease.awaitInitial(wait);
            OwnedExporter<T> exported = new OwnedExporter<>(delegate.export(invoker), lease);
            exporters.add(exported);
            return exported;
        } catch (RuntimeException | Error error) {
            lease.close();
            throw error;
        }
    }

    @Override
    public <T> Invoker<T> refer(Class<T> type, URL url) {
        Invoker<T> invoker = delegate.refer(type, NativeTlsAdapter.decorate(url));
        XdsEndpoint endpoint = NativeTlsAdapter.endpoint(url);
        return endpoint != null && "tri".equals(url.getProtocol())
                ? new XdsAdmissionInvoker<>(invoker, endpoint)
                : invoker;
    }

    @Override
    public void destroy() {
        try {
            delegate.destroy();
        } finally {
            exporters.forEach(OwnedExporter::release);
            exporters.clear();
            ports.forEach(XdsServerBindings::discardRetired);
            ports.clear();
        }
    }

    private final class OwnedExporter<T> implements Exporter<T> {
        final Exporter<T> delegate;
        final ServerLease lease;
        final AtomicBoolean closed = new AtomicBoolean();

        OwnedExporter(Exporter<T> delegate, ServerLease lease) {
            this.delegate = delegate;
            this.lease = lease;
        }

        @Override
        public Invoker<T> getInvoker() {
            return delegate.getInvoker();
        }

        @Override
        public void register() {
            delegate.register();
        }

        @Override
        public void unregister() {
            delegate.unregister();
        }

        @Override
        public void unexport() {
            if (closed.compareAndSet(false, true)) {
                try {
                    delegate.unexport();
                } finally {
                    lease.close();
                    exporters.remove(this);
                }
            }
        }

        void release() {
            if (closed.compareAndSet(false, true)) {
                lease.close();
            }
        }
    }
}
