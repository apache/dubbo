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
package org.apache.dubbo.registry.dns;

import com.alibaba.fastjson.JSONObject;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ArgumentConfig;
import org.apache.dubbo.config.MethodConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.metadata.InstanceMetadataChangedListener;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.registry.Constants;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;
import org.apache.dubbo.registry.dns.util.DNSClientConst;
import org.apache.dubbo.registry.dns.util.DNSResolver;
import org.apache.dubbo.registry.dns.util.ResolveResult;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ScopeModelUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_PROTOCOL;
import static org.apache.dubbo.metadata.MetadataConstants.METADATA_PROXY_TIMEOUT_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DNSServiceDiscoveryTest {

    @BeforeEach
    public void setup() {
        DubboBootstrap.reset();
        ApplicationConfig applicationConfig = new ApplicationConfig("Test");
        ApplicationModel.defaultModel().getApplicationConfigManager().setApplication(applicationConfig);
    }

    @AfterEach
    public void destroy() {
        DubboBootstrap.reset();
    }

    @Test
    public void testProvider() throws Exception {
        ApplicationModel applicationModel = ApplicationModel.defaultModel();
        DNSServiceDiscovery dnsServiceDiscovery = new DNSServiceDiscovery();
        dnsServiceDiscovery.setApplicationModel(applicationModel);

        URL registryURL = URL.valueOf("dns://");
        registryURL.setScopeModel(ApplicationModel.defaultModel());
        dnsServiceDiscovery.initialize(registryURL);

        assertEquals(registryURL, dnsServiceDiscovery.getUrl());

        ServiceInstance serviceInstance = new DefaultServiceInstance("TestService", "localhost", 12345, ScopeModelUtil.getApplicationModel(dnsServiceDiscovery.getUrl().getScopeModel()));
        serviceInstance.getMetadata().put("a", "b");

        dnsServiceDiscovery.register(serviceInstance);

        WritableMetadataService metadataService = WritableMetadataService.getDefaultExtension(applicationModel);
        InstanceMetadataChangedListener changeListener = Mockito.mock(InstanceMetadataChangedListener.class);

        String metadataString = metadataService
                .getAndListenInstanceMetadata("test", changeListener);

        assertEquals(JSONObject.toJSONString(serviceInstance.getMetadata()), metadataString);
        assertEquals(serviceInstance, dnsServiceDiscovery.getLocalInstance());

        dnsServiceDiscovery.unregister(serviceInstance);

        Mockito.verify(changeListener, Mockito.times(1)).onEvent(Mockito.any());

        metadataService.getInstanceMetadataChangedListenerMap().clear();
        metadataService.exportInstanceMetadata(null);

        dnsServiceDiscovery.destroy();

    }

    @Test
    public void testConsumer() throws Exception {
        ApplicationModel applicationModel = ApplicationModel.defaultModel();
        DNSServiceDiscovery dnsServiceDiscovery = new DNSServiceDiscovery();
        dnsServiceDiscovery.setApplicationModel(applicationModel);

        URL registryURL = URL.valueOf("dns://")
                .addParameter(DNSClientConst.DNS_POLLING_CYCLE, 100)
                .addParameter(Constants.ECHO_POLLING_CYCLE_KEY, 100);
        registryURL.setScopeModel(ApplicationModel.defaultModel());
        applicationModel.getModelEnvironment().getAppExternalConfigMap()
                .put(METADATA_PROXY_TIMEOUT_KEY, String.valueOf(500));
        dnsServiceDiscovery.initialize(registryURL);

        WritableMetadataService metadataService = WritableMetadataService.getDefaultExtension(applicationModel);
        ServiceInstance serviceInstance = new DefaultServiceInstance("TestService", "localhost", 12345, ScopeModelUtil.getApplicationModel(dnsServiceDiscovery.getUrl().getScopeModel()));
        serviceInstance.getMetadata().put("a", "b");

        dnsServiceDiscovery.register(serviceInstance);

        int port = NetUtils.getAvailablePort();
        applicationModel.getCurrentConfig().setMetadataServicePort(port);

        WritableMetadataService spiedMetadataService = Mockito.spy(metadataService);

        ServiceConfig<MetadataService> serviceConfig = exportMockMetadataService(spiedMetadataService, port);

        DNSResolver dnsResolver = Mockito.mock(DNSResolver.class);
        ResolveResult resolveResult = new ResolveResult();
        resolveResult.getHostnameList().add("127.0.0.1");
        Mockito.when(dnsResolver.resolve("Test.Service.")).thenReturn(resolveResult);
        dnsServiceDiscovery.setDnsResolver(dnsResolver);

        List<ServiceInstance> serviceInstances = dnsServiceDiscovery.getInstances("Test.Service.");
        assertEquals("b", serviceInstances.get(0).getMetadata("a"));

        Set<String> serviceNames = new HashSet<>();
        serviceNames.add("Test.Service.");
        ServiceInstancesChangedListener changedListener = Mockito.spy(new ServiceInstancesChangedListener(serviceNames, null));
        Mockito.doNothing().when(changedListener).onEvent(Mockito.any());

        serviceInstance.getMetadata().put("a", "c");
        dnsServiceDiscovery.update(serviceInstance);

        serviceInstances = dnsServiceDiscovery.getInstances("Test.Service.");
        assertEquals("c", serviceInstances.get(0).getMetadata("a"));

        ArgumentCaptor<ServiceInstancesChangedEvent> argument = ArgumentCaptor.forClass(ServiceInstancesChangedEvent.class);
        dnsServiceDiscovery.addServiceInstancesChangedListener(changedListener);
        Thread.sleep(1000);
        Mockito.verify(changedListener, Mockito.timeout(1000)).onEvent(argument.capture());
        assertEquals("c", argument.getValue().getServiceInstances().get(0).getMetadata("a"));

        Mockito.when(dnsResolver.resolve("Test.Service.")).thenReturn(new ResolveResult());

        Thread.sleep(1000);
        assertTrue(dnsServiceDiscovery.getCachedServiceInstances().get("Test.Service.").isEmpty());

        metadataService.exportInstanceMetadata(null);
        metadataService.getInstanceMetadataChangedListenerMap().clear();
        serviceConfig.unexport();

        dnsServiceDiscovery.destroy();
        applicationModel.getModelEnvironment().getAppExternalConfigMap()
                .remove(METADATA_PROXY_TIMEOUT_KEY);
    }

    private ServiceConfig<MetadataService> exportMockMetadataService(MetadataService metadataService, int port) {
        ServiceConfig<MetadataService> serviceConfig = new ServiceConfig<>();
        serviceConfig.setProtocol(new ProtocolConfig(DUBBO_PROTOCOL, port));
        serviceConfig.setRegistry(new RegistryConfig("239.255.255.255", "multicast"));
        serviceConfig.setInterface(MetadataService.class);
        serviceConfig.setRef(metadataService);
        serviceConfig.setGroup("Test.Service.");
        serviceConfig.setVersion(MetadataService.VERSION);
        MethodConfig methodConfig = new MethodConfig();
        methodConfig.setName("getAndListenInstanceMetadata");

        ArgumentConfig argumentConfig = new ArgumentConfig();
        argumentConfig.setIndex(1);
        argumentConfig.setCallback(true);

        methodConfig.setArguments(Collections.singletonList(argumentConfig));
        serviceConfig.setMethods(Collections.singletonList(methodConfig));

        serviceConfig.export();

        return serviceConfig;
    }
}
