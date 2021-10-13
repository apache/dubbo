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
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ScopeModelUtil;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test for NacosServiceDiscovery
 */
public class NacosServiceDiscoveryTest {

    private static final String SERVICE_NAME = "NACOS_SERVICE";

    private static final String LOCALHOST = "127.0.0.1";

    private URL registryUrl;

    private NacosServiceDiscovery nacosServiceDiscovery;

    private DefaultServiceInstance createServiceInstance(String serviceName, String host, int port) {
        return new DefaultServiceInstance(serviceName, host, port, ScopeModelUtil.getApplicationModel(registryUrl.getScopeModel()));
    }

    @BeforeEach
    public void init() throws Exception {
        this.registryUrl = URL.valueOf("nacos://127.0.0.1:" + NetUtils.getAvailablePort());
        registryUrl.setScopeModel(ApplicationModel.defaultModel());
        this.nacosServiceDiscovery = new NacosServiceDiscovery();
        this.nacosServiceDiscovery.doInitialize(registryUrl);
    }

    @AfterEach
    public void destroy() throws Exception {
        nacosServiceDiscovery.destroy();
    }

    @Test
    public void testDoRegister() {
        DefaultServiceInstance serviceInstance = createServiceInstance(SERVICE_NAME, LOCALHOST, NetUtils.getAvailablePort());
        // register
        nacosServiceDiscovery.doRegister(serviceInstance);

        List<ServiceInstance> serviceInstances = nacosServiceDiscovery.getInstances(SERVICE_NAME);

        assertEquals(1, serviceInstances.size());
        assertEquals(serviceInstances.get(0), serviceInstance);
    }

    @Test
    public void testDoUnRegister() {
        DefaultServiceInstance serviceInstance = createServiceInstance(SERVICE_NAME, LOCALHOST, NetUtils.getAvailablePort());
        // register
        nacosServiceDiscovery.doRegister(serviceInstance);

        List<ServiceInstance> serviceInstances = nacosServiceDiscovery.getInstances(SERVICE_NAME);

        assertEquals(1, serviceInstances.size());
        assertEquals(serviceInstances.get(0), serviceInstance);

        // unRegister
        nacosServiceDiscovery.doUnregister(serviceInstance);
        assertEquals(0, serviceInstances.size());
    }
}
