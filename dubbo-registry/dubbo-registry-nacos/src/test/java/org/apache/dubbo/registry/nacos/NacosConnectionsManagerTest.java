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

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import static com.alibaba.nacos.client.constant.Constants.HealthCheck.DOWN;
import static com.alibaba.nacos.client.constant.Constants.HealthCheck.UP;
import static org.mockito.ArgumentMatchers.any;

public class NacosConnectionsManagerTest {
    @Test
    public void testGet() {
        NamingService namingService = Mockito.mock(NamingService.class);
        NacosConnectionManager nacosConnectionManager = new NacosConnectionManager(namingService);
        Assertions.assertEquals(namingService, nacosConnectionManager.getNamingService());
        Assertions.assertEquals(namingService, nacosConnectionManager.getNamingService());
        Assertions.assertEquals(namingService, nacosConnectionManager.getNamingService());
    }

    @Test
    public void testCreate() {
        List<NamingService> namingServiceList = new ArrayList<>();
        NacosConnectionManager nacosConnectionManager = new NacosConnectionManager(URL.valueOf(""), false, 0, 0) {
            @Override
            protected NamingService createNamingService() {
                NamingService namingService = Mockito.mock(NamingService.class);
                namingServiceList.add(namingService);
                return namingService;
            }
        };

        Assertions.assertEquals(1, namingServiceList.size());
        Assertions.assertEquals(namingServiceList.get(0), nacosConnectionManager.getNamingService());
        Assertions.assertEquals(namingServiceList.get(0), nacosConnectionManager.getNamingService());
        Assertions.assertEquals(namingServiceList.get(0), nacosConnectionManager.getNamingService());
        Assertions.assertEquals(namingServiceList.get(0), nacosConnectionManager.getNamingService());

        LinkedList<NamingService> copy = new LinkedList<>(namingServiceList);
        Assertions.assertFalse(copy.contains(nacosConnectionManager.getNamingService(new HashSet<>(copy))));
        copy = new LinkedList<>(namingServiceList);
        Assertions.assertFalse(copy.contains(nacosConnectionManager.getNamingService(new HashSet<>(copy))));
        copy = new LinkedList<>(namingServiceList);
        Assertions.assertFalse(copy.contains(nacosConnectionManager.getNamingService(new HashSet<>(copy))));
        copy = new LinkedList<>(namingServiceList);
        Assertions.assertFalse(copy.contains(nacosConnectionManager.getNamingService(new HashSet<>(copy))));

        Assertions.assertEquals(5, namingServiceList.size());

        copy = new LinkedList<>(namingServiceList);
        for (int i = 0; i < 1000; i++) {
            if (copy.size() == 0) {
                break;
            }
            copy.remove(nacosConnectionManager.getNamingService());
        }

        Assertions.assertTrue(copy.isEmpty());

        nacosConnectionManager.shutdownAll();
        copy = new LinkedList<>(namingServiceList);
        Assertions.assertFalse(copy.contains(nacosConnectionManager.getNamingService()));
    }

    @Test
    void testRetryCreate() {
        try (MockedStatic<NacosFactory> nacosFactoryMockedStatic = Mockito.mockStatic(NacosFactory.class)) {
            AtomicInteger atomicInteger = new AtomicInteger(0);
            NamingService mock = new MockNamingService() {
                @Override
                public String getServerStatus() {
                    return atomicInteger.incrementAndGet() > 10 ? UP : DOWN;
                }
            };
            nacosFactoryMockedStatic.when(() -> NacosFactory.createNamingService((Properties) any())).thenReturn(mock);

            URL url = URL.valueOf("nacos://127.0.0.1:8848");
            Assertions.assertThrows(IllegalStateException.class, () -> new NacosConnectionManager(url, true, 5, 10));

            try {
                new NacosConnectionManager(url, true, 5, 10);
            } catch (Throwable t) {
                Assertions.fail(t);
            }
        }
    }
    @Test
    void testNoCheck() {
        try (MockedStatic<NacosFactory> nacosFactoryMockedStatic = Mockito.mockStatic(NacosFactory.class)) {
            NamingService mock = new MockNamingService() {
                @Override
                public String getServerStatus() {
                    return DOWN;
                }
            };
            nacosFactoryMockedStatic.when(() -> NacosFactory.createNamingService((Properties) any())).thenReturn(mock);

            URL url = URL.valueOf("nacos://127.0.0.1:8848");

            try {
                new NacosConnectionManager(url, false, 5, 10);
            } catch (Throwable t) {
                Assertions.fail(t);
            }
        }
    }

    @Test
    void testDisable() {
        try (MockedStatic<NacosFactory> nacosFactoryMockedStatic = Mockito.mockStatic(NacosFactory.class)) {
            NamingService mock = new MockNamingService() {
                @Override
                public String getServerStatus() {
                    return DOWN;
                }
            };
            nacosFactoryMockedStatic.when(() -> NacosFactory.createNamingService((Properties) any())).thenReturn(mock);


            URL url = URL.valueOf("nacos://127.0.0.1:8848")
                .addParameter("nacos.retry", 5)
                .addParameter("nacos.retry-wait", 10)
                .addParameter("nacos.check", "false");
            try {
                new NacosConnectionManager(url, false, 5, 10);
            } catch (Throwable t) {
                Assertions.fail(t);
            }
        }
    }

    @Test
    void testRequest() {
        try (MockedStatic<NacosFactory> nacosFactoryMockedStatic = Mockito.mockStatic(NacosFactory.class)) {
            AtomicInteger atomicInteger = new AtomicInteger(0);
            NamingService mock = new MockNamingService() {
                @Override
                public List<Instance> getAllInstances(String serviceName, boolean subscribe) throws NacosException {
                    if (atomicInteger.incrementAndGet() > 10) {
                        return null;
                    } else {
                        throw new NacosException();
                    }
                }

                @Override
                public String getServerStatus() {
                    return UP;
                }
            };
            nacosFactoryMockedStatic.when(() -> NacosFactory.createNamingService((Properties) any())).thenReturn(mock);


            URL url = URL.valueOf("nacos://127.0.0.1:8848")
                .addParameter("nacos.retry", 5)
                .addParameter("nacos.retry-wait", 10);
            Assertions.assertThrows(IllegalStateException.class, () -> new NacosConnectionManager(url, true, 5, 10));

            try {
                new NacosConnectionManager(url, true, 5, 10);
            } catch (Throwable t) {
                Assertions.fail(t);
            }
        }
    }
}
