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
import org.apache.dubbo.common.nacos.NacosAppNameUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static com.alibaba.nacos.client.constant.Constants.HealthCheck.DOWN;
import static com.alibaba.nacos.client.constant.Constants.HealthCheck.UP;
import static org.mockito.ArgumentMatchers.any;

public class NacosConnectionsManagerTest {
    @Test
    void testSetProjectNameFromDubboApplicationNameWhenEnabled() {
        String old = System.getProperty(NacosAppNameUtils.PROJECT_NAME_SYS_PROP_KEY);
        String expectedAppName = "test-dubbo-app-for-nacos";
        ApplicationModel applicationModel = FrameworkModel.defaultModel().newApplication();
        try (MockedStatic<NacosFactory> nacosFactoryMockedStatic = Mockito.mockStatic(NacosFactory.class)) {
            // force NacosFactory call succeed without connecting to server
            NamingService mock = new MockNamingService() {
                @Override
                public String getServerStatus() {
                    return UP;
                }
            };
            nacosFactoryMockedStatic
                    .when(() -> NacosFactory.createNamingService((Properties) any()))
                    .thenReturn(mock);

            System.clearProperty(NacosAppNameUtils.PROJECT_NAME_SYS_PROP_KEY);

            // Set up ApplicationModel with a specific app name
            applicationModel.getApplicationConfigManager().setApplication(new ApplicationConfig(expectedAppName));

            // URL has a scope model so application model is available; enable mapping by parameter
            URL url = URL.valueOf("nacos://127.0.0.1:8848")
                    .addParameter(NacosAppNameUtils.NACOS_SET_PROJECT_NAME_KEY, "true")
                    .setScopeModel(applicationModel);

            new NacosConnectionManager(url, false, 0, 0);

            String projectName = System.getProperty(NacosAppNameUtils.PROJECT_NAME_SYS_PROP_KEY);
            Assertions.assertEquals(expectedAppName, projectName, "project.name should equal dubbo application name");
        } finally {
            // restore to avoid global side effects across tests
            if (old == null) {
                System.clearProperty(NacosAppNameUtils.PROJECT_NAME_SYS_PROP_KEY);
            } else {
                System.setProperty(NacosAppNameUtils.PROJECT_NAME_SYS_PROP_KEY, old);
            }
            applicationModel.destroy();
        }
    }

    @Test
    void testSetProjectNameSkippedWhenDisabled() {
        String old = System.getProperty(NacosAppNameUtils.PROJECT_NAME_SYS_PROP_KEY);
        ApplicationModel applicationModel = FrameworkModel.defaultModel().newApplication();
        try (MockedStatic<NacosFactory> nacosFactoryMockedStatic = Mockito.mockStatic(NacosFactory.class)) {
            NamingService mock = new MockNamingService() {
                @Override
                public String getServerStatus() {
                    return UP;
                }
            };
            nacosFactoryMockedStatic
                    .when(() -> NacosFactory.createNamingService((Properties) any()))
                    .thenReturn(mock);

            System.clearProperty(NacosAppNameUtils.PROJECT_NAME_SYS_PROP_KEY);
            applicationModel.getApplicationConfigManager().setApplication(new ApplicationConfig("some-app"));

            // nacos.set-project-name is NOT set (defaults to false)
            URL url = URL.valueOf("nacos://127.0.0.1:8848").setScopeModel(applicationModel);

            new NacosConnectionManager(url, false, 0, 0);

            String projectName = System.getProperty(NacosAppNameUtils.PROJECT_NAME_SYS_PROP_KEY);
            Assertions.assertNull(projectName, "project.name should NOT be set when feature is disabled");
        } finally {
            if (old == null) {
                System.clearProperty(NacosAppNameUtils.PROJECT_NAME_SYS_PROP_KEY);
            } else {
                System.setProperty(NacosAppNameUtils.PROJECT_NAME_SYS_PROP_KEY, old);
            }
            applicationModel.destroy();
        }
    }

    @Test
    void testSetProjectNameNotOverwriteExisting() {
        String old = System.getProperty(NacosAppNameUtils.PROJECT_NAME_SYS_PROP_KEY);
        String existingValue = "already-set-by-user";
        ApplicationModel applicationModel = FrameworkModel.defaultModel().newApplication();
        try (MockedStatic<NacosFactory> nacosFactoryMockedStatic = Mockito.mockStatic(NacosFactory.class)) {
            NamingService mock = new MockNamingService() {
                @Override
                public String getServerStatus() {
                    return UP;
                }
            };
            nacosFactoryMockedStatic
                    .when(() -> NacosFactory.createNamingService((Properties) any()))
                    .thenReturn(mock);

            // Pre-set the system property
            System.setProperty(NacosAppNameUtils.PROJECT_NAME_SYS_PROP_KEY, existingValue);
            applicationModel.getApplicationConfigManager().setApplication(new ApplicationConfig("dubbo-app"));

            URL url = URL.valueOf("nacos://127.0.0.1:8848")
                    .addParameter(NacosAppNameUtils.NACOS_SET_PROJECT_NAME_KEY, "true")
                    .setScopeModel(applicationModel);

            new NacosConnectionManager(url, false, 0, 0);

            String projectName = System.getProperty(NacosAppNameUtils.PROJECT_NAME_SYS_PROP_KEY);
            Assertions.assertEquals(existingValue, projectName, "project.name should NOT be overwritten");
        } finally {
            if (old == null) {
                System.clearProperty(NacosAppNameUtils.PROJECT_NAME_SYS_PROP_KEY);
            } else {
                System.setProperty(NacosAppNameUtils.PROJECT_NAME_SYS_PROP_KEY, old);
            }
            applicationModel.destroy();
        }
    }

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
            nacosFactoryMockedStatic
                    .when(() -> NacosFactory.createNamingService((Properties) any()))
                    .thenReturn(mock);

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
            nacosFactoryMockedStatic
                    .when(() -> NacosFactory.createNamingService((Properties) any()))
                    .thenReturn(mock);

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
            nacosFactoryMockedStatic
                    .when(() -> NacosFactory.createNamingService((Properties) any()))
                    .thenReturn(mock);

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
            nacosFactoryMockedStatic
                    .when(() -> NacosFactory.createNamingService((Properties) any()))
                    .thenReturn(mock);

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
