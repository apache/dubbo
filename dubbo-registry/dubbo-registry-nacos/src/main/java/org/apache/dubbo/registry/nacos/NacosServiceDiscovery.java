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
package org.apache.dubbo.registry.nacos;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.function.ThrowableFunction;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.registry.client.AbstractServiceDiscovery;
import org.apache.dubbo.registry.client.ServiceDiscovery;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;
import org.apache.dubbo.registry.nacos.util.NacosNamingServiceUtils;
import org.apache.dubbo.rpc.model.ApplicationModel;

import com.alibaba.nacos.api.common.Constants;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.ListView;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.function.ThrowableConsumer.execute;
import static org.apache.dubbo.registry.nacos.util.NacosNamingServiceUtils.createNamingService;
import static org.apache.dubbo.registry.nacos.util.NacosNamingServiceUtils.toInstance;

/**
 * Nacos {@link ServiceDiscovery} implementation
 *
 * @see ServiceDiscovery
 * @since 2.7.5
 */
public class NacosServiceDiscovery extends AbstractServiceDiscovery {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private NacosNamingServiceWrapper namingService;

    public NacosServiceDiscovery(ApplicationModel applicationModel, URL registryURL) {
        super(applicationModel, registryURL);
        this.namingService = createNamingService(registryURL);
    }

    @Override
    public void doDestroy() throws Exception {
        this.namingService.shutdown();
    }

    @Override
    public void doRegister(ServiceInstance serviceInstance) {
        execute(namingService, service -> {
            Instance instance = toInstance(serviceInstance);
            // Should not register real group for ServiceInstance
            // Or will cause consumer unable to fetch all of the providers from every group
            // Provider's group is invisible for consumer
            service.registerInstance(instance.getServiceName(), Constants.DEFAULT_GROUP, instance);
        });
    }

    @Override
    public void doUnregister(ServiceInstance serviceInstance) throws RuntimeException {
        execute(namingService, service -> {
            Instance instance = toInstance(serviceInstance);
            // Should not register real group for ServiceInstance
            // Or will cause consumer unable to fetch all of the providers from every group
            // Provider's group is invisible for consumer
            service.deregisterInstance(instance.getServiceName(), Constants.DEFAULT_GROUP, instance);
        });
    }

    @Override
    public Set<String> getServices() {
        return ThrowableFunction.execute(namingService, service -> {
            // Should not register real group for ServiceInstance
            // Or will cause consumer unable to fetch all of the providers from every group
            // Provider's group is invisible for consumer
            ListView<String> view = service.getServicesOfServer(0, Integer.MAX_VALUE, Constants.DEFAULT_GROUP);
            return new LinkedHashSet<>(view.getData());
        });
    }

    @Override
    public List<ServiceInstance> getInstances(String serviceName) throws NullPointerException {
        return ThrowableFunction.execute(namingService, service ->
            service.selectInstances(serviceName, Constants.DEFAULT_GROUP, true)
                .stream().map((i) -> NacosNamingServiceUtils.toServiceInstance(registryURL, i))
                .collect(Collectors.toList())
        );
    }

    @Override
    public void addServiceInstancesChangedListener(ServiceInstancesChangedListener listener)
        throws NullPointerException, IllegalArgumentException {
        // check if listener has already been added through another interface/service
        if (!instanceListeners.add(listener)) {
            return;
        }
        execute(namingService, service -> listener.getServiceNames().forEach(serviceName -> {
            try {
                service.subscribe(serviceName, Constants.DEFAULT_GROUP, e -> { // Register Nacos EventListener
                    if (e instanceof NamingEvent) {
                        NamingEvent event = (NamingEvent) e;
                        handleEvent(event, listener);
                    }
                });
            } catch (NacosException e) {
                logger.error("add nacos service instances changed listener fail ", e);
            }
        }));
    }

    @Override
    public URL getUrl() {
        return registryURL;
    }

    private void handleEvent(NamingEvent event, ServiceInstancesChangedListener listener) {
        String serviceName = event.getServiceName();
        List<ServiceInstance> serviceInstances = event.getInstances()
            .stream()
            .map((i) -> NacosNamingServiceUtils.toServiceInstance(registryURL, i))
            .collect(Collectors.toList());
        listener.onEvent(new ServiceInstancesChangedEvent(serviceName, serviceInstances));
    }
}
