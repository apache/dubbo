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
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Test for https://github.com/apache/dubbo/issues/8698
 */
class ServiceInstanceHostPortCustomizerTest {
    private static ServiceInstanceHostPortCustomizer serviceInstanceHostPortCustomizer;
    
    @BeforeAll
    public static void setUp() {
        serviceInstanceHostPortCustomizer = new ServiceInstanceHostPortCustomizer();
    }

    @AfterAll
    public static void clearUp() {
        ApplicationModel.reset();
    }
    
    @Test
    void customizePreferredProtocol() throws ExecutionException, InterruptedException {
        ScopeBeanFactory beanFactory = mock(ScopeBeanFactory.class);
        MetadataService metadataService = mock(MetadataService.class);
        when(beanFactory.getBean(MetadataService.class)).thenReturn(metadataService);
        ApplicationModel applicationModel = spy(ApplicationModel.defaultModel());
        when(applicationModel.getBeanFactory()).thenReturn(beanFactory);

        // test protocol set
        ApplicationConfig applicationConfig = new ApplicationConfig("aa");
        // when(applicationModel.getCurrentConfig()).thenReturn(applicationConfig);
        doReturn(applicationConfig).when(applicationModel).getCurrentConfig();
        DefaultServiceInstance serviceInstance1 = new DefaultServiceInstance("without-preferredProtocol", applicationModel);
        MetadataInfo metadataInfo = new MetadataInfo();
        metadataInfo.addService(URL.valueOf("tri://127.1.1.1:50052/org.apache.dubbo.demo.GreetingService"));
        serviceInstance1.setServiceMetadata(metadataInfo);
        serviceInstanceHostPortCustomizer.customize(serviceInstance1, applicationModel);
        Assertions.assertEquals("127.1.1.1", serviceInstance1.getHost());
        Assertions.assertEquals(50052, serviceInstance1.getPort());

        // pick the preferredProtocol
        applicationConfig.setProtocol("tri");
        metadataInfo.addService(URL.valueOf("dubbo://127.1.2.3:20889/org.apache.dubbo.demo.HelloService"));
        serviceInstanceHostPortCustomizer.customize(serviceInstance1, applicationModel);
        Assertions.assertEquals("127.1.1.1", serviceInstance1.getHost());
        Assertions.assertEquals(50052, serviceInstance1.getPort());
    }
}
