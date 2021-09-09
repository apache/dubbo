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

import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.registry.RegistryNotifier;
import org.apache.dubbo.registry.client.ServiceDiscovery;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;
import org.apache.dubbo.rpc.model.ScopeModelUtil;

import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import static org.apache.zookeeper.Watcher.Event.EventType.NodeChildrenChanged;
import static org.apache.zookeeper.Watcher.Event.EventType.NodeDataChanged;

/**
 * Zookeeper {@link ServiceDiscovery} Change {@link CuratorWatcher watcher} only interests in
 * {@link Watcher.Event.EventType#NodeChildrenChanged} and {@link Watcher.Event.EventType#NodeDataChanged} event types,
 * which will multicast a {@link ServiceInstancesChangedEvent} when the service node has been changed.
 *
 * @since 2.7.5
 */
public class ZookeeperServiceDiscoveryChangeWatcher implements CuratorWatcher {
    private Set<ServiceInstancesChangedListener> listeners = new ConcurrentHashSet<>();

    private final ZookeeperServiceDiscovery zookeeperServiceDiscovery;

    private final RegistryNotifier notifier;

    private volatile boolean keepWatching = true;

    private final String serviceName;

    private final String path;

    private CountDownLatch latch;

    public ZookeeperServiceDiscoveryChangeWatcher(ZookeeperServiceDiscovery zookeeperServiceDiscovery,
                                                  String serviceName,
                                                  String path,
                                                  CountDownLatch latch) {
        this.zookeeperServiceDiscovery = zookeeperServiceDiscovery;
        this.serviceName = serviceName;
        this.path = path;
        this.latch = latch;
        this.notifier = new RegistryNotifier(zookeeperServiceDiscovery.getUrl(), zookeeperServiceDiscovery.getDelay(),
            ScopeModelUtil.getApplicationModel(zookeeperServiceDiscovery.getUrl().getScopeModel()).getApplicationExecutorRepository().getServiceDiscoveryAddressNotificationExecutor()) {
            @Override
            protected void doNotify(Object rawAddresses) {
                listeners.forEach(listener -> listener.onEvent((ServiceInstancesChangedEvent) rawAddresses));
            }
        };
    }

    @Override
    public void process(WatchedEvent event) throws Exception {
        try {
            latch.await();
        } catch (InterruptedException e) {
        }

        Watcher.Event.EventType eventType = event.getType();

        if (NodeChildrenChanged.equals(eventType) || NodeDataChanged.equals(eventType)) {
            if (shouldKeepWatching()) {
                zookeeperServiceDiscovery.reRegisterWatcher(this);
                List<ServiceInstance> instanceList = zookeeperServiceDiscovery.getInstances(serviceName);
                notifier.notify(new ServiceInstancesChangedEvent(serviceName, instanceList));
            }
        }
    }

    public String getPath() {
        return path;
    }

    public void addListener(ServiceInstancesChangedListener listener) {
        listeners.add(listener);
    }

    public boolean shouldKeepWatching() {
        return keepWatching;
    }

    public void stopWatching() {
        this.keepWatching = false;
    }
}
