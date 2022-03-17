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
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;
import org.apache.dubbo.registry.nacos.util.NacosNamingServiceUtils;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.ListView;
import com.alibaba.nacos.api.naming.utils.NamingUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

    private String group;

    private NacosNamingServiceWrapper namingService;

    private URL registryURL;

    @Override
    public void initialize(URL registryURL) throws Exception {
        this.namingService = createNamingService(registryURL);
        this.group = getGroup(registryURL);
        this.registryURL = registryURL;
    }

    @Override
    public void destroy() {
        this.namingService = null;
    }

    /**
     * 服务自省 注册
     * @param serviceInstance an instance of {@link ServiceInstance} to be registered
     * @throws RuntimeException
     */
    @Override
    public void doRegister(ServiceInstance serviceInstance) {
        execute(namingService, service -> {
            // 转换
            Instance instance = toInstance(serviceInstance);
            appendPreservedParam(instance);
            // 注册
            service.registerInstance(instance.getServiceName(), group, instance);
        });
    }

    /**
     * 更新注册服务
     * @param serviceInstance
     */
    @Override
    public void doUpdate(ServiceInstance serviceInstance) {
        if (this.serviceInstance == null) {
            /**
             * 之前没有注册服务  则直接注册
             */
            register(serviceInstance);
        } else {
            /**
             * 注销服务
             */
            unregister(serviceInstance);
            /**
             * 重新注册
             */
            register(serviceInstance);
        }
    }

    /**
     * 注销服务
     * @param serviceInstance an instance of {@link ServiceInstance} to be unregistered
     * @throws RuntimeException
     */
    @Override
    public void unregister(ServiceInstance serviceInstance) throws RuntimeException {
        execute(namingService, service -> {
            Instance instance = toInstance(serviceInstance);
            /**
             * 注销服务
             */
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

    /**
     * 在nacos（注册中心）中获取serviceName对应的服务实例
     * @param serviceName the service name
     * @return
     * @throws NullPointerException
     */
    @Override
    public List<ServiceInstance> getInstances(String serviceName) throws NullPointerException {
        return ThrowableFunction.execute(namingService, service ->
                /**
                 * 在nacos（注册中心）中获取serviceName对应的服务实例
                 * 优先从本地缓存获取
                 */
                service.selectInstances(serviceName, true)
                        .stream().map(NacosNamingServiceUtils::toServiceInstance)
                        .collect(Collectors.toList())
        );
    }

    @Override
    public void addServiceInstancesChangedListener(ServiceInstancesChangedListener listener)
            throws NullPointerException, IllegalArgumentException {
        //异步执行
        execute(namingService, service -> {
            listener.getServiceNames().forEach(serviceName -> {
                try {
                    // 注册中心监听  返回服务对应的实例
                    service.subscribe(serviceName, e -> { // Register Nacos EventListener
                        if (e instanceof NamingEvent) {
                            NamingEvent event = (NamingEvent) e;
                            // 回调   处理返回的实例
                            handleEvent(event, listener);
                        }
                    });
                } catch (NacosException e) {
                    e.printStackTrace();
                }
            });
        });
    }

    @Override
    public URL getUrl() {
        return registryURL;
    }

    private void handleEvent(NamingEvent event, ServiceInstancesChangedListener listener) {
        String serviceName = NamingUtils.getServiceName(event.getServiceName());
        /**
         * Instance列表 转换ServiceInstance列表
         */
        List<ServiceInstance> serviceInstances = event.getInstances()
                .stream()
                // 转换
                .map(NacosNamingServiceUtils::toServiceInstance)
                .collect(Collectors.toList());
        /**
         * 发布事件   再次异步
         */
        dispatchServiceInstancesChangedEvent(serviceName, serviceInstances);
    }

    private void appendPreservedParam(Instance instance) {
        Map<String, String> preservedParam = NacosNamingServiceUtils.getNacosPreservedParam(getUrl());
        instance.getMetadata().putAll(preservedParam);
    }
}
