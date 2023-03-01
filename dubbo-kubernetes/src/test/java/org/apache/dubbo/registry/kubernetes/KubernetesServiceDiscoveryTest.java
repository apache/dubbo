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

import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.EndpointsBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;
import org.apache.dubbo.registry.kubernetes.util.KubernetesClientConst;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ScopeModelUtil;
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

import static org.apache.dubbo.registry.kubernetes.util.KubernetesClientConst.NAMESPACE;
import static org.awaitility.Awaitility.await;

@ExtendWith({MockitoExtension.class})
class KubernetesServiceDiscoveryTest {
    private static final String SERVICE_NAME = "TestService";

    private static final String POD_NAME = "TestServer";

    public KubernetesServer mockServer = new KubernetesServer(false, true);

    private NamespacedKubernetesClient mockClient;

    private ServiceInstancesChangedListener mockListener = Mockito.mock(ServiceInstancesChangedListener.class);

    private URL serverUrl;

    private Map<String, String> selector;

    private KubernetesServiceDiscovery serviceDiscovery;


    @BeforeEach
    public void setUp() {
        mockServer.before();
        mockClient = mockServer.getClient().inNamespace("dubbo-demo");

        ApplicationModel applicationModel = ApplicationModel.defaultModel();
        applicationModel.getApplicationConfigManager().setApplication(new ApplicationConfig());

        serverUrl = URL.valueOf(mockClient.getConfiguration().getMasterUrl())
                .setProtocol("kubernetes")
                .addParameter(NAMESPACE, "dubbo-demo")
                .addParameter(KubernetesClientConst.USE_HTTPS, "false")
                .addParameter(KubernetesClientConst.HTTP2_DISABLE, "true");
        serverUrl.setScopeModel(applicationModel);

        this.serviceDiscovery = new KubernetesServiceDiscovery(applicationModel, serverUrl);

        System.setProperty(Config.KUBERNETES_AUTH_TRYKUBECONFIG_SYSTEM_PROPERTY, "false");
        System.setProperty(Config.KUBERNETES_AUTH_TRYSERVICEACCOUNT_SYSTEM_PROPERTY, "false");

        selector = new HashMap<>(4);
        selector.put("l", "v");
        Pod pod = new PodBuilder()
                .withNewMetadata().withName(POD_NAME).withLabels(selector).endMetadata()
                .build();

        Service service = new ServiceBuilder()
                .withNewMetadata().withName(SERVICE_NAME).endMetadata()
                .withNewSpec().withSelector(selector).endSpec().build();

        Endpoints endPoints = new EndpointsBuilder()
                .withNewMetadata().withName(SERVICE_NAME).endMetadata()
                .addNewSubset()
                .addNewAddress().withIp("ip1")
                .withNewTargetRef().withUid("uid1").withName(POD_NAME).endTargetRef().endAddress()
                .addNewPort("Test", "Test", 12345, "TCP").endSubset()
                .build();

        mockClient.pods().resource(pod).create();
        mockClient.services().resource(service).create();
        mockClient.endpoints().resource(endPoints).create();
    }

    @AfterEach
    public void destroy() throws Exception {
        serviceDiscovery.destroy();
        mockClient.close();
        mockServer.after();
    }

    @Test
    void testEndpointsUpdate() {
        serviceDiscovery.setCurrentHostname(POD_NAME);
        serviceDiscovery.setKubernetesClient(mockClient);

        ServiceInstance serviceInstance = new DefaultServiceInstance(SERVICE_NAME, "Test", 12345, ScopeModelUtil.getApplicationModel(serviceDiscovery.getUrl().getScopeModel()));

        serviceDiscovery.doRegister(serviceInstance);

        HashSet<String> serviceList = new HashSet<>(4);
        serviceList.add(SERVICE_NAME);
        Mockito.when(mockListener.getServiceNames()).thenReturn(serviceList);
        Mockito.doNothing().when(mockListener).onEvent(Mockito.any());

        serviceDiscovery.addServiceInstancesChangedListener(mockListener);
        mockClient.endpoints().withName(SERVICE_NAME)
                .edit(endpoints ->
                        new EndpointsBuilder(endpoints)
                                .editFirstSubset()
                                .addNewAddress()
                                .withIp("ip2")
                                .withNewTargetRef().withUid("uid2").withName(POD_NAME).endTargetRef()
                                .endAddress().endSubset()
                                .build());

        await().until(() -> {
            ArgumentCaptor<ServiceInstancesChangedEvent> captor = ArgumentCaptor.forClass(ServiceInstancesChangedEvent.class);
            Mockito.verify(mockListener, Mockito.atLeast(0)).onEvent(captor.capture());
            return captor.getValue().getServiceInstances().size() == 2;
        });
        ArgumentCaptor<ServiceInstancesChangedEvent> eventArgumentCaptor =
                ArgumentCaptor.forClass(ServiceInstancesChangedEvent.class);
        Mockito.verify(mockListener, Mockito.times(2)).onEvent(eventArgumentCaptor.capture());
        Assertions.assertEquals(2, eventArgumentCaptor.getValue().getServiceInstances().size());

        serviceDiscovery.doUnregister(serviceInstance);
    }

