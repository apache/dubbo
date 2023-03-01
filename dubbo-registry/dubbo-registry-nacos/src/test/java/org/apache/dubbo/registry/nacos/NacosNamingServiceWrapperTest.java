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

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.pojo.Instance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

class NacosNamingServiceWrapperTest {
    @Test
    void testSubscribe() throws NacosException {
        NacosConnectionManager connectionManager = Mockito.mock(NacosConnectionManager.class);
        NamingService namingService = Mockito.mock(NamingService.class);
        Mockito.when(connectionManager.getNamingService()).thenReturn(namingService);

        NacosNamingServiceWrapper nacosNamingServiceWrapper = new NacosNamingServiceWrapper(connectionManager, 0, 0);

        EventListener eventListener = Mockito.mock(EventListener.class);
        nacosNamingServiceWrapper.subscribe("service_name", "test", eventListener);
        Mockito.verify(namingService, Mockito.times(1)).subscribe("service_name", "test", eventListener);

        nacosNamingServiceWrapper.subscribe("service_name", "test", eventListener);
        Mockito.verify(namingService, Mockito.times(2)).subscribe("service_name", "test", eventListener);

        nacosNamingServiceWrapper.unsubscribe("service_name", "test", eventListener);
        Mockito.verify(namingService, Mockito.times(1)).unsubscribe("service_name", "test", eventListener);

        nacosNamingServiceWrapper.unsubscribe("service_name", "test", eventListener);
        Mockito.verify(namingService, Mockito.times(1)).unsubscribe("service_name", "test", eventListener);

        nacosNamingServiceWrapper.unsubscribe("service_name", "mock", eventListener);
        Mockito.verify(namingService, Mockito.times(0)).unsubscribe("service_name", "mock", eventListener);
    }

    @Test
    void testSubscribeMultiManager() throws NacosException {
        NacosConnectionManager connectionManager = Mockito.mock(NacosConnectionManager.class);
        NamingService namingService1 = Mockito.mock(NamingService.class);
        NamingService namingService2 = Mockito.mock(NamingService.class);

        NacosNamingServiceWrapper nacosNamingServiceWrapper = new NacosNamingServiceWrapper(connectionManager, 0, 0);

        EventListener eventListener = Mockito.mock(EventListener.class);
        Mockito.when(connectionManager.getNamingService()).thenReturn(namingService1);
        nacosNamingServiceWrapper.subscribe("service_name", "test", eventListener);
        Mockito.verify(namingService1, Mockito.times(1)).subscribe("service_name", "test", eventListener);

        Mockito.when(connectionManager.getNamingService()).thenReturn(namingService2);
        nacosNamingServiceWrapper.subscribe("service_name", "test", eventListener);
        Mockito.verify(namingService1, Mockito.times(2)).subscribe("service_name", "test", eventListener);

        nacosNamingServiceWrapper.unsubscribe("service_name", "test", eventListener);
        Mockito.verify(namingService1, Mockito.times(1)).unsubscribe("service_name", "test", eventListener);

        nacosNamingServiceWrapper.unsubscribe("service_name", "test", eventListener);
        Mockito.verify(namingService1, Mockito.times(1)).unsubscribe("service_name", "test", eventListener);

        nacosNamingServiceWrapper.unsubscribe("service_name", "mock", eventListener);
        Mockito.verify(namingService1, Mockito.times(0)).unsubscribe("service_name", "mock", eventListener);
        Mockito.verify(namingService2, Mockito.times(0)).unsubscribe("service_name", "mock", eventListener);
    }

