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
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.apache.dubbo.registry.client.metadata.MetadataServiceURLBuilderTest.serviceInstance;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link StandardMetadataServiceURLBuilder} Test
 */
public class StandardMetadataServiceURLBuilderTest {

    @BeforeAll
    public static void setUp() {
        ApplicationConfig applicationConfig = new ApplicationConfig("demo");
        applicationConfig.setMetadataServicePort(7001);
        ApplicationModel.defaultModel().getApplicationConfigManager().setApplication(applicationConfig);
    }

    @AfterAll
    public static void clearUp() {
        ApplicationModel.reset();
    }

    @Test
    public void testBuild() {
        ExtensionLoader<MetadataServiceURLBuilder> loader = ApplicationModel.defaultModel()
            .getExtensionLoader(MetadataServiceURLBuilder.class);
        MetadataServiceURLBuilder builder = loader.getExtension(StandardMetadataServiceURLBuilder.NAME);

        // test generateUrlWithoutMetadata
        List<URL> urls = builder.build(new DefaultServiceInstance("test", "127.0.0.1", 8080, ApplicationModel.defaultModel()));
        assertEquals(1, urls.size());
        URL url = urls.get(0);
        assertEquals(url.getProtocol(), "dubbo");
        assertEquals(url.getHost(), "127.0.0.1");
        assertEquals(url.getPort(), 7001);
        assertEquals(url.getServiceInterface(), MetadataService.class.getName());
        assertEquals(url.getGroup(), "test");
        assertEquals(url.getSide(), "consumer");
        assertEquals(url.getVersion(), "1.0.0");
//        assertEquals(url.getParameters().get("getAndListenInstanceMetadata.1.callback"), "true");
        assertEquals(url.getParameters().get("reconnect"), "false");
        assertEquals(url.getParameters().get("timeout"), "5000");
        assertEquals(url.getApplicationModel(), ApplicationModel.defaultModel());

        // test generateWithMetadata
        urls = builder.build(serviceInstance);
        assertEquals(1, urls.size());
        url = urls.get(0);
        assertEquals(url.getProtocol(), "rest");
        assertEquals(url.getHost(), "127.0.0.1");
        assertEquals(url.getPort(), 20880);
        assertEquals(url.getServiceInterface(), MetadataService.class.getName());
        assertEquals(url.getGroup(), "test");
        assertEquals(url.getSide(), "consumer");
        assertEquals(url.getVersion(), "1.0.0");
        assertEquals(url.getApplication(), "dubbo-provider-demo");
        assertEquals(url.getParameters().get("timeout"), "5000");
    }

}
