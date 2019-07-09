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
package org.apache.dubbo.registry.etcd;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.event.EventListener;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.client.ServiceDiscovery;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;
import org.apache.dubbo.remoting.etcd.ChildListener;
import org.apache.dubbo.remoting.etcd.EtcdClient;
import org.apache.dubbo.remoting.etcd.EtcdTransporter;
import org.apache.dubbo.remoting.etcd.StateListener;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
MetadataServiceProx
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PATH_SEPARATOR;

/**
 * @author cvictory ON 2019-07-08
 */
public class EtcdServiceDiscovery implements ServiceDiscovery, EventListener<ServiceInstancesChangedEvent> {

    private final static Logger logger = LoggerFactory.getLogger(EtcdServiceDiscovery.class);

//    private final String root;

    private final Set<String> anyServices = new ConcurrentHashSet<>();

    private final ConcurrentMap<URL, ConcurrentMap<NotifyListener, ChildListener>> etcdListeners = new ConcurrentHashMap<>();
//    private final EtcdClient etcdClient;

    public EtcdServiceDiscovery(URL url, EtcdTransporter etcdTransporter) {
//        if (url.isAnyHost()) {
//            throw new IllegalStateException("Service discovery address is invalid, actual: '" + url.getHost() + "'");
//        }
////        String group = url.getParameter(GROUP_KEY, DEFAULT_ROOT);
//        if (!group.startsWith(PATH_SEPARATOR)) {
//            group = PATH_SEPARATOR + group;
//        }
//        this.root = group;
//        etcdClient = etcdTransporter.connect(url);
//
//        etcdClient.addStateListener(state -> {
//            if (state == StateListener.CONNECTED) {
//                try {
////                    recover();
//                } catch (Exception e) {
//                    logger.error(e.getMessage(), e);
//                }
//            }
//        });
    }

    @Override
    public void onEvent(ServiceInstancesChangedEvent event) {

    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void register(ServiceInstance serviceInstance) throws RuntimeException {

    }

    @Override
    public void update(ServiceInstance serviceInstance) throws RuntimeException {

    }

    @Override
    public void unregister(ServiceInstance serviceInstance) throws RuntimeException {

    }

    @Override
    public Set<String> getServices() {
        return null;
    }

    @Override
    public void addServiceInstancesChangedListener(String serviceName, ServiceInstancesChangedListener listener) throws NullPointerException, IllegalArgumentException {

    }
}