    @Test
    void testRegisterNacos2_0_x() throws NacosException {
        List<NamingService> namingServiceList = new LinkedList<>();
        NacosConnectionManager nacosConnectionManager = new NacosConnectionManager(URL.valueOf(""), false, 0, 0) {
            @Override
            protected NamingService createNamingService() {
                NamingService namingService = Mockito.mock(NamingService.class);
                namingServiceList.add(namingService);
                return namingService;
            }
        };

        Assertions.assertEquals(1, namingServiceList.size());

        NacosNamingServiceWrapper nacosNamingServiceWrapper = new NacosNamingServiceWrapper(nacosConnectionManager, false, 0, 0);

        Instance instance1 = new Instance();
        instance1.setIp("ip1");
        instance1.setPort(1);
        nacosNamingServiceWrapper.registerInstance("service_name", "test", instance1);
        Mockito.verify(namingServiceList.get(0), Mockito.times(1)).registerInstance("service_name", "test", instance1);

        Instance instance2 = new Instance();
        instance2.setIp("ip2");
        instance2.setPort(2);
        nacosNamingServiceWrapper.registerInstance("service_name", "test", instance2);
        Assertions.assertEquals(2, namingServiceList.size());
        Mockito.verify(namingServiceList.get(0), Mockito.times(1)).registerInstance("service_name", "test", instance1);
        Mockito.verify(namingServiceList.get(1), Mockito.times(1)).registerInstance("service_name", "test", instance2);

        Instance instance3 = new Instance();
        instance3.setIp("ip3");
        instance3.setPort(3);
        nacosNamingServiceWrapper.registerInstance("service_name", "test", instance3);
        Assertions.assertEquals(3, namingServiceList.size());
        Mockito.verify(namingServiceList.get(0), Mockito.times(1)).registerInstance("service_name", "test", instance1);
        Mockito.verify(namingServiceList.get(1), Mockito.times(1)).registerInstance("service_name", "test", instance2);
        Mockito.verify(namingServiceList.get(2), Mockito.times(1)).registerInstance("service_name", "test", instance3);

        nacosNamingServiceWrapper.deregisterInstance("service_name", "test", instance1);
        Mockito.verify(namingServiceList.get(0), Mockito.times(1)).deregisterInstance("service_name", "test", instance1);

        nacosNamingServiceWrapper.deregisterInstance("service_name", "test", instance2);
        Mockito.verify(namingServiceList.get(1), Mockito.times(1)).deregisterInstance("service_name", "test", instance2);

        nacosNamingServiceWrapper.deregisterInstance("service_name", "test", instance3);
        Mockito.verify(namingServiceList.get(2), Mockito.times(1)).deregisterInstance("service_name", "test", instance3);
    }

