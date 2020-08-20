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
package org.apache.dubbo.registry.integration;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.URLBuilder;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.client.RegistryProtocol;
import org.apache.dubbo.registry.client.ServiceDiscoveryRegistryDirectory;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.Cluster;
import org.apache.dubbo.rpc.cluster.ClusterInvoker;
import org.apache.dubbo.rpc.cluster.Directory;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_PROTOCOL;
import static org.apache.dubbo.registry.Constants.DEFAULT_REGISTRY;

/**
 * RegistryProtocol
 */
public class InterfaceCompatibleRegistryProtocol extends RegistryProtocol {

    @Override
    protected URL getRegistryUrl(Invoker<?> originInvoker) {
        URL registryUrl = originInvoker.getUrl();
        if (REGISTRY_PROTOCOL.equals(registryUrl.getProtocol())) {
            String protocol = registryUrl.getParameter(REGISTRY_KEY, DEFAULT_REGISTRY);
            registryUrl = registryUrl.setProtocol(protocol).removeParameter(REGISTRY_KEY);
        }
        return registryUrl;
    }

    @Override
    protected URL getRegistryUrl(URL url) {
        return URLBuilder.from(url)
                .setProtocol(url.getParameter(REGISTRY_KEY, DEFAULT_REGISTRY))
                .removeParameter(REGISTRY_KEY)
                .build();
    }

    protected <T> Invoker<T> doRefer(Cluster cluster, Registry registry, Class<T> type, URL url) {
//        ClusterInvoker<T> invoker = getInvoker(cluster, registry, type, url);
        ClusterInvoker<T> migrationInvoker = new MigrationInvoker<>(this, cluster, registry, type, url);
        return interceptInvoker(migrationInvoker, url);
    }

    protected <T> ClusterInvoker<T> getInvoker(Cluster cluster, Registry registry, Class<T> type, URL url) {
        DynamicDirectory<T> directory = new RegistryDirectory<>(type, url);
        return doCreateInvoker(directory, cluster, registry, type);
    }

    protected <T> ClusterInvoker<T> getServiceDiscoveryInvoker(Cluster cluster, Class<T> type, URL url) {
        Registry registry = registryFactory.getRegistry(super.getRegistryUrl(url));
        DynamicDirectory<T> directory = new ServiceDiscoveryRegistryDirectory<>(type, url);
        return doCreateInvoker(directory, cluster, registry, type);
    }

    public static class MigrationInvoker<T> implements ClusterInvoker<T> {
        private static final ExecutorService executor = Executors.newSingleThreadExecutor();

        private URL url;
        private Cluster cluster;
        private Registry registry;
        private Class<T> type;
        private InterfaceCompatibleRegistryProtocol registryProtocol;

        private ClusterInvoker<T> invoker;
        private ClusterInvoker<T> serviceDiscoveryInvoker;

        public MigrationInvoker(InterfaceCompatibleRegistryProtocol registryProtocol,
                                Cluster cluster,
                                Registry registry,
                                Class<T> type,
                                URL url) {
            this(null, null, registryProtocol, cluster, registry, type, url);
        }

        public MigrationInvoker(ClusterInvoker<T> invoker,
                                ClusterInvoker<T> serviceDiscoveryInvoker,
                                InterfaceCompatibleRegistryProtocol registryProtocol,
                                Cluster cluster,
                                Registry registry,
                                Class<T> type,
                                URL url) {
            this.invoker = invoker;
            this.serviceDiscoveryInvoker = serviceDiscoveryInvoker;
            this.registryProtocol = registryProtocol;
            this.cluster = cluster;
            this.registry = registry;
            this.type = type;
            this.url = url;
        }

        public ClusterInvoker<T> getInvoker() {
            return invoker;
        }

        public void setInvoker(ClusterInvoker<T> invoker) {
            this.invoker = invoker;
        }

        public ClusterInvoker<T> getServiceDiscoveryInvoker() {
            return serviceDiscoveryInvoker;
        }

        public void setServiceDiscoveryInvoker(ClusterInvoker<T> serviceDiscoveryInvoker) {
            this.serviceDiscoveryInvoker = serviceDiscoveryInvoker;
        }

        @Override
        public Class<T> getInterface() {
            return invoker.getInterface();
        }

        private long count;
        private boolean forceMigrate;

        public boolean isForceMigrate() {
            return forceMigrate;
        }

        public void setForceMigrate(boolean forceMigrate) {
            this.forceMigrate = forceMigrate;
        }

        public synchronized void migrateToServiceDiscoveryInvoker(boolean forceMigrate) {
            setForceMigrate(forceMigrate);
            if (!forceMigrate) {
                refreshServiceDiscoveryInvoker();
                refreshInterfaceInvoker();
            } else {
                refreshServiceDiscoveryInvoker();
                destroyInterfaceInvoker();
            }
        }

        public synchronized void fallbackToInterfaceInvoker() {
            refreshInterfaceInvoker();

            destroyServiceDiscoveryInvoker();
        }

        private synchronized void destroyServiceDiscoveryInvoker() {
            if (serviceDiscoveryInvoker != null) {
                serviceDiscoveryInvoker.destroy();
                serviceDiscoveryInvoker = null;
            }
        }

        private synchronized void refreshServiceDiscoveryInvoker() {
            if (serviceDiscoveryInvoker == null || serviceDiscoveryInvoker.isDestroyed()) {
                serviceDiscoveryInvoker = registryProtocol.getServiceDiscoveryInvoker(cluster, type, url);
            }
        }

        private synchronized void refreshInterfaceInvoker() {
            if (invoker == null || invoker.isDestroyed()) {
                invoker = registryProtocol.getInvoker(cluster, registry, type, url);
            }
        }

        private synchronized void destroyInterfaceInvoker() {
            if (invoker != null) {
                invoker.destroy();
                invoker = null;
            }
        }

        @Override
        public Result invoke(Invocation invocation) throws RpcException {
            if (serviceDiscoveryInvoker == null || serviceDiscoveryInvoker.isDestroyed()) {
                return invoker.invoke(invocation);
            }

            if (invoker == null || invoker.isDestroyed()) {
                return serviceDiscoveryInvoker.invoke(invocation);
            }

            if (!isForceMigrate()) {
                Set<MigrationDetector> detectors = ExtensionLoader.getExtensionLoader(MigrationDetector.class).getSupportedExtensionInstances();
                if (detectors.stream().allMatch(migrationDetector -> migrationDetector.shouldMigrate(serviceDiscoveryInvoker, invoker))) {
                    destroyInterfaceInvoker();
                    // try to destroy asynchronously
//                    executor.submit(() -> {
//                        if ()
//                        destroyInterfaceInvoker();
//                    });
                    return serviceDiscoveryInvoker.invoke(invocation);
                }
            } else {
                return serviceDiscoveryInvoker.invoke(invocation);
            }

            return invoker.invoke(invocation);
        }

        @Override
        public URL getUrl() {
            return invoker.getUrl();
        }

        @Override
        public boolean isAvailable() {
            return invoker.isAvailable() || serviceDiscoveryInvoker.isAvailable();
        }

        @Override
        public void destroy() {
            invoker.destroy();
            serviceDiscoveryInvoker.destroy();
        }

        @Override
        public URL getRegistryUrl() {
            return invoker.getRegistryUrl();
        }

        @Override
        public Directory<T> getDirectory() {
            return invoker.getDirectory();
        }

        @Override
        public boolean isDestroyed() {
            return invoker.isDestroyed() && serviceDiscoveryInvoker.isDestroyed();
        }
    }

}
