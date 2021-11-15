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
package org.apache.dubbo.registry.nacos.util;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.metadata.report.MetadataReport;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.nacos.NacosNamingServiceWrapper;

import com.alibaba.nacos.api.naming.pojo.Instance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;

/**
 * Test for NacosNamingServiceUtils
 */
public class NacosNamingServiceUtilsTest {
    private static MetadataReport metadataReport = Mockito.mock(MetadataReport.class);

    @Test
    public void testToInstance() {
        ServiceInstance serviceInstance = mock(ServiceInstance.class);
        Instance instance = NacosNamingServiceUtils.toInstance(serviceInstance);
        Assertions.assertNotNull(instance);
    }

    @Test
    public void testToServiceInstance() {
        URL registryUrl = URL.valueOf("test://test:8080/test");
        Instance instance = new Instance();
        instance.setServiceName("serviceName");
        instance.setIp("1.1.1.1");
        instance.setPort(800);
        instance.setWeight(2);
        instance.setHealthy(Boolean.TRUE);
        instance.setEnabled(Boolean.TRUE);
        Map<String, String> map = new HashMap<String, String>();
        map.put("netType", "external");
        map.put("version", "2.0");
        instance.setMetadata(map);

        ServiceInstance serviceInstance = NacosNamingServiceUtils.toServiceInstance(registryUrl, instance);
        Assertions.assertNotNull(serviceInstance);
        Assertions.assertEquals(serviceInstance.isEnabled(), Boolean.TRUE);
        Assertions.assertEquals(serviceInstance.getServiceName(), "serviceName");
    }

    @Test
    public void testCreateNamingService() {
        URL url = URL.valueOf("test://test:8080/test?backup=backup");
        NacosNamingServiceWrapper namingService = NacosNamingServiceUtils.createNamingService(url);
        Assertions.assertNotNull(namingService);
    }
}