    @Test
    void testRegisterNacos2_1_xClient2_0_xServer() throws NacosException {
        List<NamingService> namingServiceList = new LinkedList<>();
        NacosConnectionManager nacosConnectionManager = new NacosConnectionManager(URL.valueOf(""), false, 0, 0) {
            @Override
            protected NamingService createNamingService() {
                NamingService namingService = Mockito.mock(NamingService.class);
                try {
                    Mockito.doThrow(new NacosException()).when(namingService).batchRegisterInstance(Mockito.anyString(), Mockito.anyString(), Mockito.any(List.class));
                } catch (NacosException e) {
                    throw new RuntimeException(e);
                }
                namingServiceList.add(namingService);
                return namingService;
            }
        };

        Assertions.assertEquals(1, namingServiceList.size());

        NacosNamingServiceWrapper nacosNamingServiceWrapper = new NacosNamingServiceWrapper(nacosConnectionManager, true, 0, 0);

        Instance instance1 = new Instance();
        instance1.setIp("ip1");
        instance1.setPort(1);
        nacosNamingServiceWrapper.registerInstance("service_name", "test", instance1);
        Mockito.verify(namingServiceList.get(0), Mockito.times(1)).registerInstance("service_name", "test", instance1);

        Instance instance2 = new Instance();
        instance2.setIp("ip2");
        instance2.setPort(2);
        nacosNamingServiceWrapper.registerInstance("service_name", "test", instance2);
        Assertions.assertEquals(2, namingServiceList.size());
        Mockito.verify(namingServiceList.get(0), Mockito.times(1)).registerInstance("service_name", "test", instance1);
        Mockito.verify(namingServiceList.get(1), Mockito.times(1)).registerInstance("service_name", "test", instance2);

        Instance instance3 = new Instance();
        instance3.setIp("ip3");
        instance3.setPort(3);
        nacosNamingServiceWrapper.registerInstance("service_name", "test", instance3);
        Assertions.assertEquals(3, namingServiceList.size());
        Mockito.verify(namingServiceList.get(0), Mockito.times(1)).registerInstance("service_name", "test", instance1);
        Mockito.verify(namingServiceList.get(1), Mockito.times(1)).registerInstance("service_name", "test", instance2);
        Mockito.verify(namingServiceList.get(2), Mockito.times(1)).registerInstance("service_name", "test", instance3);

        nacosNamingServiceWrapper.deregisterInstance("service_name", "test", instance1);
        Mockito.verify(namingServiceList.get(0), Mockito.times(1)).deregisterInstance("service_name", "test", instance1);

        nacosNamingServiceWrapper.deregisterInstance("service_name", "test", instance2);
        Mockito.verify(namingServiceList.get(1), Mockito.times(1)).deregisterInstance("service_name", "test", instance2);

        nacosNamingServiceWrapper.registerInstance("service_name", "test", instance1);
        nacosNamingServiceWrapper.registerInstance("service_name", "test", instance2);
        Mockito.verify(namingServiceList.get(0), Mockito.times(2)).registerInstance(Mockito.eq("service_name"), Mockito.eq("test"), Mockito.any());
        Mockito.verify(namingServiceList.get(1), Mockito.times(2)).registerInstance(Mockito.eq("service_name"), Mockito.eq("test"), Mockito.any());

        nacosNamingServiceWrapper.deregisterInstance("service_name", "test", instance1);
        nacosNamingServiceWrapper.deregisterInstance("service_name", "test", instance2);
        Mockito.verify(namingServiceList.get(0), Mockito.times(2)).deregisterInstance(Mockito.eq("service_name"), Mockito.eq("test"), Mockito.any());
        Mockito.verify(namingServiceList.get(1), Mockito.times(2)).deregisterInstance(Mockito.eq("service_name"), Mockito.eq("test"), Mockito.any());

        nacosNamingServiceWrapper.deregisterInstance("service_name", "test", instance3);
        Mockito.verify(namingServiceList.get(2), Mockito.times(1)).deregisterInstance("service_name", "test", instance3);
    }

