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
package org.apache.dubbo.registry.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

/**
 * AbstractRegistryFactoryTest
 */
public class AbstractRegistryFactoryTest {

    private AbstractRegistryFactory registryFactory;

    @BeforeEach
    public void setup() {
        registryFactory = new AbstractRegistryFactory() {

            @Override
            protected Registry createRegistry(final URL url) {
                return new Registry() {

                    public URL getUrl() {
                        return url;
                    }

                    @Override
                    public boolean isAvailable() {
                        return false;
                    }

                    @Override
                    public void destroy() {
                    }

                    @Override
                    public void register(URL url) {
                    }

                    @Override
                    public void unregister(URL url) {
                    }

                    @Override
                    public void subscribe(URL url, NotifyListener listener) {
                    }

                    @Override
                    public void unsubscribe(URL url, NotifyListener listener) {
                    }

                    @Override
                    public List<URL> lookup(URL url) {
                        return null;
                    }

                };
            }
        };
        registryFactory.setApplicationModel(ApplicationModel.defaultModel());
    }

    @AfterEach
    public void teardown() {
        ApplicationModel.defaultModel().destroy();
    }

    @Test
    public void testRegistryFactoryCache() throws Exception {
        URL url = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostAddress() + ":2233");
        Registry registry1 = registryFactory.getRegistry(url);
        Registry registry2 = registryFactory.getRegistry(url);
        Assertions.assertEquals(registry1, registry2);
    }

    /**
     * Registration center address `dubbo` does not resolve
     */
    // @Test
    public void testRegistryFactoryIpCache() throws Exception {
        Registry registry1 = registryFactory.getRegistry(URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":2233"));
        Registry registry2 = registryFactory.getRegistry(URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostAddress() + ":2233"));
        Assertions.assertEquals(registry1, registry2);
    }

    @Test
    public void testRegistryFactoryGroupCache() throws Exception {
        Registry registry1 = registryFactory.getRegistry(URL.valueOf("dubbo://" + NetUtils.getLocalHost() + ":2233?group=aaa"));
        Registry registry2 = registryFactory.getRegistry(URL.valueOf("dubbo://" + NetUtils.getLocalHost() + ":2233?group=bbb"));
        Assertions.assertNotSame(registry1, registry2);
    }

    @Test
    public void testDestroyAllRegistries() {
        Registry registry1 = registryFactory.getRegistry(URL.valueOf("dubbo://" + NetUtils.getLocalHost() + ":8888?group=xxx"));
        Registry registry2 = registryFactory.getRegistry(URL.valueOf("dubbo://" + NetUtils.getLocalHost() + ":9999?group=yyy"));
        Registry registry3 = new AbstractRegistry(URL.valueOf("dubbo://" + NetUtils.getLocalHost() + ":2020?group=yyy")) {
            @Override
            public boolean isAvailable() {
                return true;
            }
        };

        RegistryManager registryManager = ApplicationModel.defaultModel().getBeanFactory().getBean(RegistryManager.class);

        Collection<Registry> registries = registryManager.getRegistries();
        Assertions.assertTrue(registries.contains(registry1));
        Assertions.assertTrue(registries.contains(registry2));
        registry3.destroy();
        registries = registryManager.getRegistries();
        Assertions.assertFalse(registries.contains(registry3));
        registryManager.destroyAll();
        registries = registryManager.getRegistries();
        Assertions.assertFalse(registries.contains(registry1));
        Assertions.assertFalse(registries.contains(registry2));
    }
}
