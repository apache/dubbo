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
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.function.ThrowableFunction;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.registry.client.AbstractServiceDiscovery;
import org.apache.dubbo.registry.client.ServiceDiscovery;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;
import org.apache.dubbo.registry.nacos.util.NacosNamingServiceUtils;
import org.apache.dubbo.rpc.model.ApplicationModel;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.ListView;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.alibaba.nacos.api.common.Constants.DEFAULT_GROUP;
import static org.apache.dubbo.common.function.ThrowableConsumer.execute;
import static org.apache.dubbo.registry.nacos.util.NacosNamingServiceUtils.createNamingService;
import static org.apache.dubbo.registry.nacos.util.NacosNamingServiceUtils.getGroup;
import static org.apache.dubbo.registry.nacos.util.NacosNamingServiceUtils.toInstance;

/**
 * Nacos {@link ServiceDiscovery} implementation
 *
 * @see ServiceDiscovery
 * @since 2.7.5
 */
public class NacosServiceDiscovery extends AbstractServiceDiscovery {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String group;

    private final NacosNamingServiceWrapper namingService;

    private static final String NACOS_SD_USE_DEFAULT_GROUP_KEY = "dubbo.nacos-service-discovery.use-default-group";

    private final ConcurrentHashMap<String, NacosEventListener> eventListeners = new ConcurrentHashMap<>();

    public NacosServiceDiscovery(ApplicationModel applicationModel, URL registryURL) {
        super(applicationModel, registryURL);
        this.namingService = createNamingService(registryURL);
        // backward compatibility for 3.0.x
        this.group = Boolean.parseBoolean(ConfigurationUtils.getProperty(applicationModel, NACOS_SD_USE_DEFAULT_GROUP_KEY, "false")) ?
            DEFAULT_GROUP: getGroup(registryURL);
    }

    @Override
    public void doDestroy() throws Exception {
        this.namingService.shutdown();
    }

    @Override
    public void doRegister(ServiceInstance serviceInstance) {
        execute(namingService, service -> {
            Instance instance = toInstance(serviceInstance);
            service.registerInstance(instance.getServiceName(), group, instance);
        });
    }

    @Override
    public void doUnregister(ServiceInstance serviceInstance) throws RuntimeException {
        execute(namingService, service -> {
            Instance instance = toInstance(serviceInstance);
            service.deregisterInstance(instance.getServiceName(), group, instance);
        });
    }

    @Override
    public Set<String> getServices() {
        return ThrowableFunction.execute(namingService, service -> {
            ListView<String> view = service.getServicesOfServer(0, Integer.MAX_VALUE, group);
            return new LinkedHashSet<>(view.getData());
        });
    }

    @Override
    public List<ServiceInstance> getInstances(String serviceName) throws NullPointerException {
        return ThrowableFunction.execute(namingService, service ->
            service.selectInstances(serviceName, group, true)
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
        for (String serviceName : listener.getServiceNames()) {
            NacosEventListener nacosEventListener = eventListeners.get(serviceName);
            if (nacosEventListener != null) {
                nacosEventListener.addListener(listener);
            } else {
                try {
                    nacosEventListener = new NacosEventListener();
                    nacosEventListener.addListener(listener);
                    namingService.subscribe(serviceName, group, nacosEventListener);
                    eventListeners.put(serviceName, nacosEventListener);
                } catch (NacosException e) {
                    logger.error("add nacos service instances changed listener fail ", e);
                }
            }
        }
    }

    @Override
    public void removeServiceInstancesChangedListener(ServiceInstancesChangedListener listener) throws IllegalArgumentException {
        if (!instanceListeners.remove(listener)) {
            return;
        }
        for (String serviceName : listener.getServiceNames()) {
            NacosEventListener nacosEventListener = eventListeners.get(serviceName);
            if (nacosEventListener != null) {
                nacosEventListener.removeListener(listener);
                if (nacosEventListener.isEmpty()) {
                    eventListeners.remove(serviceName);
                    try {
                        namingService.unsubscribe(serviceName, group, nacosEventListener);
                    } catch (NacosException e) {
                        logger.error("remove nacos service instances changed listener fail ", e);
                    }
                }
            }
        }
    }

    public class NacosEventListener implements EventListener {
        private final Set<ServiceInstancesChangedListener> listeners = new ConcurrentHashSet<>();

        @Override
        public void onEvent(Event e) {
            if (e instanceof NamingEvent) {
                for (ServiceInstancesChangedListener listener : listeners) {
                    NamingEvent event = (NamingEvent) e;
                    handleEvent(event, listener);
                }
            }
        }

        public void addListener(ServiceInstancesChangedListener listener) {
            listeners.add(listener);
        }

        public void removeListener(ServiceInstancesChangedListener listener) {
            listeners.remove(listener);
        }

        public boolean isEmpty() {
            return listeners.isEmpty();
        }
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
