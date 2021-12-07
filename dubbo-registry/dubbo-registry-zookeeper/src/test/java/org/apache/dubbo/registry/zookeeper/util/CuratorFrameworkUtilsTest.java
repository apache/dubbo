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
package org.apache.dubbo.registry.zookeeper.util;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.metadata.report.MetadataReport;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.zookeeper.ZookeeperInstance;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.EXPORTED_SERVICES_REVISION_PROPERTY_NAME;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.METADATA_STORAGE_TYPE_PROPERTY_NAME;
import static org.apache.dubbo.registry.zookeeper.util.CuratorFrameworkParams.ROOT_PATH;

/**
 * {@link CuratorFrameworkUtils} Test
 */
class CuratorFrameworkUtilsTest {
    private static URL registryUrl;
    private static String zookeeperConnectionAddress1;
    private static MetadataReport metadataReport;

    @BeforeAll
    public static void init() throws Exception {
        zookeeperConnectionAddress1 = System.getProperty("zookeeper.connection.address.1");

        registryUrl = URL.valueOf(zookeeperConnectionAddress1);
        registryUrl.setScopeModel(ApplicationModel.defaultModel());

        metadataReport = Mockito.mock(MetadataReport.class);
    }

    @Test
    void testBuildCuratorFramework() throws Exception {
        CuratorFramework curatorFramework = CuratorFrameworkUtils.buildCuratorFramework(registryUrl);
        Assertions.assertNotNull(curatorFramework);
        Assertions.assertTrue(curatorFramework.getZookeeperClient().isConnected());
        curatorFramework.getZookeeperClient().close();
    }

    @Test
    void testBuildServiceDiscovery() throws Exception {
        CuratorFramework curatorFramework = CuratorFrameworkUtils.buildCuratorFramework(registryUrl);
        ServiceDiscovery<ZookeeperInstance> discovery = CuratorFrameworkUtils.buildServiceDiscovery(curatorFramework, ROOT_PATH.getParameterValue(registryUrl));
        Assertions.assertNotNull(discovery);
        discovery.close();
        curatorFramework.getZookeeperClient().close();
    }

    @Test
    void testBuild() {
        ServiceInstance dubboServiceInstance = new DefaultServiceInstance("A", "127.0.0.1", 8888, ApplicationModel.defaultModel());
        Map<String, String> metadata = dubboServiceInstance.getMetadata();
        metadata.put(METADATA_STORAGE_TYPE_PROPERTY_NAME, "remote");
        metadata.put(EXPORTED_SERVICES_REVISION_PROPERTY_NAME, "111");
        metadata.put("site", "dubbo");

        // convert {org.apache.dubbo.registry.client.ServiceInstance} to {org.apache.curator.x.discovery.ServiceInstance<ZookeeperInstance>}
        org.apache.curator.x.discovery.ServiceInstance<ZookeeperInstance> curatorServiceInstance = CuratorFrameworkUtils.build(dubboServiceInstance);
        Assertions.assertEquals(curatorServiceInstance.getId(), dubboServiceInstance.getAddress());
        Assertions.assertEquals(curatorServiceInstance.getName(), dubboServiceInstance.getServiceName());
        Assertions.assertEquals(curatorServiceInstance.getAddress(), dubboServiceInstance.getHost());
        Assertions.assertEquals(curatorServiceInstance.getPort(), dubboServiceInstance.getPort());

        ZookeeperInstance payload = curatorServiceInstance.getPayload();
        Assertions.assertNotNull(payload);
        Assertions.assertEquals(payload.getMetadata(), metadata);
        Assertions.assertEquals(payload.getName(), dubboServiceInstance.getServiceName());

        // convert {org.apache.curator.x.discovery.ServiceInstance<ZookeeperInstance>} to {org.apache.dubbo.registry.client.ServiceInstance}
        ServiceInstance serviceInstance = CuratorFrameworkUtils.build(registryUrl, curatorServiceInstance);
        Assertions.assertEquals(serviceInstance, dubboServiceInstance);

        // convert {Collection<org.apache.curator.x.discovery.ServiceInstance<ZookeeperInstance>>} to {List<org.apache.dubbo.registry.client.ServiceInstance>}
        List<ServiceInstance> serviceInstances = CuratorFrameworkUtils.build(registryUrl, Arrays.asList(curatorServiceInstance));
        Assertions.assertNotNull(serviceInstances);
        Assertions.assertEquals(serviceInstances.get(0), dubboServiceInstance);
    }

}
