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
package org.apache.dubbo.registry.client;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * {@link AbstractServiceDiscoveryFactory}
 */
class AbstractServiceDiscoveryFactoryTest {

    @Test
    void testGetServiceDiscoveryWithCache() {
        ApplicationModel.defaultModel().getApplicationConfigManager().setApplication(new ApplicationConfig("AbstractServiceDiscoveryFactoryTest"));
        URL url = URL.valueOf("mock://127.0.0.1:8888");
        ServiceDiscoveryFactory factory = ServiceDiscoveryFactory.getExtension(url);
        ServiceDiscovery serviceDiscovery1 = factory.getServiceDiscovery(url);
        ServiceDiscovery serviceDiscovery2 = factory.getServiceDiscovery(url);
        Assertions.assertEquals(serviceDiscovery1, serviceDiscovery2);

        url = url.setPath("test");
        ServiceDiscovery serviceDiscovery3 = factory.getServiceDiscovery(url);
        Assertions.assertNotEquals(serviceDiscovery2, serviceDiscovery3);

        AbstractServiceDiscoveryFactory abstractServiceDiscoveryFactory = (AbstractServiceDiscoveryFactory) factory;
        List<ServiceDiscovery> allServiceDiscoveries = abstractServiceDiscoveryFactory.getAllServiceDiscoveries();
        Assertions.assertEquals(2, allServiceDiscoveries.size());
        Assertions.assertTrue(allServiceDiscoveries.contains(serviceDiscovery1));
        Assertions.assertTrue(allServiceDiscoveries.contains(serviceDiscovery3));
    }
}