    @Test
    void testPodsUpdate() throws Exception {
        serviceDiscovery.setCurrentHostname(POD_NAME);
        serviceDiscovery.setKubernetesClient(mockClient);

        ServiceInstance serviceInstance = new DefaultServiceInstance(SERVICE_NAME, "Test", 12345, ScopeModelUtil.getApplicationModel(serviceDiscovery.getUrl().getScopeModel()));

        serviceDiscovery.doRegister(serviceInstance);

        HashSet<String> serviceList = new HashSet<>(4);
        serviceList.add(SERVICE_NAME);
        Mockito.when(mockListener.getServiceNames()).thenReturn(serviceList);
        Mockito.doNothing().when(mockListener).onEvent(Mockito.any());

        serviceDiscovery.addServiceInstancesChangedListener(mockListener);

        serviceInstance = new DefaultServiceInstance(SERVICE_NAME, "Test12345", 12345, ScopeModelUtil.getApplicationModel(serviceDiscovery.getUrl().getScopeModel()));
        serviceDiscovery.doUpdate(serviceInstance, serviceInstance);

        await().until(() -> {
            ArgumentCaptor<ServiceInstancesChangedEvent> captor = ArgumentCaptor.forClass(ServiceInstancesChangedEvent.class);
            Mockito.verify(mockListener, Mockito.atLeast(0)).onEvent(captor.capture());
            return captor.getValue().getServiceInstances().size() == 1;
        });
        ArgumentCaptor<ServiceInstancesChangedEvent> eventArgumentCaptor =
                ArgumentCaptor.forClass(ServiceInstancesChangedEvent.class);
        Mockito.verify(mockListener, Mockito.times(1)).onEvent(eventArgumentCaptor.capture());
        Assertions.assertEquals(1, eventArgumentCaptor.getValue().getServiceInstances().size());

        serviceDiscovery.doUnregister(serviceInstance);
    }

    @Test
    void testServiceUpdate() {
        serviceDiscovery.setCurrentHostname(POD_NAME);
        serviceDiscovery.setKubernetesClient(mockClient);

        ServiceInstance serviceInstance = new DefaultServiceInstance(SERVICE_NAME, "Test", 12345, ScopeModelUtil.getApplicationModel(serviceDiscovery.getUrl().getScopeModel()));

        serviceDiscovery.doRegister(serviceInstance);

        HashSet<String> serviceList = new HashSet<>(4);
        serviceList.add(SERVICE_NAME);
        Mockito.when(mockListener.getServiceNames()).thenReturn(serviceList);
        Mockito.doNothing().when(mockListener).onEvent(Mockito.any());

        serviceDiscovery.addServiceInstancesChangedListener(mockListener);

        selector.put("app", "test");
        mockClient.services().withName(SERVICE_NAME)
                .edit(service -> new ServiceBuilder(service)
                        .editSpec()
                        .addToSelector(selector)
                        .endSpec()
                        .build());

        await().until(() -> {
            ArgumentCaptor<ServiceInstancesChangedEvent> captor = ArgumentCaptor.forClass(ServiceInstancesChangedEvent.class);
            Mockito.verify(mockListener, Mockito.atLeast(0)).onEvent(captor.capture());
            return captor.getValue().getServiceInstances().size() == 1;
        });
        ArgumentCaptor<ServiceInstancesChangedEvent> eventArgumentCaptor =
                ArgumentCaptor.forClass(ServiceInstancesChangedEvent.class);
        Mockito.verify(mockListener, Mockito.times(1)).onEvent(eventArgumentCaptor.capture());
        Assertions.assertEquals(1, eventArgumentCaptor.getValue().getServiceInstances().size());

        serviceDiscovery.doUnregister(serviceInstance);
    }

    @Test
    void testGetInstance() {
        serviceDiscovery.setCurrentHostname(POD_NAME);
        serviceDiscovery.setKubernetesClient(mockClient);

        ServiceInstance serviceInstance = new DefaultServiceInstance(SERVICE_NAME, "Test", 12345, ScopeModelUtil.getApplicationModel(serviceDiscovery.getUrl().getScopeModel()));

        serviceDiscovery.doRegister(serviceInstance);

        serviceDiscovery.doUpdate(serviceInstance, serviceInstance);

        Assertions.assertEquals(1, serviceDiscovery.getServices().size());
        Assertions.assertEquals(1, serviceDiscovery.getInstances(SERVICE_NAME).size());

        serviceDiscovery.doUnregister(serviceInstance);
    }
}