    @Test
    void testRegisterNacos2_1_xClient2_1_xServer() throws NacosException {
        List<NamingService> namingServiceList = new LinkedList<>();
        NacosConnectionManager nacosConnectionManager = new NacosConnectionManager(URL.valueOf(""), false, 0, 0) {
            @Override
            protected NamingService createNamingService() {
                NamingService namingService = Mockito.mock(NamingService.class);
                namingServiceList.add(namingService);
                return namingService;
            }
        };

        Assertions.assertEquals(1, namingServiceList.size());

        NacosNamingServiceWrapper nacosNamingServiceWrapper = new NacosNamingServiceWrapper(nacosConnectionManager, true, 0, 0);

        Instance instance1 = new Instance();
        instance1.setIp("ip1");
        instance1.setPort(1);
        nacosNamingServiceWrapper.registerInstance("service_name", "test", instance1);
        Mockito.verify(namingServiceList.get(0), Mockito.times(1)).registerInstance("service_name", "test", instance1);

        Instance instance2 = new Instance();
        instance2.setIp("ip2");
        instance2.setPort(2);
        nacosNamingServiceWrapper.registerInstance("service_name", "test", instance2);
        Assertions.assertEquals(1, namingServiceList.size());
        Mockito.verify(namingServiceList.get(0), Mockito.times(1)).batchRegisterInstance(Mockito.eq("service_name"), Mockito.eq("test"), Mockito.eq(new ArrayList<>(Arrays.asList(instance1, instance2))));

        Instance instance3 = new Instance();
        instance3.setIp("ip3");
        instance3.setPort(3);
        nacosNamingServiceWrapper.registerInstance("service_name", "test", instance3);
        Assertions.assertEquals(1, namingServiceList.size());
        Mockito.verify(namingServiceList.get(0), Mockito.times(1)).batchRegisterInstance(Mockito.eq("service_name"), Mockito.eq("test"), Mockito.eq(new ArrayList<>(Arrays.asList(instance1, instance2, instance3))));

        nacosNamingServiceWrapper.deregisterInstance("service_name", "test", instance1);
        Mockito.verify(namingServiceList.get(0), Mockito.times(1)).batchRegisterInstance(Mockito.eq("service_name"), Mockito.eq("test"), Mockito.eq(new ArrayList<>(Arrays.asList(instance2, instance3))));

        nacosNamingServiceWrapper.deregisterInstance("service_name", "test", instance2);
        Mockito.verify(namingServiceList.get(0), Mockito.times(1)).batchRegisterInstance(Mockito.eq("service_name"), Mockito.eq("test"), Mockito.eq(Collections.singletonList(instance3)));

        nacosNamingServiceWrapper.registerInstance("service_name", "test", instance1);
        Assertions.assertEquals(1, namingServiceList.size());
        Mockito.verify(namingServiceList.get(0), Mockito.times(1)).batchRegisterInstance(Mockito.eq("service_name"), Mockito.eq("test"), Mockito.eq(new ArrayList<>(Arrays.asList(instance3, instance1))));

        nacosNamingServiceWrapper.registerInstance("service_name", "test", instance2);
        Assertions.assertEquals(1, namingServiceList.size());
        Mockito.verify(namingServiceList.get(0), Mockito.times(1)).batchRegisterInstance(Mockito.eq("service_name"), Mockito.eq("test"), Mockito.eq(new ArrayList<>(Arrays.asList(instance3, instance1, instance2))));

        nacosNamingServiceWrapper.deregisterInstance("service_name", "test", instance1);
        Mockito.verify(namingServiceList.get(0), Mockito.times(1)).batchRegisterInstance(Mockito.eq("service_name"), Mockito.eq("test"), Mockito.eq(new ArrayList<>(Arrays.asList(instance3, instance2))));

        nacosNamingServiceWrapper.deregisterInstance("service_name", "test", instance2);
        Mockito.verify(namingServiceList.get(0), Mockito.times(2)).batchRegisterInstance(Mockito.eq("service_name"), Mockito.eq("test"), Mockito.eq(Collections.singletonList(instance3)));

        nacosNamingServiceWrapper.deregisterInstance("service_name", "test", instance3);
        Mockito.verify(namingServiceList.get(0), Mockito.times(1)).deregisterInstance("service_name", "test", instance3);

        nacosNamingServiceWrapper.deregisterInstance("service_name", "test", instance3);
        Mockito.verify(namingServiceList.get(0), Mockito.times(1)).deregisterInstance("service_name", "test", instance3);


        // rerun
        nacosNamingServiceWrapper.registerInstance("service_name", "test", instance1);
        Mockito.verify(namingServiceList.get(0), Mockito.times(2)).registerInstance("service_name", "test", instance1);

        nacosNamingServiceWrapper.registerInstance("service_name", "test", instance2);
        Assertions.assertEquals(1, namingServiceList.size());
        Mockito.verify(namingServiceList.get(0), Mockito.times(2)).batchRegisterInstance(Mockito.eq("service_name"), Mockito.eq("test"), Mockito.eq(new ArrayList<>(Arrays.asList(instance1, instance2))));

        nacosNamingServiceWrapper.registerInstance("service_name", "test", instance3);
        Assertions.assertEquals(1, namingServiceList.size());
        Mockito.verify(namingServiceList.get(0), Mockito.times(2)).batchRegisterInstance(Mockito.eq("service_name"), Mockito.eq("test"), Mockito.eq(new ArrayList<>(Arrays.asList(instance1, instance2, instance3))));

        nacosNamingServiceWrapper.deregisterInstance("service_name", "test", instance1);
        Mockito.verify(namingServiceList.get(0), Mockito.times(2)).batchRegisterInstance(Mockito.eq("service_name"), Mockito.eq("test"), Mockito.eq(new ArrayList<>(Arrays.asList(instance2, instance3))));

        nacosNamingServiceWrapper.deregisterInstance("service_name", "test", instance2);
        Mockito.verify(namingServiceList.get(0), Mockito.times(3)).batchRegisterInstance(Mockito.eq("service_name"), Mockito.eq("test"), Mockito.eq(Collections.singletonList(instance3)));

        nacosNamingServiceWrapper.registerInstance("service_name", "test", instance1);
        Assertions.assertEquals(1, namingServiceList.size());
        Mockito.verify(namingServiceList.get(0), Mockito.times(2)).batchRegisterInstance(Mockito.eq("service_name"), Mockito.eq("test"), Mockito.eq(new ArrayList<>(Arrays.asList(instance3, instance1))));

        nacosNamingServiceWrapper.registerInstance("service_name", "test", instance2);
        Assertions.assertEquals(1, namingServiceList.size());
        Mockito.verify(namingServiceList.get(0), Mockito.times(2)).batchRegisterInstance(Mockito.eq("service_name"), Mockito.eq("test"), Mockito.eq(new ArrayList<>(Arrays.asList(instance3, instance1, instance2))));

        nacosNamingServiceWrapper.deregisterInstance("service_name", "test", instance1);
        Mockito.verify(namingServiceList.get(0), Mockito.times(2)).batchRegisterInstance(Mockito.eq("service_name"), Mockito.eq("test"), Mockito.eq(new ArrayList<>(Arrays.asList(instance3, instance2))));

        nacosNamingServiceWrapper.deregisterInstance("service_name", "test", instance2);
        Mockito.verify(namingServiceList.get(0), Mockito.times(4)).batchRegisterInstance(Mockito.eq("service_name"), Mockito.eq("test"), Mockito.eq(Collections.singletonList(instance3)));

        nacosNamingServiceWrapper.deregisterInstance("service_name", "test", instance3);
        Mockito.verify(namingServiceList.get(0), Mockito.times(2)).deregisterInstance("service_name", "test", instance3);

        nacosNamingServiceWrapper.deregisterInstance("service_name", "test", instance3);
        Mockito.verify(namingServiceList.get(0), Mockito.times(2)).deregisterInstance("service_name", "test", instance3);
    }


