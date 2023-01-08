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

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.registry.client.support.MockServiceDiscovery;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.apache.dubbo.common.constants.CommonConstants.METADATA_INFO_CACHE_EXPIRE_KEY;
import static org.awaitility.Awaitility.await;

class ServiceDiscoveryCacheTest {

    @Test
    void test() throws InterruptedException {
        ApplicationModel applicationModel = FrameworkModel.defaultModel().newApplication();
        applicationModel.getApplicationConfigManager().setApplication(new ApplicationConfig("Test"));

        URL registryUrl = URL.valueOf("mock://127.0.0.1:12345").addParameter(METADATA_INFO_CACHE_EXPIRE_KEY, 10);
        MockServiceDiscovery mockServiceDiscovery = Mockito.spy(new MockServiceDiscovery(applicationModel, registryUrl));

        mockServiceDiscovery.register(URL.valueOf("mock://127.0.0.1:12345")
            .setServiceInterface("org.apache.dubbo.registry.service.DemoService"));
        mockServiceDiscovery.register();

        ServiceInstance localInstance = mockServiceDiscovery.getLocalInstance();

        Assertions.assertEquals(localInstance.getServiceMetadata(), mockServiceDiscovery.getLocalMetadata(localInstance.getServiceMetadata().getRevision()));

        List<MetadataInfo> instances = new LinkedList<>();
        instances.add(localInstance.getServiceMetadata().clone());

        for (int i = 0; i < 15; i++) {
            Thread.sleep(1);
            mockServiceDiscovery.register(URL.valueOf("mock://127.0.0.1:12345")
                .setServiceInterface("org.apache.dubbo.registry.service.DemoService" + i));
            mockServiceDiscovery.update();
            instances.add(mockServiceDiscovery.getLocalInstance().getServiceMetadata().clone());
        }

        for (MetadataInfo instance : instances) {
            Assertions.assertEquals(instance, mockServiceDiscovery.getLocalMetadata(instance.getRevision()));
        }

        for (int i = 0; i < 5; i++) {
            Thread.sleep(1);
            mockServiceDiscovery.register(URL.valueOf("mock://127.0.0.1:12345")
                .setServiceInterface("org.apache.dubbo.registry.service.DemoService-new" + i));
            mockServiceDiscovery.update();
            instances.add(mockServiceDiscovery.getLocalInstance().getServiceMetadata().clone());
        }

        await().until(() -> Objects.isNull(mockServiceDiscovery.getLocalMetadata(instances.get(4).getRevision())));

        for (int i = 0; i < 5; i++) {
            Assertions.assertNull(mockServiceDiscovery.getLocalMetadata(instances.get(i).getRevision()));
        }

        applicationModel.destroy();
    }
}
