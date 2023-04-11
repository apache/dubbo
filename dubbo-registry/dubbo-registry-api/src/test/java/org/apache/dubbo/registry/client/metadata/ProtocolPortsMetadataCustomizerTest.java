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
import org.apache.dubbo.common.beans.factory.ScopeBeanFactory;
import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.List;

import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.ENDPOINTS;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProtocolPortsMetadataCustomizerTest {

    public DefaultServiceInstance instance;
    private MetadataService mockedMetadataService;
    private ApplicationModel mockedApplicationModel;
    private ScopeBeanFactory mockedBeanFactory;

    public static DefaultServiceInstance createInstance() {
        return new DefaultServiceInstance("A", "127.0.0.1", 20880, ApplicationModel.defaultModel());
    }

    @AfterAll
    public static void clearUp() {
        ApplicationModel.reset();
    }

    @BeforeEach
    public void init() {
        instance = createInstance();

        URL dubboUrl = URL.valueOf("dubbo://30.10.104.63:20880/org.apache.dubbo.demo.GreetingService?" +
            "REGISTRY_CLUSTER=registry1&anyhost=true&application=demo-provider2&delay=5000&deprecated=false&dubbo=2.0.2&dynamic=true&generic=false&group=greeting&interface=org.apache.dubbo.demo.GreetingService&metadata-type=remote&methods=hello&pid=55805&release=&revision=1.0.0&service-name-mapping=true&side=provider&timeout=5000&timestamp=1630229110058&version=1.0.0");
        URL triURL = URL.valueOf("tri://30.10.104.63:50332/org.apache.dubbo.demo.GreetingService?" +
            "REGISTRY_CLUSTER=registry1&anyhost=true&application=demo-provider2&delay=5000&deprecated=false&dubbo=2.0.2&dynamic=true&generic=false&group=greeting&interface=org.apache.dubbo.demo.GreetingService&metadata-type=remote&methods=hello&pid=55805&release=&revision=1.0.0&service-name-mapping=true&side=provider&timeout=5000&timestamp=1630229110058&version=1.0.0");

        MetadataInfo metadataInfo = new MetadataInfo();
        metadataInfo.addService(dubboUrl);
        metadataInfo.addService(triURL);
        instance.setServiceMetadata(metadataInfo);

        mockedMetadataService = Mockito.mock(MetadataService.class);
    }

    @AfterEach
    public void tearDown() throws IOException {
        Mockito.framework().clearInlineMocks();
    }

    @Test
    void test() {
        ProtocolPortsMetadataCustomizer customizer = new ProtocolPortsMetadataCustomizer();
        customizer.customize(instance, ApplicationModel.defaultModel());
        String endpoints = instance.getMetadata().get(ENDPOINTS);
        assertNotNull(endpoints);
        List<DefaultServiceInstance.Endpoint> endpointList = JsonUtils.toJavaList(endpoints, DefaultServiceInstance.Endpoint.class);
        assertEquals(2, endpointList.size());
        MatcherAssert.assertThat(endpointList, hasItem(hasProperty("protocol", equalTo("dubbo"))));
        MatcherAssert.assertThat(endpointList, hasItem(hasProperty("port", equalTo(20880))));
        MatcherAssert.assertThat(endpointList, hasItem(hasProperty("protocol", equalTo("tri"))));
        MatcherAssert.assertThat(endpointList, hasItem(hasProperty("port", equalTo(50332))));
    }
}
