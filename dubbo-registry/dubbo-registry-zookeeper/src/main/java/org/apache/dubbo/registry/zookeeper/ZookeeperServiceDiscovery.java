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
package org.apache.dubbo.registry.zookeeper;

<<<<<<< HEAD
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorWatcher;
=======
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.x.discovery.ServiceCache;
>>>>>>> origin/3.2
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.function.ThrowableConsumer;
import org.apache.dubbo.common.function.ThrowableFunction;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
<<<<<<< HEAD
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.DefaultPage;
import org.apache.dubbo.common.utils.Page;
=======
>>>>>>> origin/3.2
import org.apache.dubbo.registry.client.AbstractServiceDiscovery;
import org.apache.dubbo.registry.client.ServiceDiscovery;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;
<<<<<<< HEAD
import org.apache.zookeeper.KeeperException;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

=======
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.model.ApplicationModel;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_ZOOKEEPER_EXCEPTION;
>>>>>>> origin/3.2
import static org.apache.dubbo.common.function.ThrowableFunction.execute;
import static org.apache.dubbo.metadata.RevisionResolver.EMPTY_REVISION;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.getExportedServicesRevision;
import static org.apache.dubbo.registry.zookeeper.util.CuratorFrameworkUtils.build;
import static org.apache.dubbo.registry.zookeeper.util.CuratorFrameworkUtils.buildCuratorFramework;
import static org.apache.dubbo.registry.zookeeper.util.CuratorFrameworkUtils.buildServiceDiscovery;
import static org.apache.dubbo.registry.zookeeper.util.CuratorFrameworkUtils.getRootPath;
import static org.apache.dubbo.rpc.RpcException.REGISTRY_EXCEPTION;

/**
 * Zookeeper {@link ServiceDiscovery} implementation based on
 * <a href="https://curator.apache.org/curator-x-discovery/index.html">Apache Curator X Discovery</a>
 * <p>
 * TODO, replace curator CuratorFramework with dubbo ZookeeperClient
 */
public class ZookeeperServiceDiscovery extends AbstractServiceDiscovery {

    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(getClass());

<<<<<<< HEAD
    private URL registryURL;
=======
    public static final String DEFAULT_GROUP = "/services";
>>>>>>> origin/3.2

    private final CuratorFramework curatorFramework;

    private final String rootPath;

    private final org.apache.curator.x.discovery.ServiceDiscovery<ZookeeperInstance> serviceDiscovery;

    /**
     * The Key is watched Zookeeper path, the value is an instance of {@link CuratorWatcher}
     */
    private final Map<String, ZookeeperServiceDiscoveryChangeWatcher> watcherCaches = new ConcurrentHashMap<>();

<<<<<<< HEAD
    @Override
    public void initialize(URL registryURL) throws Exception {
        this.registryURL = registryURL;
        this.curatorFramework = buildCuratorFramework(registryURL);
        this.rootPath = ROOT_PATH.getParameterValue(registryURL);
        this.serviceDiscovery = buildServiceDiscovery(curatorFramework, rootPath);
        this.serviceDiscovery.start();
    }

    @Override
    public URL getUrl() {
        return registryURL;
    }

