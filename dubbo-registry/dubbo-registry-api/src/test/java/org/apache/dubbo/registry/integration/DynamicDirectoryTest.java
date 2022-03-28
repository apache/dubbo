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
package org.apache.dubbo.registry.integration;


import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.url.component.ServiceConfigURL;
import org.apache.dubbo.registry.Registry;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.CATEGORY_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.CONSUMERS_CATEGORY;
import static org.apache.dubbo.registry.Constants.REGISTER_IP_KEY;
import static org.apache.dubbo.registry.Constants.SIMPLIFIED_KEY;
import static org.apache.dubbo.registry.integration.RegistryProtocol.DEFAULT_REGISTER_CONSUMER_KEYS;
import static org.apache.dubbo.remoting.Constants.CHECK_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.REFER_KEY;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class DynamicDirectoryTest {

    /**
     * verify simplified consumer url information that needs to be registered
     */
    @Test
    public void testSimplifiedUrl() {

        // verify that the consumer url information that needs to be registered is not simplified by default
        Map<String, String> parameters = new HashMap<>();
        parameters.put(INTERFACE_KEY, DemoService.class.getName());
        parameters.put("registry", "zookeeper");
        parameters.put("register", "true");
        parameters.put(REGISTER_IP_KEY, "172.23.236.180");


        Map<String, Object> attributes = new HashMap<>();
        ServiceConfigURL serviceConfigURLWithoutSimplified = new ServiceConfigURL("registry",
            "127.0.0.1",
            2181,
            "org.apache.dubbo.registry.RegistryService",
            parameters);
        Map<String, String> refer = new HashMap<>();
        attributes.put(REFER_KEY, refer);
        attributes.put("key1", "value1");
        URL urlWithoutSimplified = serviceConfigURLWithoutSimplified.addAttributes(attributes);

        DemoDynamicDirectory<DemoService> dynamicDirectoryWithoutSimplified =
            new DemoDynamicDirectory<>(DemoService.class, urlWithoutSimplified);

        URL registeredConsumerUrlWithoutSimplified = new ServiceConfigURL("dubbo",
            "127.0.0.1",
            2181,
            DemoService.class.getName(),
            parameters);

        dynamicDirectoryWithoutSimplified.setRegisteredConsumerUrl(registeredConsumerUrlWithoutSimplified);

        URL urlForNotSimplified = registeredConsumerUrlWithoutSimplified
            .addParameters(CATEGORY_KEY, CONSUMERS_CATEGORY, CHECK_KEY, String.valueOf(false));

        Assertions.assertEquals(urlForNotSimplified, dynamicDirectoryWithoutSimplified.getRegisteredConsumerUrl());

        // verify simplified consumer url information that needs to be registered
        parameters.put(SIMPLIFIED_KEY, "true");
        ServiceConfigURL serviceConfigURLWithSimplified = new ServiceConfigURL("registry",
            "127.0.0.1",
            2181,
            "org.apache.dubbo.registry.RegistryService",
            parameters);
        URL urlWithSimplified = serviceConfigURLWithSimplified.addAttributes(attributes);
        DemoDynamicDirectory<DemoService> dynamicDirectoryWithSimplified = new DemoDynamicDirectory<>(DemoService.class, urlWithSimplified);

        URL registeredConsumerUrlWithSimplified = new ServiceConfigURL("dubbo",
            "127.0.0.1",
            2181,
            DemoService.class.getName(),
            parameters);

        dynamicDirectoryWithSimplified.setRegisteredConsumerUrl(registeredConsumerUrlWithSimplified);

        URL urlForSimplified = URL.valueOf(
            registeredConsumerUrlWithSimplified,
            DEFAULT_REGISTER_CONSUMER_KEYS,
            null).addParameters(CATEGORY_KEY, CONSUMERS_CATEGORY, CHECK_KEY, String.valueOf(false));

        Assertions.assertEquals(urlForSimplified, dynamicDirectoryWithSimplified.getRegisteredConsumerUrl());

    }


    @Test
    public void testSubscribe() {

        Map<String, String> parameters = new HashMap<>();
        parameters.put(INTERFACE_KEY, DemoService.class.getName());
        parameters.put("registry", "zookeeper");
        parameters.put("register", "true");
        parameters.put(REGISTER_IP_KEY, "172.23.236.180");


        Map<String, Object> attributes = new HashMap<>();
        ServiceConfigURL serviceConfigUrl = new ServiceConfigURL("registry",
            "127.0.0.1",
            2181,
            "org.apache.dubbo.registry.RegistryService",
            parameters);
        Map<String, String> refer = new HashMap<>();
        attributes.put(REFER_KEY, refer);
        attributes.put("key1", "value1");
        URL url = serviceConfigUrl.addAttributes(attributes);

        DemoDynamicDirectory<DemoService> demoDynamicDirectory = new DemoDynamicDirectory<>(DemoService.class, url);

        URL subscribeUrl = new ServiceConfigURL("dubbo",
            "127.0.0.1",
            20881,
            DemoService.class.getName(),
            parameters);

        Registry registry = mock(Registry.class);
        demoDynamicDirectory.setRegistry(registry);

        demoDynamicDirectory.subscribe(subscribeUrl);

        verify(registry, times(1)).subscribe(subscribeUrl, demoDynamicDirectory);
        Assertions.assertEquals(subscribeUrl, demoDynamicDirectory.getSubscribeUrl());
    }


    static class DemoDynamicDirectory<T> extends DynamicDirectory<T> {

        public DemoDynamicDirectory(Class<T> serviceType, URL url) {
            super(serviceType, url);
        }

        @Override
        protected void destroyAllInvokers() {

        }

        @Override
        public boolean isAvailable() {
            return false;
        }

        @Override
        public void notify(List<URL> urls) {

        }
    }
}
