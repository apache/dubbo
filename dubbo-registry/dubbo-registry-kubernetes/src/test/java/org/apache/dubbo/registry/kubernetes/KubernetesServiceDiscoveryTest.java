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
package org.apache.dubbo.registry.kubernetes;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;
import org.apache.dubbo.registry.kubernetes.util.KubernetesClientConst;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ScopeModelUtil;

import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.EndpointsBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

@ExtendWith({MockitoExtension.class})
public class KubernetesServiceDiscoveryTest {
    public KubernetesServer mockServer = new KubernetesServer(false, true);

    private NamespacedKubernetesClient mockClient;

    private ServiceInstancesChangedListener mockListener = Mockito.mock(ServiceInstancesChangedListener.class);

    private URL serverUrl;

    private Map<String, String> selector;

    @BeforeEach
    public void setUp() {
        mockServer.before();
        mockClient = mockServer.getClient();

        serverUrl = URL.valueOf(mockClient.getConfiguration().getMasterUrl())
                .setProtocol("kubernetes")
                .addParameter(KubernetesClientConst.USE_HTTPS, "false")
                .addParameter(KubernetesClientConst.HTTP2_DISABLE, "true");
        serverUrl.setScopeModel(ApplicationModel.defaultModel());

        System.setProperty(Config.KUBERNETES_AUTH_TRYKUBECONFIG_SYSTEM_PROPERTY, "false");
        System.setProperty(Config.KUBERNETES_AUTH_TRYSERVICEACCOUNT_SYSTEM_PROPERTY, "false");

        selector = new HashMap<>(4);
        selector.put("l", "v");
        Pod pod = new PodBuilder()
                .withNewMetadata().withName("TestServer").withLabels(selector).endMetadata()
                .build();

        Service service = new ServiceBuilder()
                .withNewMetadata().withName("TestService").endMetadata()
                .withNewSpec().withSelector(selector).endSpec().build();

        Endpoints endPoints = new EndpointsBuilder()
                .withNewMetadata().withName("TestService").endMetadata()
                .addNewSubset()
                .addNewAddress().withIp("ip1")
                .withNewTargetRef().withUid("uid1").withName("TestServer").endTargetRef().endAddress()
                .addNewPort("Test", "Test", 12345, "TCP").endSubset()
                .build();

        mockClient.pods().create(pod);
        mockClient.services().create(service);
        mockClient.endpoints().create(endPoints);
    }

    @AfterEach
    public void destroy() {
        mockServer.after();
    }

    @Test
    public void testEndpointsUpdate() throws Exception {

        KubernetesServiceDiscovery serviceDiscovery = new KubernetesServiceDiscovery();
        serviceDiscovery.initialize(serverUrl);

        serviceDiscovery.setCurrentHostname("TestServer");
        serviceDiscovery.setKubernetesClient(mockClient);

        ServiceInstance serviceInstance = new DefaultServiceInstance("TestService", "Test", 12345, ScopeModelUtil.getApplicationModel(serviceDiscovery.getUrl().getScopeModel()));
        serviceDiscovery.register(serviceInstance);

        HashSet<String> serviceList = new HashSet<>(4);
        serviceList.add("TestService");
        Mockito.when(mockListener.getServiceNames()).thenReturn(serviceList);
        Mockito.doNothing().when(mockListener).onEvent(Mockito.any());

        serviceDiscovery.addServiceInstancesChangedListener(mockListener);
        mockClient.endpoints().withName("TestService")
                .edit(endpoints ->
                        new EndpointsBuilder(endpoints)
                                .editFirstSubset()
                                .addNewAddress()
                                .withIp("ip2")
                                .withNewTargetRef().withUid("uid2").withName("TestServer").endTargetRef()
                                .endAddress().endSubset()
                                .build());

        Thread.sleep(5000);
        ArgumentCaptor<ServiceInstancesChangedEvent> eventArgumentCaptor =
                ArgumentCaptor.forClass(ServiceInstancesChangedEvent.class);
        Mockito.verify(mockListener, Mockito.times(2)).onEvent(eventArgumentCaptor.capture());
        Assertions.assertEquals(2, eventArgumentCaptor.getValue().getServiceInstances().size());

        serviceDiscovery.unregister(serviceInstance);

        serviceDiscovery.destroy();
    }

    @Test
    public void testPodsUpdate() throws Exception {

        KubernetesServiceDiscovery serviceDiscovery = new KubernetesServiceDiscovery();
        serviceDiscovery.initialize(serverUrl);

        serviceDiscovery.setCurrentHostname("TestServer");
        serviceDiscovery.setKubernetesClient(mockClient);

        ServiceInstance serviceInstance = new DefaultServiceInstance("TestService", "Test", 12345, ScopeModelUtil.getApplicationModel(serviceDiscovery.getUrl().getScopeModel()));
        serviceDiscovery.register(serviceInstance);

        HashSet<String> serviceList = new HashSet<>(4);
        serviceList.add("TestService");
        Mockito.when(mockListener.getServiceNames()).thenReturn(serviceList);
        Mockito.doNothing().when(mockListener).onEvent(Mockito.any());

        serviceDiscovery.addServiceInstancesChangedListener(mockListener);

        serviceInstance = new DefaultServiceInstance("TestService", "Test12345", 12345, ScopeModelUtil.getApplicationModel(serviceDiscovery.getUrl().getScopeModel()));
        serviceDiscovery.update(serviceInstance);

        Thread.sleep(5000);
        ArgumentCaptor<ServiceInstancesChangedEvent> eventArgumentCaptor =
                ArgumentCaptor.forClass(ServiceInstancesChangedEvent.class);
        Mockito.verify(mockListener, Mockito.times(1)).onEvent(eventArgumentCaptor.capture());
        Assertions.assertEquals(1, eventArgumentCaptor.getValue().getServiceInstances().size());

        serviceDiscovery.unregister(serviceInstance);

        serviceDiscovery.destroy();
    }

    @Test
    public void testGetInstance() throws Exception {
        KubernetesServiceDiscovery serviceDiscovery = new KubernetesServiceDiscovery();
        serviceDiscovery.initialize(serverUrl);

        serviceDiscovery.setCurrentHostname("TestServer");
        serviceDiscovery.setKubernetesClient(mockClient);

        ServiceInstance serviceInstance = new DefaultServiceInstance("TestService", "Test", 12345, ScopeModelUtil.getApplicationModel(serviceDiscovery.getUrl().getScopeModel()));
        serviceDiscovery.register(serviceInstance);

        serviceDiscovery.update(serviceInstance);

        Assertions.assertEquals(1, serviceDiscovery.getServices().size());
        Assertions.assertEquals(1, serviceDiscovery.getInstances("TestService").size());

        Assertions.assertEquals(serviceInstance, serviceDiscovery.getLocalInstance());

        serviceDiscovery.unregister(serviceInstance);

        serviceDiscovery.destroy();
    }
}
