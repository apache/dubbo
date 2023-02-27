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
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.constants.CommonConstants.SIDE_KEY;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

class ServiceInstanceMetadataCustomizerTest {
    private static ServiceInstanceMetadataCustomizer serviceInstanceMetadataCustomizer;

    @BeforeAll
    public static void setUp() {
        serviceInstanceMetadataCustomizer = new ServiceInstanceMetadataCustomizer();
    }

    @AfterAll
    public static void clearUp() {
        ApplicationModel.reset();
    }


    /**
     * Only 'include' policy spicified in Customized Filter will take effect
     */
    @Test
    void testCustomizeWithIncludeFilters() {
        ApplicationModel applicationModel = spy(ApplicationModel.defaultModel());
        ApplicationConfig applicationConfig = new ApplicationConfig("aa");
        doReturn(applicationConfig).when(applicationModel).getCurrentConfig();

        DefaultServiceInstance serviceInstance1 = new DefaultServiceInstance("ServiceInstanceMetadataCustomizerTest", applicationModel);
        MetadataInfo metadataInfo = new MetadataInfo();
        metadataInfo.addService(URL.valueOf("tri://127.1.1.1:50052/org.apache.dubbo.demo.GreetingService?application=ServiceInstanceMetadataCustomizerTest&env=test&side=provider&group=test"));
        serviceInstance1.setServiceMetadata(metadataInfo);
        serviceInstanceMetadataCustomizer.customize(serviceInstance1, applicationModel);
        Assertions.assertEquals(1, serviceInstance1.getMetadata().size());
        Assertions.assertEquals("provider", serviceInstance1.getMetadata(SIDE_KEY));
        Assertions.assertNull( serviceInstance1.getMetadata("env"));
        Assertions.assertNull( serviceInstance1.getMetadata("application"));
    }

    /**
     * Only 'exclude' policies specified in Exclude Filters will take effect
     */
    @Test
    void testCustomizeWithExcludeFilters() {
        ApplicationModel applicationModel = spy(ApplicationModel.defaultModel());
        ApplicationConfig applicationConfig = new ApplicationConfig("aa");
        doReturn(applicationConfig).when(applicationModel).getCurrentConfig();

        DefaultServiceInstance serviceInstance1 = new DefaultServiceInstance("ServiceInstanceMetadataCustomizerTest", applicationModel);
        MetadataInfo metadataInfo = new MetadataInfo();
        metadataInfo.addService(URL.valueOf("tri://127.1.1.1:50052/org.apache.dubbo.demo.GreetingService?application=ServiceInstanceMetadataCustomizerTest&env=test&side=provider&group=test&params-filter=-customized,-dubbo"));
        serviceInstance1.setServiceMetadata(metadataInfo);
        serviceInstanceMetadataCustomizer.customize(serviceInstance1, applicationModel);
        Assertions.assertEquals(2, serviceInstance1.getMetadata().size());
        Assertions.assertEquals("ServiceInstanceMetadataCustomizerTest", serviceInstance1.getMetadata("application"));
        Assertions.assertEquals("test", serviceInstance1.getMetadata("env"));

        Assertions.assertNull( serviceInstance1.getMetadata("side"));
        Assertions.assertNull( serviceInstance1.getMetadata("group"));
    }
}
