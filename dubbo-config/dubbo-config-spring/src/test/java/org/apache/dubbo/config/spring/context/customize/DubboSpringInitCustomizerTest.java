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
package org.apache.dubbo.config.spring.context.customize;

import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.spring.SysProps;
import org.apache.dubbo.config.spring.api.HelloService;
import org.apache.dubbo.config.spring.context.DubboSpringInitCustomizerHolder;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.test.check.registrycenter.config.ZookeeperRegistryCenterConfig;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

class DubboSpringInitCustomizerTest {

    @BeforeAll
    public static void beforeAll() {
        DubboBootstrap.reset();
        SysProps.setProperty("dubbo.registry.address", ZookeeperRegistryCenterConfig.getConnectionAddress());

    }

    @AfterAll
    public static void afterAll() {
        DubboBootstrap.reset();
        SysProps.clear();
    }

    @Test
    void testReloadSpringContext() {

        ClassPathXmlApplicationContext providerContext1 = null;
        ClassPathXmlApplicationContext providerContext2 = null;

        ApplicationModel applicationModel = new FrameworkModel().newApplication();
        applicationModel.getDefaultModule();

        try {
            // start spring context 1
            ModuleModel moduleModel1 = applicationModel.newModule();
            DubboSpringInitCustomizerHolder.get().addCustomizer(context -> {
                context.setModuleModel(moduleModel1);
            });

            providerContext1 = new ClassPathXmlApplicationContext("dubbo-provider-v1.xml", getClass());
            ModuleModel moduleModelFromSpring1 = providerContext1.getBean(ModuleModel.class);
            Assertions.assertSame(moduleModel1, moduleModelFromSpring1);
            String serviceKey1 = HelloService.class.getName() + ":1.0.0";
            ServiceDescriptor serviceDescriptor1 = moduleModelFromSpring1.getServiceRepository().lookupService(serviceKey1);
            Assertions.assertNotNull(serviceDescriptor1);

            // close spring context 1
            providerContext1.close();
            Assertions.assertTrue(moduleModel1.isDestroyed());
            Assertions.assertFalse(moduleModel1.getApplicationModel().isDestroyed());
            providerContext1 = null;

            ModuleModel moduleModel2 = applicationModel.newModule();
            DubboSpringInitCustomizerHolder.get().addCustomizer(context -> {
                context.setModuleModel(moduleModel2);
            });

            // load spring context 2
            providerContext2 = new ClassPathXmlApplicationContext("dubbo-provider-v2.xml", getClass());
            ModuleModel moduleModelFromSpring2 = providerContext2.getBean(ModuleModel.class);
            Assertions.assertSame(moduleModel2, moduleModelFromSpring2);
            Assertions.assertNotSame(moduleModelFromSpring1, moduleModelFromSpring2);
            String serviceKey2 = HelloService.class.getName() + ":2.0.0";
            ServiceDescriptor serviceDescriptor2 = moduleModelFromSpring2.getServiceRepository().lookupService(serviceKey2);
            Assertions.assertNotNull(serviceDescriptor2);
            Assertions.assertNotSame(serviceDescriptor1, serviceDescriptor2);

            providerContext2.close();
            providerContext2 = null;
        }finally {
            if (providerContext1 != null) {
                providerContext1.close();
            }
            if (providerContext2 != null) {
                providerContext2.close();
            }
            applicationModel.destroy();
        }
    }


}
