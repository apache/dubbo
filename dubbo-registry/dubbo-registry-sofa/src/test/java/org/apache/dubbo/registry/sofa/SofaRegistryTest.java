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
package org.apache.dubbo.registry.sofa;

import com.alipay.sofa.registry.server.test.TestRegistryMain;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 */
public class SofaRegistryTest {

    public static final Logger LOGGER = LoggerFactory.getLogger(SofaRegistryTest.class);

    private static TestRegistryMain registryMain;

    private static ApplicationConfig applicationConfig = new ApplicationConfig("test-sofa-registry");

    private static ProtocolConfig protocolConfig1;

    private static ProtocolConfig protocolConfig2;

    private static RegistryConfig registryConfig;

    @BeforeAll
    public static void beforeClass() {

        protocolConfig1 = new ProtocolConfig();
        protocolConfig1.setName("dubbo");
        protocolConfig1.setPort(20890);

        protocolConfig2 = new ProtocolConfig();
        protocolConfig2.setName("dubbo");
        protocolConfig2.setPort(20891);

        registryConfig = new RegistryConfig();
        registryConfig.setAddress("sofa://127.0.0.1:9603");
        registryConfig.setProtocol("sofa");

        registryMain = new TestRegistryMain();
        try {
            registryMain.startRegistry();
        } catch (Exception e) {
            LOGGER.error("start test sofa registry error!", e);
        }
    }

    @Test
    public void testPubAndSub() throws InterruptedException {

        ServiceConfig<HelloService> serviceConfig1 = new ServiceConfig<>();
        serviceConfig1.setInterface(HelloService.class);
        serviceConfig1.setRef(new HelloServiceImpl("rrr11"));
        serviceConfig1.setProtocol(protocolConfig1);
        serviceConfig1.setRegistry(registryConfig);
        serviceConfig1.setGroup("g1");
        serviceConfig1.setApplication(applicationConfig);
        serviceConfig1.export();

        ServiceConfig<HelloService> serviceConfig2 = new ServiceConfig<>();
        serviceConfig2.setInterface(HelloService.class);
        serviceConfig2.setRef(new HelloServiceImpl("rrr22"));
        serviceConfig2.setProtocol(protocolConfig1);
        serviceConfig2.setRegistry(registryConfig);
        serviceConfig2.setGroup("g2");
        serviceConfig2.setApplication(applicationConfig);
        serviceConfig2.setRegister(false);
        serviceConfig2.export();

        Thread.sleep(1000);

        // do refer
        ReferenceConfig<HelloService> referenceConfig1 = new ReferenceConfig<>();
        referenceConfig1.setInterface(HelloService.class);
        referenceConfig1.setProtocol("dubbo");
        referenceConfig1.setInjvm(false);
        referenceConfig1.setGroup("g1");
        referenceConfig1.setRegistry(registryConfig);
        referenceConfig1.setApplication(applicationConfig);
        HelloService service = referenceConfig1.get();
        Assertions.assertEquals("rrr11", service.sayHello("xxx"));

        // do refer duplicated
        ReferenceConfig<HelloService> referenceConfig2 = new ReferenceConfig<>();
        referenceConfig2.setInterface(HelloService.class);
        referenceConfig2.setProtocol("dubbo");
        referenceConfig2.setInjvm(false);
        referenceConfig2.setGroup("g1");
        referenceConfig2.setRegistry(registryConfig);
        referenceConfig2.setApplication(applicationConfig);
        HelloService service2 = referenceConfig2.get();
        Assertions.assertEquals("rrr11", service2.sayHello("xxx"));

        // export one service
        ServiceConfig<HelloService> serviceConfig3 = new ServiceConfig<>();
        serviceConfig3.setInterface(HelloService.class);
        serviceConfig3.setRef(new HelloServiceImpl("rrr12"));
        serviceConfig3.setProtocol(protocolConfig2);
        serviceConfig3.setRegistry(registryConfig);
        serviceConfig3.setGroup("g1");
        serviceConfig3.setApplication(applicationConfig);
        serviceConfig3.export();
        Assertions.assertTrue(service2.sayHello("xxx").startsWith("rrr1"));

        // unrefer
        referenceConfig1.destroy();
        Assertions.assertTrue(service2.sayHello("xxx").startsWith("rrr1"));

        // unexport one service
        serviceConfig1.unexport();
        Thread.sleep(2000);
        Assertions.assertTrue(service2.sayHello("xxx").startsWith("rrr1"));
    }

    @AfterAll
    public static void afterClass() {
        try {
            registryMain.stopRegistry();
            protocolConfig1.destroy();
        } catch (Exception e) {
            LOGGER.error("Stop test sofa registry error!", e);
        }
    }

}