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
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class RegistryFactoryWrapperTest {
    private RegistryFactory registryFactory = ExtensionLoader.getExtensionLoader(RegistryFactory.class).getAdaptiveExtension();

    @Test
    public void test() throws Exception {
        RegistryServiceListener listener1 = Mockito.mock(RegistryServiceListener.class);
        RegistryServiceListener1.delegate = listener1;
        RegistryServiceListener listener2 = Mockito.mock(RegistryServiceListener.class);
        RegistryServiceListener2.delegate = listener2;

        Registry registry = registryFactory.getRegistry(URL.valueOf("simple://localhost:8080/registry-service"));
        URL url = URL.valueOf("dubbo://localhost:8081/simple.service");
        registry.register(url);

        Mockito.verify(listener1, Mockito.times(1)).onRegister(url);
        Mockito.verify(listener2, Mockito.times(1)).onRegister(url);

        registry.unregister(url);
        Mockito.verify(listener1, Mockito.times(1)).onUnregister(url);
        Mockito.verify(listener2, Mockito.times(1)).onUnregister(url);

        registry.subscribe(url, Mockito.mock(NotifyListener.class));
        Mockito.verify(listener1, Mockito.times(1)).onSubscribe(url);
        Mockito.verify(listener2, Mockito.times(1)).onSubscribe(url);

        registry.unsubscribe(url, Mockito.mock(NotifyListener.class));
        Mockito.verify(listener1, Mockito.times(1)).onUnsubscribe(url);
        Mockito.verify(listener2, Mockito.times(1)).onUnsubscribe(url);
    }

}
