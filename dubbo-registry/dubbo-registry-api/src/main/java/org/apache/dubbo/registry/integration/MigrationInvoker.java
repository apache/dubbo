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
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.client.RegistryProtocol;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.Cluster;
import org.apache.dubbo.rpc.cluster.ClusterInvoker;
import org.apache.dubbo.rpc.cluster.Directory;

import java.util.Set;

public class MigrationInvoker<T> implements ClusterInvoker<T> {

    private URL url;
    private Cluster cluster;
    private Registry registry;
    private Class<T> type;
    private RegistryProtocol registryProtocol;

    private ClusterInvoker<T> invoker;
    private ClusterInvoker<T> serviceDiscoveryInvoker;

    public MigrationInvoker(RegistryProtocol registryProtocol,
                            Cluster cluster,
                            Registry registry,
                            Class<T> type,
                            URL url) {
        this(null, null, registryProtocol, cluster, registry, type, url);
    }

    public MigrationInvoker(ClusterInvoker<T> invoker,
                            ClusterInvoker<T> serviceDiscoveryInvoker,
                            RegistryProtocol registryProtocol,
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
        return type;
    }

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
            // any of the address list changes, compare these two lists.
            ((DynamicDirectory) invoker.getDirectory()).addInvokersChangedListener(this::checkAddresses);
            ((DynamicDirectory) serviceDiscoveryInvoker.getDirectory()).addInvokersChangedListener(this::checkAddresses);
            this.checkAddresses();
        } else {
            refreshServiceDiscoveryInvoker();
            destroyInterfaceInvoker();
        }
    }

    private synchronized void checkAddresses() {
        Set<MigrationAddressComparator> detectors = ExtensionLoader.getExtensionLoader(MigrationAddressComparator.class).getSupportedExtensionInstances();
        if (detectors != null && detectors.stream().allMatch(migrationDetector -> migrationDetector.shouldMigrate(serviceDiscoveryInvoker, invoker))) {
            discardInterfaceInvokerAddress();
        } else {
            discardServiceDiscoveryInvokerAddress();
        }
    }

    public synchronized void fallbackToInterfaceInvoker() {
        refreshInterfaceInvoker();
        destroyServiceDiscoveryInvoker();
    }

    protected synchronized void destroyServiceDiscoveryInvoker() {
        if (serviceDiscoveryInvoker != null) {
            serviceDiscoveryInvoker.destroy();
            serviceDiscoveryInvoker = null;
        }
    }

    protected synchronized void discardServiceDiscoveryInvokerAddress() {
        if (serviceDiscoveryInvoker != null) {
            serviceDiscoveryInvoker.getDirectory().discordAddresses();
        }
    }

    protected synchronized void refreshServiceDiscoveryInvoker() {
        if (needRefresh(serviceDiscoveryInvoker)) {
            serviceDiscoveryInvoker = registryProtocol.getServiceDiscoveryInvoker(cluster, registry, type, url);
        }
    }

    protected synchronized void refreshInterfaceInvoker() {
        if (needRefresh(invoker)) {
            // FIXME invoker.destroy();
            invoker = registryProtocol.getInvoker(cluster, registry, type, url);
        }
    }

    protected synchronized void destroyInterfaceInvoker() {
        if (invoker != null) {
            invoker.destroy();
            invoker = null;
        }
    }

    protected synchronized void discardInterfaceInvokerAddress() {
        if (invoker != null) {
            invoker.getDirectory().discordAddresses();
        }
    }

    private boolean needRefresh(ClusterInvoker<T> invoker) {
        return invoker == null || invoker.isDestroyed() || !invoker.isAvailable();
    }

    @Override
    public Result invoke(Invocation invocation) throws RpcException {
        if (needRefresh(serviceDiscoveryInvoker)) {
            return invoker.invoke(invocation);
        }

        if (needRefresh(invoker)) {
            return serviceDiscoveryInvoker.invoke(invocation);
        }

        throw new IllegalStateException("Service discovery invoker and Interface invoker should has at least one being available, " + invocation.getServiceName());
    }

    @Override
    public URL getUrl() {
        if (invoker != null) {
            return invoker.getUrl();
        }
        return serviceDiscoveryInvoker.getUrl();
    }

    @Override
    public boolean isAvailable() {
        return (invoker != null && invoker.isAvailable())
                || (serviceDiscoveryInvoker != null && serviceDiscoveryInvoker.isAvailable());
    }

    @Override
    public void destroy() {
        if (invoker != null) {
            invoker.destroy();
        }
        if (serviceDiscoveryInvoker != null) {
            serviceDiscoveryInvoker.destroy();
        }
    }

    @Override
    public URL getRegistryUrl() {
        if (invoker != null) {
            return invoker.getRegistryUrl();
        }
        return serviceDiscoveryInvoker.getRegistryUrl();
    }

    @Override
    public Directory<T> getDirectory() {
        if (invoker != null) {
            return invoker.getDirectory();
        }
        return serviceDiscoveryInvoker.getDirectory();
    }

    @Override
    public boolean isDestroyed() {
        return (invoker == null || invoker.isDestroyed())
                && (serviceDiscoveryInvoker == null || serviceDiscoveryInvoker.isDestroyed());
    }
}
