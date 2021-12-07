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
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ScopeModelUtil;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.ListView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.internal.util.collections.Sets;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for NacosServiceDiscovery
 */
public class NacosServiceDiscoveryTest {

    private static final String SERVICE_NAME = "NACOS_SERVICE";

    private static final String LOCALHOST = "127.0.0.1";

    private URL registryUrl;

    private NacosServiceDiscovery nacosServiceDiscovery;

    private NacosNamingServiceWrapper namingServiceWrapper;

    private DefaultServiceInstance createServiceInstance(String serviceName, String host, int port) {
        return new DefaultServiceInstance(serviceName, host, port, ScopeModelUtil.getApplicationModel(registryUrl.getScopeModel()));
    }

    @BeforeEach
    public void init() throws Exception {
        ApplicationModel applicationModel = ApplicationModel.defaultModel();
        applicationModel.getApplicationConfigManager().setApplication(new ApplicationConfig(SERVICE_NAME));

        this.registryUrl = URL.valueOf("nacos://127.0.0.1:" + NetUtils.getAvailablePort());
        registryUrl.setScopeModel(applicationModel);

//        this.nacosServiceDiscovery = new NacosServiceDiscovery(SERVICE_NAME, registryUrl);
        this.nacosServiceDiscovery = new NacosServiceDiscovery(applicationModel, registryUrl);
        Field namingService = nacosServiceDiscovery.getClass().getDeclaredField("namingService");
        namingService.setAccessible(true);
        namingServiceWrapper = mock(NacosNamingServiceWrapper.class);
        namingService.set(nacosServiceDiscovery, namingServiceWrapper);
    }

    @AfterEach
    public void destroy() throws Exception {
        nacosServiceDiscovery.destroy();
    }

    @Test
    public void testDoRegister() throws NacosException {
        DefaultServiceInstance serviceInstance = createServiceInstance(SERVICE_NAME, LOCALHOST, NetUtils.getAvailablePort());
        // register
        nacosServiceDiscovery.doRegister(serviceInstance);

        List<Instance> instances = new ArrayList<>();
        Instance instance1 = new Instance();
        Instance instance2 = new Instance();
        Instance instance3 = new Instance();

        instances.add(instance1);
        instances.add(instance2);
        instances.add(instance3);

        ArgumentCaptor<Instance> instance = ArgumentCaptor.forClass(Instance.class);
        verify(namingServiceWrapper, times(1)).registerInstance(any(), any(), instance.capture());

        when(namingServiceWrapper.getAllInstances(anyString(), anyString())).thenReturn(instances);
        assertEquals(3, namingServiceWrapper.getAllInstances(anyString(), anyString()).size());
    }

    @Test
    public void testDoUnRegister() throws NacosException {
        // register
        nacosServiceDiscovery.register(URL.valueOf("dubbo://10.0.2.3:20880/DemoService?interface=DemoService"));
        nacosServiceDiscovery.register();

        List<Instance> instances = new ArrayList<>();
        Instance instance1 = new Instance();
        Instance instance2 = new Instance();
        Instance instance3 = new Instance();

        instances.add(instance1);
        instances.add(instance2);
        instances.add(instance3);

        ArgumentCaptor<Instance> instance = ArgumentCaptor.forClass(Instance.class);
        verify(namingServiceWrapper, times(1)).registerInstance(any(), any(), instance.capture());

        when(namingServiceWrapper.getAllInstances(anyString(), anyString())).thenReturn(instances);
        assertEquals(3, namingServiceWrapper.getAllInstances(anyString(), anyString()).size());

        // unRegister
        nacosServiceDiscovery.unregister();
    }

    @Test
    public void testGetServices() throws NacosException {
        DefaultServiceInstance serviceInstance = createServiceInstance(SERVICE_NAME, LOCALHOST, NetUtils.getAvailablePort());
        // register
        nacosServiceDiscovery.doRegister(serviceInstance);

        ArgumentCaptor<Instance> instance = ArgumentCaptor.forClass(Instance.class);
        verify(namingServiceWrapper, times(1)).registerInstance(any(), any(), instance.capture());

        String serviceNameWithoutVersion = "providers:org.apache.dubbo.registry.nacos.NacosService:default";
        String serviceName = "providers:org.apache.dubbo.registry.nacos.NacosService:1.0.0:default";
        List<String> serviceNames = new ArrayList<>();
        serviceNames.add(serviceNameWithoutVersion);
        serviceNames.add(serviceName);
        ListView<String> result = new ListView<>();
        result.setData(serviceNames);
        when(namingServiceWrapper.getServicesOfServer(anyInt(), anyInt(), anyString())).thenReturn(result);
        Set<String> services = nacosServiceDiscovery.getServices();
        assertEquals(2, services.size());
    }

    @Test
    public void testAddServiceInstancesChangedListener() {
        List<ServiceInstance> serviceInstances = new LinkedList<>();
        // Add Listener
        nacosServiceDiscovery.addServiceInstancesChangedListener(
            new ServiceInstancesChangedListener(Sets.newSet(SERVICE_NAME), nacosServiceDiscovery) {
                @Override
                public void onEvent(ServiceInstancesChangedEvent event) {
                    serviceInstances.addAll(event.getServiceInstances());
                }
            });

        nacosServiceDiscovery.register();
        nacosServiceDiscovery.update();
        nacosServiceDiscovery.unregister();

        assertTrue(serviceInstances.isEmpty());
    }
}
