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
package org.apache.dubbo.registry.etcd;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstance;

import com.google.gson.Gson;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.valueOf;

/**
 * 2019-08-30
 * <p>
 * There is no embedded server. so it works depend on etcd local server.
 */
@Disabled
public class EtcdServiceDiscoveryTest {

    static EtcdServiceDiscovery etcdServiceDiscovery;

    @BeforeAll
    public static void setUp() throws Exception {
        URL url = URL.valueOf("etcd3://127.0.0.1:2379/org.apache.dubbo.registry.RegistryService");
        etcdServiceDiscovery = new EtcdServiceDiscovery();
        Assertions.assertNull(etcdServiceDiscovery.etcdClient);
        etcdServiceDiscovery.initialize(url);
    }

    @AfterAll
    public static void destroy() throws Exception {
//        etcdServiceDiscovery.destroy();
    }


    @Test
    public void testLifecycle() throws Exception {
        URL url = URL.valueOf("etcd3://127.0.0.1:2233/org.apache.dubbo.registry.RegistryService");
        EtcdServiceDiscovery etcdServiceDiscoveryTmp = new EtcdServiceDiscovery();
        Assertions.assertNull(etcdServiceDiscoveryTmp.etcdClient);
        etcdServiceDiscoveryTmp.initialize(url);
        Assertions.assertNotNull(etcdServiceDiscoveryTmp.etcdClient);
        Assertions.assertTrue(etcdServiceDiscoveryTmp.etcdClient.isConnected());
        etcdServiceDiscoveryTmp.destroy();
        Assertions.assertFalse(etcdServiceDiscoveryTmp.etcdClient.isConnected());
    }

    @Test
    public void testRegistry() throws Exception {
        ServiceInstance serviceInstance = new DefaultServiceInstance(valueOf(System.nanoTime()), "EtcdTestService", "127.0.0.1", 8080);
        Assertions.assertNull(etcdServiceDiscovery.etcdClient.getKVValue(etcdServiceDiscovery.toPath(serviceInstance)));
        etcdServiceDiscovery.register(serviceInstance);
        Assertions.assertNotNull(etcdServiceDiscovery.etcdClient.getKVValue(etcdServiceDiscovery.toPath(serviceInstance)));
    }

    @Test
    public void testUnRegistry() throws Exception {
        ServiceInstance serviceInstance = new DefaultServiceInstance(valueOf(System.nanoTime()), "EtcdTest2Service", "127.0.0.1", 8080);
        Assertions.assertNull(etcdServiceDiscovery.etcdClient.getKVValue(etcdServiceDiscovery.toPath(serviceInstance)));
        etcdServiceDiscovery.register(serviceInstance);
        Assertions.assertNotNull(etcdServiceDiscovery.etcdClient.getKVValue(etcdServiceDiscovery.toPath(serviceInstance)));
        etcdServiceDiscovery.unregister(serviceInstance);
        Assertions.assertNull(etcdServiceDiscovery.etcdClient.getKVValue(etcdServiceDiscovery.toPath(serviceInstance)));
    }

    @Test
    public void testUpdate() throws Exception {
        DefaultServiceInstance serviceInstance = new DefaultServiceInstance(valueOf(System.nanoTime()), "EtcdTest34Service", "127.0.0.1", 8080);
        Assertions.assertNull(etcdServiceDiscovery.etcdClient.getKVValue(etcdServiceDiscovery.toPath(serviceInstance)));
        etcdServiceDiscovery.register(serviceInstance);
        Assertions.assertNotNull(etcdServiceDiscovery.etcdClient.getKVValue(etcdServiceDiscovery.toPath(serviceInstance)));
        Assertions.assertEquals(etcdServiceDiscovery.etcdClient.getKVValue(etcdServiceDiscovery.toPath(serviceInstance)),
                new Gson().toJson(serviceInstance));
        serviceInstance.setPort(9999);
        etcdServiceDiscovery.update(serviceInstance);
        Assertions.assertNotNull(etcdServiceDiscovery.etcdClient.getKVValue(etcdServiceDiscovery.toPath(serviceInstance)));
        Assertions.assertEquals(etcdServiceDiscovery.etcdClient.getKVValue(etcdServiceDiscovery.toPath(serviceInstance)),
                new Gson().toJson(serviceInstance));
    }

    @Test
    public void testGetInstances() throws Exception {
        String serviceName = "EtcdTest77Service";
        Assertions.assertTrue(etcdServiceDiscovery.getInstances(serviceName).isEmpty());
        etcdServiceDiscovery.register(new DefaultServiceInstance(valueOf(System.nanoTime()), serviceName, "127.0.0.1", 8080));
        etcdServiceDiscovery.register(new DefaultServiceInstance(valueOf(System.nanoTime()), serviceName, "127.0.0.1", 9809));
        Assertions.assertFalse(etcdServiceDiscovery.getInstances(serviceName).isEmpty());
        List<String> r = convertToIpPort(etcdServiceDiscovery.getInstances(serviceName));
        Assertions.assertTrue(r.contains("127.0.0.1:8080"));
        Assertions.assertTrue(r.contains("127.0.0.1:9809"));
    }

    private List<String> convertToIpPort(List<ServiceInstance> serviceInstances) {
        List<String> result = new ArrayList<>();
        for (ServiceInstance serviceInstance : serviceInstances) {
            result.add(serviceInstance.getHost() + ":" + serviceInstance.getPort());
        }
        return result;
    }

}
