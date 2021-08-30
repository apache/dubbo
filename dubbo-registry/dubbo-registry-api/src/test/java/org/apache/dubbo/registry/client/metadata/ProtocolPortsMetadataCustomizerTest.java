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
package org.apache.dubbo.registry.client.metadata;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.metadata.store.InMemoryWritableMetadataService;
import org.apache.dubbo.rpc.model.ApplicationModel;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.ENDPOINTS;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProtocolPortsMetadataCustomizerTest {
    private static final Gson gson = new Gson();

    public DefaultServiceInstance instance;
    private InMemoryWritableMetadataService metadataService;

    public static DefaultServiceInstance createInstance() {
        return new DefaultServiceInstance("A", "127.0.0.1", 20880);
    }

    @BeforeAll
    public static void setUp() {
        ApplicationConfig applicationConfig = new ApplicationConfig("test");
        ApplicationModel.getConfigManager().setApplication(applicationConfig);
    }

    @AfterAll
    public static void clearUp() {
        ApplicationModel.reset();
    }

    @BeforeEach
    public void init() {
        instance = createInstance();
        metadataService = mock(InMemoryWritableMetadataService.class);

        URL duboURL = URL.valueOf("dubbo://30.10.104.63:20880/org.apache.dubbo.demo.GreetingService?" +
            "REGISTRY_CLUSTER=registry1&anyhost=true&application=demo-provider2&delay=5000&deprecated=false&dubbo=2.0.2&dynamic=true&generic=false&group=greeting&interface=org.apache.dubbo.demo.GreetingService&metadata-type=remote&methods=hello&pid=55805&release=&revision=1.0.0&service-name-mapping=true&side=provider&timeout=5000&timestamp=1630229110058&version=1.0.0");
        URL triURL = URL.valueOf("tri://30.10.104.63:50332/org.apache.dubbo.demo.GreetingService?" +
            "REGISTRY_CLUSTER=registry1&anyhost=true&application=demo-provider2&delay=5000&deprecated=false&dubbo=2.0.2&dynamic=true&generic=false&group=greeting&interface=org.apache.dubbo.demo.GreetingService&metadata-type=remote&methods=hello&pid=55805&release=&revision=1.0.0&service-name-mapping=true&side=provider&timeout=5000&timestamp=1630229110058&version=1.0.0");
        Set<URL> urls = new HashSet<>();
        urls.add(duboURL);
        urls.add(triURL);
        when(metadataService.getExportedServiceURLs()).thenReturn(urls);
    }

    @Test
    public void test() {
        ProtocolPortsMetadataCustomizer customizer = new ProtocolPortsMetadataCustomizer();
        try (MockedStatic<WritableMetadataService> mockMetadataService = Mockito.mockStatic(WritableMetadataService.class)) {
            mockMetadataService.when(() -> WritableMetadataService.getDefaultExtension()).thenReturn(metadataService);
            customizer.customize(instance);
            String endpoints = instance.getMetadata().get(ENDPOINTS);
            assertNotNull(endpoints);
            List<DefaultServiceInstance.Endpoint> endpointList = gson.fromJson(endpoints, new TypeToken<List<DefaultServiceInstance.Endpoint>>(){}.getType());
            assertEquals(2, endpointList.size());
            MatcherAssert.assertThat(endpointList, hasItem(hasProperty("protocol", equalTo("dubbo"))));
            MatcherAssert.assertThat(endpointList, hasItem(hasProperty("port", equalTo(20880))));
            MatcherAssert.assertThat(endpointList, hasItem(hasProperty("protocol", equalTo("tri"))));
            MatcherAssert.assertThat(endpointList, hasItem(hasProperty("port", equalTo(50332))));
        }
    }
}
