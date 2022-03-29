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
package org.apache.dubbo.registry;


import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.url.component.ServiceConfigURL;
import org.apache.dubbo.registry.integration.DemoService;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.registry.Constants.REGISTER_IP_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.REFER_KEY;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ListenerRegistryWrapperTest {

    @Test
    public void testSubscribe() {

        Map<String, String> parameters = new HashMap<>();
        parameters.put(INTERFACE_KEY, DemoService.class.getName());
        parameters.put("registry", "zookeeper");
        parameters.put("register", "true");
        parameters.put(REGISTER_IP_KEY, "172.23.236.180");
        parameters.put("registry.listeners", "listener-one");

        Map<String, Object> attributes = new HashMap<>();
        ServiceConfigURL serviceConfigURL = new ServiceConfigURL("registry",
            "127.0.0.1",
            2181,
            "org.apache.dubbo.registry.RegistryService",
            parameters);
        Map<String, String> refer = new HashMap<>();
        attributes.put(REFER_KEY, refer);
        attributes.put("key1", "value1");
        URL url = serviceConfigURL.addAttributes(attributes);

        RegistryFactory registryFactory = mock(RegistryFactory.class);
        Registry registry = mock(Registry.class);
        NotifyListener notifyListener = mock(NotifyListener.class);
        when(registryFactory.getRegistry(url)).thenReturn(registry);

        RegistryFactoryWrapper registryFactoryWrapper = new RegistryFactoryWrapper(registryFactory);
        Registry registryWrapper = registryFactoryWrapper.getRegistry(url);

        Assertions.assertTrue(registryWrapper instanceof ListenerRegistryWrapper);

        URL subscribeUrl = new ServiceConfigURL("dubbo",
            "127.0.0.1",
            20881,
            DemoService.class.getName(),
            parameters);

        RegistryServiceListener listener = Mockito.mock(RegistryServiceListener.class);
        RegistryServiceListener1.delegate = listener;

        registryWrapper.subscribe(subscribeUrl, notifyListener);
        verify(listener, times(1)).onSubscribe(subscribeUrl, registry);
    }

}
