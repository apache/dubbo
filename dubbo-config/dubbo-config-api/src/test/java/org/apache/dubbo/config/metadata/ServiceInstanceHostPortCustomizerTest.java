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
package org.apache.dubbo.config.metadata;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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
    void customizePreferredProtocol() {
        ApplicationModel applicationModel= new ApplicationModel(new FrameworkModel());
        applicationModel.getApplicationConfigManager().setApplication(new ApplicationConfig("service-preferredProtocol"));
        
        WritableMetadataService writableMetadataService = WritableMetadataService.getDefaultExtension(applicationModel);
        
        // Only have tri protocol
        writableMetadataService.exportURL(
            URL.valueOf("tri://127.1.1.1:50052/org.apache.dubbo.demo.GreetingService")
        );
        
        // Trigger the fallback strategy
        ServiceInstance serviceInstance1 = new DefaultServiceInstance("without-preferredProtocol", applicationModel);
        serviceInstanceHostPortCustomizer.customize(serviceInstance1);
        Assertions.assertEquals("127.1.1.1", serviceInstance1.getHost());
        Assertions.assertEquals(50052, serviceInstance1.getPort());
        
        
        // Add the default protocol
        writableMetadataService.exportURL(
            URL.valueOf("dubbo://127.1.2.3:20889/org.apache.dubbo.demo.HelloService")
        );
        
        // pick the preferredProtocol
        ServiceInstance serviceInstance2 = new DefaultServiceInstance("with-preferredProtocol", applicationModel);
        serviceInstanceHostPortCustomizer.customize(serviceInstance2);
        Assertions.assertEquals("127.1.2.3", serviceInstance2.getHost());
        Assertions.assertEquals(20889, serviceInstance2.getPort());
    }
}