    @Test
    void testUnregister() throws NacosException {
        List<NamingService> namingServiceList = new LinkedList<>();
        NacosConnectionManager nacosConnectionManager = new NacosConnectionManager(URL.valueOf(""), false, 0, 0) {
            @Override
            protected NamingService createNamingService() {
                NamingService namingService = Mockito.mock(NamingService.class);
                namingServiceList.add(namingService);
                return namingService;
            }
        };

        Assertions.assertEquals(1, namingServiceList.size());

        NacosNamingServiceWrapper nacosNamingServiceWrapper = new NacosNamingServiceWrapper(nacosConnectionManager, true, 0, 0);

        Instance instance1 = new Instance();
        instance1.setIp("ip1");
        instance1.setPort(1);
        nacosNamingServiceWrapper.registerInstance("service_name", "test", instance1);
        Mockito.verify(namingServiceList.get(0), Mockito.times(1)).registerInstance("service_name", "test", instance1);

        Instance instance2 = new Instance();
        instance2.setIp("ip2");
        instance2.setPort(2);
        nacosNamingServiceWrapper.registerInstance("service_name", "test", instance2);
        Assertions.assertEquals(1, namingServiceList.size());
        Mockito.verify(namingServiceList.get(0), Mockito.times(1)).batchRegisterInstance(Mockito.eq("service_name"), Mockito.eq("test"), Mockito.eq(new ArrayList<>(Arrays.asList(instance1, instance2))));

        Instance instance3 = new Instance();
        instance3.setIp("ip3");
        instance3.setPort(3);
        nacosNamingServiceWrapper.registerInstance("service_name", "test", instance3);
        Assertions.assertEquals(1, namingServiceList.size());
        Mockito.verify(namingServiceList.get(0), Mockito.times(1)).batchRegisterInstance(Mockito.eq("service_name"), Mockito.eq("test"), Mockito.eq(new ArrayList<>(Arrays.asList(instance1, instance2, instance3))));

        nacosNamingServiceWrapper.deregisterInstance("service_name", "test", instance1);
        nacosNamingServiceWrapper.deregisterInstance("service_name", "test", instance1);
        Mockito.verify(namingServiceList.get(0), Mockito.times(1)).batchRegisterInstance(Mockito.eq("service_name"), Mockito.eq("test"), Mockito.eq(new ArrayList<>(Arrays.asList(instance2, instance3))));

        nacosNamingServiceWrapper.deregisterInstance("service_name", "test", instance2);
        nacosNamingServiceWrapper.deregisterInstance("service_name", "test", instance2);
        Mockito.verify(namingServiceList.get(0), Mockito.times(1)).batchRegisterInstance(Mockito.eq("service_name"), Mockito.eq("test"), Mockito.eq(Collections.singletonList(instance3)));

        nacosNamingServiceWrapper.registerInstance("service_name", "test", instance1);
        Assertions.assertEquals(1, namingServiceList.size());
        Mockito.verify(namingServiceList.get(0), Mockito.times(1)).batchRegisterInstance(Mockito.eq("service_name"), Mockito.eq("test"), Mockito.eq(new ArrayList<>(Arrays.asList(instance3, instance1))));

        nacosNamingServiceWrapper.registerInstance("service_name", "test", instance2);
        Assertions.assertEquals(1, namingServiceList.size());
        Mockito.verify(namingServiceList.get(0), Mockito.times(1)).batchRegisterInstance(Mockito.eq("service_name"), Mockito.eq("test"), Mockito.eq(new ArrayList<>(Arrays.asList(instance3, instance1, instance2))));

        nacosNamingServiceWrapper.deregisterInstance("service_name", "test", "ip2", 1);
        nacosNamingServiceWrapper.deregisterInstance("service_name", "test", "ip1", 2);
        nacosNamingServiceWrapper.deregisterInstance("service_name", "test", "ip1", 1);
        Mockito.verify(namingServiceList.get(0), Mockito.times(1)).batchRegisterInstance(Mockito.eq("service_name"), Mockito.eq("test"), Mockito.eq(new ArrayList<>(Arrays.asList(instance3, instance2))));

        nacosNamingServiceWrapper.deregisterInstance("service_name", "test", "ip2", 2);
        nacosNamingServiceWrapper.deregisterInstance("service_name", "test", "ip2", 2);
        Mockito.verify(namingServiceList.get(0), Mockito.times(2)).batchRegisterInstance(Mockito.eq("service_name"), Mockito.eq("test"), Mockito.eq(Collections.singletonList(instance3)));

        nacosNamingServiceWrapper.deregisterInstance("service_name", "test", "ip3", 3);
        nacosNamingServiceWrapper.deregisterInstance("service_name", "test", "ip3", 3);
        Mockito.verify(namingServiceList.get(0), Mockito.times(1)).deregisterInstance("service_name", "test", instance3);

        // rerun
        nacosNamingServiceWrapper.registerInstance("service_name", "test", instance1);
        Mockito.verify(namingServiceList.get(0), Mockito.times(2)).registerInstance("service_name", "test", instance1);

        nacosNamingServiceWrapper.registerInstance("service_name", "test", instance2);
        Assertions.assertEquals(1, namingServiceList.size());
        Mockito.verify(namingServiceList.get(0), Mockito.times(2)).batchRegisterInstance(Mockito.eq("service_name"), Mockito.eq("test"), Mockito.eq(new ArrayList<>(Arrays.asList(instance1, instance2))));

        nacosNamingServiceWrapper.registerInstance("service_name", "test", instance3);
        Assertions.assertEquals(1, namingServiceList.size());
        Mockito.verify(namingServiceList.get(0), Mockito.times(2)).batchRegisterInstance(Mockito.eq("service_name"), Mockito.eq("test"), Mockito.eq(new ArrayList<>(Arrays.asList(instance1, instance2, instance3))));

        nacosNamingServiceWrapper.deregisterInstance("service_name", "test", "ip1", 1);
        Mockito.verify(namingServiceList.get(0), Mockito.times(2)).batchRegisterInstance(Mockito.eq("service_name"), Mockito.eq("test"), Mockito.eq(new ArrayList<>(Arrays.asList(instance2, instance3))));

        nacosNamingServiceWrapper.deregisterInstance("service_name", "test", "ip2", 2);
        Mockito.verify(namingServiceList.get(0), Mockito.times(3)).batchRegisterInstance(Mockito.eq("service_name"), Mockito.eq("test"), Mockito.eq(Collections.singletonList(instance3)));

        nacosNamingServiceWrapper.registerInstance("service_name", "test", instance1);
        Assertions.assertEquals(1, namingServiceList.size());
        Mockito.verify(namingServiceList.get(0), Mockito.times(2)).batchRegisterInstance(Mockito.eq("service_name"), Mockito.eq("test"), Mockito.eq(new ArrayList<>(Arrays.asList(instance3, instance1))));

        nacosNamingServiceWrapper.registerInstance("service_name", "test", instance2);
        Assertions.assertEquals(1, namingServiceList.size());
        Mockito.verify(namingServiceList.get(0), Mockito.times(2)).batchRegisterInstance(Mockito.eq("service_name"), Mockito.eq("test"), Mockito.eq(new ArrayList<>(Arrays.asList(instance3, instance1, instance2))));

        nacosNamingServiceWrapper.deregisterInstance("service_name", "test", "ip1", 1);
        Mockito.verify(namingServiceList.get(0), Mockito.times(2)).batchRegisterInstance(Mockito.eq("service_name"), Mockito.eq("test"), Mockito.eq(new ArrayList<>(Arrays.asList(instance3, instance2))));

        nacosNamingServiceWrapper.deregisterInstance("service_name", "test", "ip2", 2);
        Mockito.verify(namingServiceList.get(0), Mockito.times(4)).batchRegisterInstance(Mockito.eq("service_name"), Mockito.eq("test"), Mockito.eq(Collections.singletonList(instance3)));

        nacosNamingServiceWrapper.deregisterInstance("service_name", "test", "ip3", 3);
        Mockito.verify(namingServiceList.get(0), Mockito.times(2)).deregisterInstance("service_name", "test", instance3);
    }

