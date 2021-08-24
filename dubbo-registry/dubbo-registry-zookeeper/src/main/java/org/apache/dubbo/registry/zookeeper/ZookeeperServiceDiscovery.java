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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.function.ThrowableConsumer;
import org.apache.dubbo.common.function.ThrowableFunction;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.DefaultPage;
import org.apache.dubbo.common.utils.Page;
import org.apache.dubbo.registry.client.AbstractServiceDiscovery;
import org.apache.dubbo.registry.client.ServiceDiscovery;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;
import org.apache.dubbo.rpc.RpcException;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.zookeeper.KeeperException;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import static org.apache.dubbo.common.function.ThrowableFunction.execute;
import static org.apache.dubbo.registry.zookeeper.util.CuratorFrameworkParams.ROOT_PATH;
import static org.apache.dubbo.registry.zookeeper.util.CuratorFrameworkUtils.build;
import static org.apache.dubbo.registry.zookeeper.util.CuratorFrameworkUtils.buildCuratorFramework;
import static org.apache.dubbo.registry.zookeeper.util.CuratorFrameworkUtils.buildServiceDiscovery;
import static org.apache.dubbo.rpc.RpcException.REGISTRY_EXCEPTION;

/**
 * Zookeeper {@link ServiceDiscovery} implementation based on
 * <a href="https://curator.apache.org/curator-x-discovery/index.html">Apache Curator X Discovery</a>
 */
public class ZookeeperServiceDiscovery extends AbstractServiceDiscovery {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private URL registryURL;

    private CuratorFramework curatorFramework;

    private String rootPath;

    private org.apache.curator.x.discovery.ServiceDiscovery<ZookeeperInstance> serviceDiscovery;

    /**
     * The Key is watched Zookeeper path, the value is an instance of {@link CuratorWatcher}
     */
    private final Map<String, ZookeeperServiceDiscoveryChangeWatcher> watcherCaches = new ConcurrentHashMap<>();

    @Override
    public void doInitialize(URL registryURL) throws Exception {
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
    public void doDestroy() throws Exception {
        serviceDiscovery.close();
    }

    @Override
    public void doRegister(ServiceInstance serviceInstance) {
        try {
            serviceDiscovery.registerService(build(serviceInstance));
        } catch (Exception e) {
            throw new RpcException(REGISTRY_EXCEPTION, "Failed register instance " + serviceInstance.toString(), e);
        }
    }

    @Override
    public void doUpdate(ServiceInstance serviceInstance) {
        ServiceInstance oldInstance = this.serviceInstance;
        this.unregister(oldInstance);
        this.register(serviceInstance);
    }

    @Override
    public void doUnregister(ServiceInstance serviceInstance) throws RuntimeException {
        doInServiceRegistry(serviceDiscovery -> serviceDiscovery.unregisterService(build(serviceInstance)));
    }


    @Override
    public Set<String> getServices() {
        return doInServiceDiscovery(s -> new LinkedHashSet<>(s.queryForNames()));
    }

    @Override
    public List<ServiceInstance> getInstances(String serviceName) throws NullPointerException {
        return doInServiceDiscovery(s -> build(s.queryForInstances(serviceName)));
    }

    @Override
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
                    serviceInstances.removeIf(instance -> !instance.isHealthy());
                }
            } catch (KeeperException.NoNodeException e) {
                logger.warn(p + " path not exist.", e);
            }

            return new DefaultPage<>(offset, pageSize, serviceInstances, totalSize);
        });
    }

    @Override
    public void addServiceInstancesChangedListener(ServiceInstancesChangedListener listener)
        throws NullPointerException, IllegalArgumentException {
        listener.getServiceNames().forEach(serviceName -> registerServiceWatcher(serviceName, listener));
    }

    @Override
    public void removeServiceInstancesChangedListener(ServiceInstancesChangedListener listener) throws IllegalArgumentException {
        listener.getServiceNames().forEach(serviceName -> {
            ZookeeperServiceDiscoveryChangeWatcher watcher = watcherCaches.remove(buildServicePath(serviceName));
            if (watcher != null) {
                watcher.stopWatching();
            }
        });
    }


    private void doInServiceRegistry(ThrowableConsumer<org.apache.curator.x.discovery.ServiceDiscovery> consumer) {
        ThrowableConsumer.execute(serviceDiscovery, s -> consumer.accept(s));
    }

    private <R> R doInServiceDiscovery(ThrowableFunction<org.apache.curator.x.discovery.ServiceDiscovery, R> function) {
        return execute(serviceDiscovery, function);
    }

    protected void registerServiceWatcher(String serviceName, ServiceInstancesChangedListener listener) {
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

        CountDownLatch latch = new CountDownLatch(1);
        ZookeeperServiceDiscoveryChangeWatcher watcher = watcherCaches.computeIfAbsent(path, key -> {
            ZookeeperServiceDiscoveryChangeWatcher tmpWatcher = new ZookeeperServiceDiscoveryChangeWatcher(this, serviceName, path, latch);
            try {
                curatorFramework.getChildren().usingWatcher(tmpWatcher).forPath(path);
            } catch (KeeperException.NoNodeException e) {
                // ignored
                if (logger.isErrorEnabled()) {
                    logger.error(e.getMessage());
                }
            } catch (Exception e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
            return tmpWatcher;
        });
        watcher.addListener(listener);
        listener.onEvent(new ServiceInstancesChangedEvent(serviceName, this.getInstances(serviceName)));
        latch.countDown();
    }

    public void reRegisterWatcher(ZookeeperServiceDiscoveryChangeWatcher watcher) throws Exception {
        curatorFramework.getChildren().usingWatcher(watcher).forPath(watcher.getPath());
    }

    private String buildServicePath(String serviceName) {
        return rootPath + "/" + serviceName;
    }
}
