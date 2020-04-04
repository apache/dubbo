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

import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.ListView;
import com.alibaba.nacos.client.naming.net.NamingProxy;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.NotifyListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.util.*;

import static org.apache.dubbo.registry.nacos.util.NacosNamingServiceUtils.createNamingService;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class NacosRegistryTest {
    //2.7.5
    private static final String url275 = "nacos://localhost:8848/org.apache.dubbo.registry.RegistryService?application=nacos-registry-demo-consumer-275&dubbo=2.0.2&interface=org.apache.dubbo.registry.RegistryService&pid=90241&release=2.7.5&timestamp=1585993802567";

    //2.7.6
    private static final String url276 = "nacos://localhost:8848/org.apache.dubbo.registry.RegistryService?application=nacos-registry-demo-consumer-276&dubbo=2.0.2&interface=org.apache.dubbo.registry.RegistryService&pid=89448&release=2.7.6&timestamp=1585991562916";

    @ParameterizedTest(name = "testRegistryCacheKey")
    @ValueSource(strings = {
            url275,
            url276
    })
    public void testRegistryCacheKey(String urlString) {
        NacosRegistryFactory nacosRegistryFactory = new NacosRegistryFactory();
        URL url = URL.valueOf(urlString);
        assertEquals(urlString, nacosRegistryFactory.createRegistryCacheKey(url));
    }

    /**
     * In order to test NacosRegistry.isAvailable()
     * <p>
     * use Mockito to mock method {@link NamingProxy#serverHealthy()) ,
     */
    @Test
    public void testRegisterAvailable() throws Exception {
        URL url = URL.valueOf(url275);
        NamingService namingService = createNamingService(url);

        NamingProxy mockProxy = mock(NamingProxy.class);
        when(mockProxy.serverHealthy()).thenReturn(true);

        Field serverProxy = namingService.getClass().getDeclaredField("serverProxy");
        serverProxy.setAccessible(true);
        serverProxy.set(namingService, mockProxy);

        NacosRegistry nacosRegistry = new NacosRegistry(url, namingService);

        assertTrue(nacosRegistry.isAvailable());

        when(mockProxy.serverHealthy()).thenReturn(false);
        assertFalse(nacosRegistry.isAvailable());
    }


    /**
     * test NacosRegistry register(URL url) and unregister(URL url)
     * <p>
     * use Mockito to mock method {@link NamingProxy#reqAPI(String api, Map<String, String> , String)}
     *
     * @throws Exception
     */
    @Test
    public void testDoRegisterAndUnregister() throws Exception {
        URL url = URL.valueOf(url276);
        NamingService namingService = createNamingService(url);

        NamingProxy mockProxy = mock(NamingProxy.class);
        when(mockProxy.reqAPI(anyString(), anyMap(), anyString())).thenReturn("ok");

        Field serverProxy = namingService.getClass().getDeclaredField("serverProxy");
        serverProxy.setAccessible(true);
        serverProxy.set(namingService, mockProxy);

        NacosRegistry registry = new NacosRegistry(url, namingService);

        List<URL> urlList = new ArrayList<>();
        // register
        for (int i = 0; i < 10; i++) {
            URL u = URL.valueOf("nacos://localhost" + i + ":8848/org.apache.dubbo.registry.RegistryService?application=nacos-registry-demo-consumer-276-" + i + "&dubbo=2.0.2&interface=org.apache.dubbo.registry.RegistryService&pid=89448&release=2.7.6&timestamp=1585993802567");

            registry.register(u);
            // regiter twice
            registry.register(u);

            assertEquals(i + 1, registry.getRegistered().size());
            assertTrue(registry.getRegistered().contains(u));
            urlList.add(u);
        }

        urlList.forEach(u -> {
            registry.unregister(u);
            registry.unregister(u);
            assertFalse(registry.getRegistered().contains(u));
        });
    }

    /**
     * test {@link NacosRegistry#subscribe(URL url, NotifyListener listener)}
     * and {@link NacosRegistry#unsubscribe(URL url, NotifyListener listener)}
     * <p>
     * use Mockito to mock {@link NotifyListener}
     */
    @Test
    public void testDoSubscribeAndUnSubscribe() {
        URL url = URL.valueOf(url276);
        NacosRegistry registry = new NacosRegistry(url, createNamingService(url));


        Map<URL, NotifyListener> map = new HashMap<>();

        for (int i = 0; i < 10; i++) {
            URL u = URL.valueOf("nacos://localhost" + i + ":8848/org.apache.dubbo.registry.RegistryService?application=nacos-registry-demo-consumer-276-" + i + "&dubbo=2.0.2&interface=org.apache.dubbo.registry.RegistryService&pid=89448&release=2.7.6&timestamp=1585993802567");
            NotifyListener mockListener = mock(NotifyListener.class);

            registry.subscribe(u, mockListener);
            // subscribe twice
            registry.subscribe(u, mockListener);

            map.put(u, mockListener);

            assertEquals(i + 1, registry.getSubscribed().size());
            assertTrue(registry.getSubscribed().containsKey(u));
        }

        map.forEach((u, listener) -> {
            registry.unsubscribe(u, listener);
            assertEquals(0, registry.getSubscribed().get(u).size());
        });

        assertEquals(map.size(), registry.getSubscribed().size());
    }

    /**
     * testLookup
     *
     * @throws Exception
     */
    @Test
    public void testLookup() throws Exception {
        URL url = URL.valueOf(url276);
        NamingService namingService = createNamingService(url);

        NamingProxy mockProxy = mock(NamingProxy.class);
        when(mockProxy.reqAPI(anyString(), anyMap(), anyString())).thenReturn("ok");

        Field serverProxy = namingService.getClass().getDeclaredField("serverProxy");
        serverProxy.setAccessible(true);
        serverProxy.set(namingService, mockProxy);

        namingService = spy(namingService);

        Instance instance = new Instance();
        instance.addMetadata("protocol", "nacos");
        instance.addMetadata("path", "org.apache.dubbo.registry.RegistryService");

        when(namingService.getAllInstances(anyString())).thenReturn(Collections.singletonList(instance));
        NacosRegistry registry = new NacosRegistry(url, namingService);

        for (int i = 0; i < 10; i++) {
            URL u = URL.valueOf("nacos://localhost" + i + ":8848/org.apache.dubbo.registry.RegistryService?application=nacos-registry-demo-consumer-276-" + i + "&dubbo=2.0.2&interface=org.apache.dubbo.registry.RegistryService&pid=89448&release=2.7.6&timestamp=1585993802567");
            registry.register(u);
            // subscribe twice
            registry.register(u);

            assertTrue(registry.lookup(u).size() > 0);
        }
    }

    /**
     * test look up Admin Url
     * <p>
     * use Mockito to mock method {@link NamingProxy#reqAPI(String api, Map<String, String> , String)}
     * <p>
     * and spy {@link NamingService} to mock return {@link ListView}
     *
     * @throws Exception
     */
    @Test
    public void testAdminUrl() throws Exception {
        URL url = URL.valueOf(url276);
        NamingService namingService = createNamingService(url);

        NamingProxy mockProxy = mock(NamingProxy.class);
        when(mockProxy.reqAPI(anyString(), anyMap(), anyString())).thenReturn("ok");

        Instance instance = new Instance();
        instance.addMetadata("protocol", "nacos");
        instance.addMetadata("path", "org.apache.dubbo.registry.RegistryService");
        instance.addMetadata("group", "group1");
        instance.addMetadata("version", "1");

        Field serverProxy = namingService.getClass().getDeclaredField("serverProxy");
        serverProxy.setAccessible(true);

        namingService = spy(namingService);
        ListView<String> mockListView = mock(ListView.class);
        when(namingService.getAllInstances("providers:org.apache.dubbo.registry.RegistryService:1:group1"))
                .thenReturn(Collections.singletonList(instance));

        serverProxy.set(namingService, mockProxy);
        NacosRegistry registry = new NacosRegistry(url, namingService);


        doReturn(mockListView).when(namingService).getServicesOfServer(anyInt(), anyInt());
        doReturn(Arrays.asList("providers:org.apache.dubbo.registry.RegistryService:1:group1", "providers:org.apache.dubbo.registry.RegistryService:2:group2", "providers:org.apache.dubbo.registry.RegistryService:3:group3")).when(mockListView).getData();
        doReturn(3).when(mockListView).getCount();

        URL url1 = URL.valueOf("admin://localhost:8848/org.apache.dubbo.registry.RegistryService?application=nacos-registry-demo-consumer-276&dubbo=2.0.2&interface=org.apache.dubbo.registry.RegistryService&version=1&group=group1");

        registry.doRegister(url1);
        assertTrue( registry.lookup(url1).size()>0);
    }
}