    @Test
    void testConcurrency() throws NacosException, InterruptedException {
        NacosConnectionManager connectionManager = Mockito.mock(NacosConnectionManager.class);


        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch stopLatch = new CountDownLatch(1);
        NamingService namingService = Mockito.mock(NamingService.class);
        Mockito.when(connectionManager.getNamingService()).thenReturn(namingService);

        NacosNamingServiceWrapper nacosNamingServiceWrapper = new NacosNamingServiceWrapper(connectionManager, false, 0, 0);

        Instance instance = new Instance();
        nacosNamingServiceWrapper.registerInstance("service_name", "test", instance);

        NacosNamingServiceWrapper.InstancesInfo instancesInfo = nacosNamingServiceWrapper.getRegisterStatus().get(new NacosNamingServiceWrapper.InstanceId("service_name", "test"));
        Assertions.assertEquals(1, instancesInfo.getInstances().size());

        nacosNamingServiceWrapper.getRegisterStatus().put(new NacosNamingServiceWrapper.InstanceId("service_name", "test"), new NacosNamingServiceWrapper.InstancesInfo(){
            private final NacosNamingServiceWrapper.InstancesInfo delegate = instancesInfo;

            @Override
            public void lock() {
                delegate.lock();
            }

            @Override
            public void unlock() {
                delegate.unlock();
            }

            @Override
            public List<NacosNamingServiceWrapper.InstanceInfo> getInstances() {
                try {
                    if (startLatch.getCount() > 0) {
                        Thread.sleep(1000);
                        startLatch.countDown();
                        Thread.sleep(1000);
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                return delegate.getInstances();
            }

            @Override
            public boolean isBatchRegistered() {
                return delegate.isBatchRegistered();
            }

            @Override
            public void setBatchRegistered(boolean batchRegistered) {
                delegate.setBatchRegistered(batchRegistered);
            }

            @Override
            public boolean isValid() {
                return delegate.isValid();
            }

            @Override
            public void setValid(boolean valid) {
                delegate.setValid(valid);
            }
        });

        new Thread(()->{
            try {
                startLatch.await();
                nacosNamingServiceWrapper.registerInstance("service_name", "test", instance);
                stopLatch.countDown();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).start();

        new Thread(()->{
            try {
                nacosNamingServiceWrapper.deregisterInstance("service_name", "test", instance);
            } catch (NacosException e) {
                throw new RuntimeException(e);
            }
        }).start();

        stopLatch.await();
        NacosNamingServiceWrapper.InstancesInfo instancesInfoNew = nacosNamingServiceWrapper.getRegisterStatus().get(new NacosNamingServiceWrapper.InstanceId("service_name", "test"));
        Assertions.assertEquals(1, instancesInfoNew.getInstances().size());

        Assertions.assertNotEquals(instancesInfo, instancesInfoNew);
    }


    @Test
    void testSuccess() {
        NamingService namingService = new MockNamingService() {
            @Override
            public void registerInstance(String serviceName, String groupName, Instance instance) {

            }

            @Override
            public List<Instance> getAllInstances(String serviceName, String groupName) {
                return null;
            }
        };

        NacosNamingServiceWrapper nacosNamingServiceWrapper = new NacosNamingServiceWrapper(new NacosConnectionManager(namingService), 0, 0);
        try {
            nacosNamingServiceWrapper.registerInstance("Test", "Test", null);
        } catch (NacosException e) {
            Assertions.fail(e);
        }
        try {
            nacosNamingServiceWrapper.getAllInstances("Test", "Test");
        } catch (NacosException e) {
            Assertions.fail(e);
        }
    }

    @Test
    void testFailNoRetry() {
        NamingService namingService = new MockNamingService() {
            @Override
            public void registerInstance(String serviceName, String groupName, Instance instance) throws NacosException {
                throw new NacosException();
            }

            @Override
            public List<Instance> getAllInstances(String serviceName, String groupName) throws NacosException {
                throw new NacosException();
            }
        };

        NacosNamingServiceWrapper nacosNamingServiceWrapper = new NacosNamingServiceWrapper(new NacosConnectionManager(namingService), 0, 0);
        Assertions.assertThrows(NacosException.class, () -> nacosNamingServiceWrapper.registerInstance("Test", "Test", null));
        Assertions.assertThrows(NacosException.class, () -> nacosNamingServiceWrapper.getAllInstances("Test", "Test"));
    }


    @Test
    void testFailRetry() {
        NamingService namingService = new MockNamingService() {
            private final AtomicInteger count1 = new AtomicInteger(0);
            private final AtomicInteger count2 = new AtomicInteger(0);

            @Override
            public void registerInstance(String serviceName, String groupName, Instance instance) throws NacosException {
                if (count1.incrementAndGet() < 10) {
                    throw new NacosException();
                }
            }

            @Override
            public List<Instance> getAllInstances(String serviceName, String groupName) throws NacosException {
                if (count2.incrementAndGet() < 10) {
                    throw new NacosException();
                }
                return null;
            }
        };

        NacosNamingServiceWrapper nacosNamingServiceWrapper = new NacosNamingServiceWrapper(new NacosConnectionManager(namingService), 5, 10);
        Assertions.assertThrows(NacosException.class, () -> nacosNamingServiceWrapper.registerInstance("Test", "Test", null));
        try {
            nacosNamingServiceWrapper.registerInstance("Test", "Test", null);
        } catch (NacosException e) {
            Assertions.fail(e);
        }

        Assertions.assertThrows(NacosException.class, () -> nacosNamingServiceWrapper.getAllInstances("Test", "Test"));
        try {
            nacosNamingServiceWrapper.getAllInstances("Test", "Test");
        } catch (NacosException e) {
            Assertions.fail(e);
        }

    }
}
