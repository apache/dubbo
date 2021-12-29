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
//package org.apache.dubbo.registry.client;
//
//import org.apache.dubbo.common.URL;
//import org.apache.dubbo.common.utils.Page;
//import org.apache.dubbo.rpc.model.ApplicationModel;
//import org.apache.dubbo.rpc.model.ScopeModelUtil;
//
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//
//import java.util.HashSet;
//import java.util.LinkedList;
//import java.util.List;
//
//import static java.util.Arrays.asList;
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertFalse;
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//
///**
// * {@link ServiceDiscovery} Test case
// *
// * @since 2.7.5
// */
//public class ServiceDiscoveryTest {
//
//    private ServiceDiscovery serviceDiscovery;
//
//    @BeforeEach
//    public void init() throws Exception {
//        if (serviceDiscovery == null) {
//            setServiceDiscovery(new InMemoryServiceDiscovery("sd-test-app"));
//        }
//        // test start()
//        URL registryUrl = URL.valueOf("");
//        registryUrl = registryUrl.setScopeModel(ApplicationModel.defaultModel());
//        serviceDiscovery.initialize(registryUrl);
//    }
//
//    @AfterEach
//    public void destroy() throws Exception {
//        // test stop()
//        serviceDiscovery.destroy();
//    }
//
//    @Test
//    public void testToString() {
//        assertEquals("InMemoryServiceDiscovery", serviceDiscovery.toString());
//    }
//
//    @Test
//    public void testRegisterAndUpdateAndUnregister() {
//
//        // register
//        serviceDiscovery.register();
//
//        ServiceInstance serviceInstance = serviceDiscovery.getLocalInstance();
//        assertNotNull(serviceInstance);
//
//        // update
//        serviceDiscovery.update();
//
//        // unregister
//        serviceDiscovery.unregister();
//
//    }
//
//    @Test
//    public void testGetInstances() {
//
//        List<ServiceInstance> instances = asList(
//                new DefaultServiceInstance("A", "127.0.0.1", 8080, ScopeModelUtil.getApplicationModel(serviceDiscovery.getUrl().getScopeModel())),
//                new DefaultServiceInstance("A", "127.0.0.1", 8081, ScopeModelUtil.getApplicationModel(serviceDiscovery.getUrl().getScopeModel())),
//                new DefaultServiceInstance("A", "127.0.0.1", 8082, ScopeModelUtil.getApplicationModel(serviceDiscovery.getUrl().getScopeModel()))
//        );
//
//        instances.forEach(serviceDiscovery::register);
//
//        // Duplicated
//        serviceDiscovery.register(new DefaultServiceInstance("A", "127.0.0.1", 8080, ScopeModelUtil.getApplicationModel(serviceDiscovery.getUrl().getScopeModel())));
//        // Duplicated
//        serviceDiscovery.register(new DefaultServiceInstance("A", "127.0.0.1", 8081, ScopeModelUtil.getApplicationModel(serviceDiscovery.getUrl().getScopeModel())));
//
//        // offset starts 0
//        int offset = 0;
//        // pageSize > total elements
//        int pageSize = 5;
//
//        Page<ServiceInstance> page = serviceDiscovery.getInstances("A", offset, pageSize);
//        assertEquals(0, page.getOffset());
//        assertEquals(5, page.getPageSize());
//        assertEquals(3, page.getTotalSize());
//        assertEquals(3, page.getData().size());
//        assertTrue(page.hasData());
//
//        for (ServiceInstance instance : page.getData()) {
//            assertTrue(instances.contains(instance));
//        }
//
//        // pageSize < total elements
//        pageSize = 2;
//
//        page = serviceDiscovery.getInstances("A", offset, pageSize);
//        assertEquals(0, page.getOffset());
//        assertEquals(2, page.getPageSize());
//        assertEquals(3, page.getTotalSize());
//        assertEquals(2, page.getData().size());
//        assertTrue(page.hasData());
//
//        for (ServiceInstance instance : page.getData()) {
//            assertTrue(instances.contains(instance));
//        }
//
//        offset = 1;
//        page = serviceDiscovery.getInstances("A", offset, pageSize);
//        assertEquals(1, page.getOffset());
//        assertEquals(2, page.getPageSize());
//        assertEquals(3, page.getTotalSize());
//        assertEquals(2, page.getData().size());
//        assertTrue(page.hasData());
//
//        for (ServiceInstance instance : page.getData()) {
//            assertTrue(instances.contains(instance));
//        }
//
//        offset = 2;
//        page = serviceDiscovery.getInstances("A", offset, pageSize);
//        assertEquals(2, page.getOffset());
//        assertEquals(2, page.getPageSize());
//        assertEquals(3, page.getTotalSize());
//        assertEquals(1, page.getData().size());
//        assertTrue(page.hasData());
//
//        offset = 3;
//        page = serviceDiscovery.getInstances("A", offset, pageSize);
//        assertEquals(3, page.getOffset());
//        assertEquals(2, page.getPageSize());
//        assertEquals(3, page.getTotalSize());
//        assertEquals(0, page.getData().size());
//        assertFalse(page.hasData());
//
//        offset = 5;
//        page = serviceDiscovery.getInstances("A", offset, pageSize);
//        assertEquals(5, page.getOffset());
//        assertEquals(2, page.getPageSize());
//        assertEquals(3, page.getTotalSize());
//        assertEquals(0, page.getData().size());
//        assertFalse(page.hasData());
//    }
//
//    @Test
//    public void testGetInstancesWithHealthy() {
//
//        List<ServiceInstance> instances = new LinkedList<>(asList(
//                new DefaultServiceInstance("A", "127.0.0.1", 8080, ScopeModelUtil.getApplicationModel(serviceDiscovery.getUrl().getScopeModel())),
//                new DefaultServiceInstance("A", "127.0.0.1", 8081, ScopeModelUtil.getApplicationModel(serviceDiscovery.getUrl().getScopeModel()))
//        ));
//
//
//        DefaultServiceInstance serviceInstance = new DefaultServiceInstance("A", "127.0.0.1", 8082, ScopeModelUtil.getApplicationModel(serviceDiscovery.getUrl().getScopeModel()));
//        serviceInstance.setHealthy(false);
//        instances.add(serviceInstance);
//
//        instances.forEach(serviceDiscovery::register);
//
//        // offset starts 0
//        int offset = 0;
//        // requestSize > total elements
//        int requestSize = 5;
//
//        Page<ServiceInstance> page = serviceDiscovery.getInstances("A", offset, requestSize, true);
//        assertEquals(0, page.getOffset());
//        assertEquals(5, page.getPageSize());
//        assertEquals(3, page.getTotalSize());
//        assertEquals(2, page.getData().size());
//        assertTrue(page.hasData());
//
//        for (ServiceInstance instance : page.getData()) {
//            assertTrue(instances.contains(instance));
//        }
//
//        // requestSize < total elements
//        requestSize = 2;
//
//        offset = 1;
//        page = serviceDiscovery.getInstances("A", offset, requestSize, true);
//        assertEquals(1, page.getOffset());
//        assertEquals(2, page.getPageSize());
//        assertEquals(3, page.getTotalSize());
//        assertEquals(1, page.getData().size());
//        assertTrue(page.hasData());
//
//        for (ServiceInstance instance : page.getData()) {
//            assertTrue(instances.contains(instance));
//        }
//
//        offset = 2;
//        page = serviceDiscovery.getInstances("A", offset, requestSize, true);
//        assertEquals(2, page.getOffset());
//        assertEquals(2, page.getPageSize());
//        assertEquals(3, page.getTotalSize());
//        assertEquals(0, page.getData().size());
//        assertFalse(page.hasData());
//
//        offset = 3;
//        page = serviceDiscovery.getInstances("A", offset, requestSize, true);
//        assertEquals(3, page.getOffset());
//        assertEquals(2, page.getPageSize());
//        assertEquals(3, page.getTotalSize());
//        assertEquals(0, page.getData().size());
//        assertFalse(page.hasData());
//
//        offset = 5;
//        page = serviceDiscovery.getInstances("A", offset, requestSize, true);
//        assertEquals(5, page.getOffset());
//        assertEquals(2, page.getPageSize());
//        assertEquals(3, page.getTotalSize());
//        assertEquals(0, page.getData().size());
//        assertFalse(page.hasData());
//    }
//
//    public void setServiceDiscovery(ServiceDiscovery serviceDiscovery) {
//        this.serviceDiscovery = serviceDiscovery;
//    }
//
//    public ServiceDiscovery getServiceDiscovery() {
//        return serviceDiscovery;
//    }
//}