    @Override
    public void destroy() throws Exception {
=======
    public ZookeeperServiceDiscovery(ApplicationModel applicationModel, URL registryURL) {
        super(applicationModel, registryURL);
        try {
            this.curatorFramework = buildCuratorFramework(registryURL, this);
            this.rootPath = getRootPath(registryURL);
            this.serviceDiscovery = buildServiceDiscovery(curatorFramework, rootPath);
            this.serviceDiscovery.start();
        } catch (Exception e) {
            throw new IllegalStateException("Create zookeeper service discovery failed.", e);
        }
    }

    @Override
    public void doDestroy() throws Exception {
>>>>>>> origin/3.2
        serviceDiscovery.close();
        curatorFramework.close();
        watcherCaches.clear();
    }

    @Override
    public void doRegister(ServiceInstance serviceInstance) {
<<<<<<< HEAD
        doInServiceRegistry(serviceDiscovery -> {
=======
        try {
>>>>>>> origin/3.2
            serviceDiscovery.registerService(build(serviceInstance));
        } catch (Exception e) {
            throw new RpcException(REGISTRY_EXCEPTION, "Failed register instance " + serviceInstance.toString(), e);
        }
    }

    @Override
<<<<<<< HEAD
    public void doUpdate(ServiceInstance serviceInstance) {
        doInServiceRegistry(serviceDiscovery -> {
            serviceDiscovery.updateService(build(serviceInstance));
        });
    }

    @Override
    public void unregister(ServiceInstance serviceInstance) throws RuntimeException {
        doInServiceRegistry(serviceDiscovery -> {
            serviceDiscovery.unregisterService(build(serviceInstance));
        });
=======
    public void doUnregister(ServiceInstance serviceInstance) throws RuntimeException {
        if (serviceInstance != null) {
            doInServiceRegistry(serviceDiscovery -> serviceDiscovery.unregisterService(build(serviceInstance)));
        }
    }

    @Override
    protected void doUpdate(ServiceInstance oldServiceInstance, ServiceInstance newServiceInstance) throws RuntimeException {
        if (EMPTY_REVISION.equals(getExportedServicesRevision(newServiceInstance))) {
            super.doUpdate(oldServiceInstance, newServiceInstance);
            return;
        }

        org.apache.curator.x.discovery.ServiceInstance<ZookeeperInstance> oldInstance = build(oldServiceInstance);
        org.apache.curator.x.discovery.ServiceInstance<ZookeeperInstance> newInstance = build(newServiceInstance);
        if (!Objects.equals(newInstance.getName(), oldInstance.getName()) ||
            !Objects.equals(newInstance.getId(), oldInstance.getId())) {
            // Ignore if id changed. Should unregister first.
            super.doUpdate(oldServiceInstance, newServiceInstance);
            return;
        }

        try {
            this.serviceInstance = newServiceInstance;
            reportMetadata(newServiceInstance.getServiceMetadata());
            serviceDiscovery.updateService(newInstance);
        } catch (Exception e) {
            throw new RpcException(REGISTRY_EXCEPTION, "Failed register instance " + newServiceInstance.toString(), e);
        }
>>>>>>> origin/3.2
    }

    @Override
    public Set<String> getServices() {
        return doInServiceDiscovery(s -> new LinkedHashSet<>(s.queryForNames()));
    }

    @Override
    public List<ServiceInstance> getInstances(String serviceName) throws NullPointerException {
        return doInServiceDiscovery(s -> build(registryURL, s.queryForInstances(serviceName)));
    }

    @Override
<<<<<<< HEAD
    public Page<ServiceInstance> getInstances(String serviceName, int offset, int pageSize, boolean healthyOnly) {
        String path = buildServicePath(serviceName);

        return execute(path, p -> {

            List<ServiceInstance> serviceInstances = new LinkedList<>();

            int totalSize = 0;
            try {
                List<String> serviceIds = new LinkedList<>(curatorFramework.getChildren().forPath(p));

                totalSize = serviceIds.size();

                Iterator<String> iterator = serviceIds.iterator();

                for (int i = 0; i < offset; i++) {
                    if (iterator.hasNext()) { // remove the elements from 0 to offset
                        iterator.next();
                        iterator.remove();
                    }
                }

                for (int i = 0; i < pageSize; i++) {
                    if (iterator.hasNext()) {
                        String serviceId = iterator.next();
                        ServiceInstance serviceInstance = build(serviceDiscovery.queryForInstance(serviceName, serviceId));
                        serviceInstances.add(serviceInstance);
                    }
                }

                if (healthyOnly) {
                    Iterator<ServiceInstance> instanceIterator = serviceInstances.iterator();
                    while (instanceIterator.hasNext()) {
                        ServiceInstance instance = instanceIterator.next();
                        if (!instance.isHealthy()) {
                            instanceIterator.remove();
                        }
=======
    public void addServiceInstancesChangedListener(ServiceInstancesChangedListener listener)
        throws NullPointerException, IllegalArgumentException {
        // check if listener has already been added through another interface/service
        if (!instanceListeners.add(listener)) {
            return;
        }
        listener.getServiceNames().forEach(serviceName -> registerServiceWatcher(serviceName, listener));
    }

    @Override
    public void removeServiceInstancesChangedListener(ServiceInstancesChangedListener listener) throws IllegalArgumentException {
        if (!instanceListeners.remove(listener)) {
            return;
        }
        listener.getServiceNames().forEach(serviceName -> {
            ZookeeperServiceDiscoveryChangeWatcher watcher = watcherCaches.get(serviceName);
            if (watcher != null) {
                watcher.getListeners().remove(listener);
                if (watcher.getListeners().isEmpty()) {
                    watcherCaches.remove(serviceName);
                    try {
                        watcher.getCacheInstance().close();
                    } catch (IOException e) {
                        logger.error(REGISTRY_ZOOKEEPER_EXCEPTION, "curator stop watch failed", "",
                            "Curator Stop service discovery watch failed. Service Name: " + serviceName);
>>>>>>> origin/3.2
                    }
                }
            } catch (KeeperException.NoNodeException e) {
                logger.warn(p + " path not exist.", e);
            }
        });
    }

<<<<<<< HEAD
    @Override
    public void addServiceInstancesChangedListener(ServiceInstancesChangedListener listener)
            throws NullPointerException, IllegalArgumentException {
        listener.getServiceNames().forEach(serviceName -> registerServiceWatcher(serviceName, listener));
    }

    @Override
    public void removeServiceInstancesChangedListener(ServiceInstancesChangedListener listener) throws IllegalArgumentException {
        listener.getServiceNames().forEach(serviceName -> {
            ZookeeperServiceDiscoveryChangeWatcher watcher = watcherCaches.remove(serviceName);
            watcher.stopWatching();
        });
    }
=======
>>>>>>> origin/3.2

    private void doInServiceRegistry(ThrowableConsumer<org.apache.curator.x.discovery.ServiceDiscovery> consumer) {
        ThrowableConsumer.execute(serviceDiscovery, s -> consumer.accept(s));
    }

    private <R> R doInServiceDiscovery(ThrowableFunction<org.apache.curator.x.discovery.ServiceDiscovery, R> function) {
        return execute(serviceDiscovery, function);
    }

    protected void registerServiceWatcher(String serviceName, ServiceInstancesChangedListener listener) {
<<<<<<< HEAD
        String path = buildServicePath(serviceName);
        try {
            curatorFramework.create().creatingParentsIfNeeded().forPath(path);
        } catch (KeeperException.NodeExistsException e) {
            // ignored
            if (logger.isDebugEnabled()) {

                logger.debug(e);
            }
        } catch (Exception e) {
            throw new IllegalStateException("registerServiceWatcher create path=" + path + " fail.", e);
        }

        CuratorWatcher prev = watcherCaches.get(path);
        CuratorWatcher watcher = watcherCaches.computeIfAbsent(path, key ->
                new ZookeeperServiceDiscoveryChangeWatcher(this, serviceName, listener));
        try {
            List<String> addresses = curatorFramework.getChildren().usingWatcher(watcher).forPath(path);
            // notify instance changed first time,
            // in the multi-registry scenario,
            // we need to merge pushed instances
            if ((prev == null || watcher != prev)
                    && CollectionUtils.isNotEmpty(addresses)) {
                listener.onEvent(new ServiceInstancesChangedEvent(serviceName, this.getInstances(serviceName)));
            }
        } catch (KeeperException.NoNodeException e) {
            // ignored
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage());
=======
        CountDownLatch latch = new CountDownLatch(1);

        ZookeeperServiceDiscoveryChangeWatcher watcher = watcherCaches.computeIfAbsent(serviceName, name -> {
            ServiceCache<ZookeeperInstance> serviceCache = serviceDiscovery.serviceCacheBuilder()
                .name(name)
                .build();
            ZookeeperServiceDiscoveryChangeWatcher newer = new ZookeeperServiceDiscoveryChangeWatcher(this, serviceCache, name, latch);
            serviceCache.addListener(newer);

            try {
                serviceCache.start();
            } catch (Exception e) {
                throw new RpcException(REGISTRY_EXCEPTION, "Failed subscribe service: " + name, e);
>>>>>>> origin/3.2
            }

<<<<<<< HEAD
    private String buildServicePath(String serviceName) {
        return rootPath + "/" + serviceName;
=======
            return newer;
        });

        watcher.addListener(listener);
        listener.onEvent(new ServiceInstancesChangedEvent(serviceName, this.getInstances(serviceName)));
        latch.countDown();
>>>>>>> origin/3.2
    }
}
