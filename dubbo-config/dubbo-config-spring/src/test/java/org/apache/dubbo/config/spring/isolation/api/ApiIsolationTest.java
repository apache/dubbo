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
package org.apache.dubbo.config.spring.isolation.api;

import org.apache.dubbo.common.threadlocal.NamedInternalThreadFactory;
import org.apache.dubbo.common.threadpool.manager.ExecutorRepository;
import org.apache.dubbo.common.threadpool.manager.IsolationExecutorRepository;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.spring.api.DemoService;
import org.apache.dubbo.config.spring.api.HelloService;
import org.apache.dubbo.config.spring.impl.DemoServiceImpl;
import org.apache.dubbo.config.spring.impl.HelloServiceImpl;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.test.check.registrycenter.config.ZookeeperRegistryCenterConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.dubbo.common.constants.CommonConstants.EXECUTOR_MANAGEMENT_MODE_ISOLATION;

public class ApiIsolationTest {
    private static RegistryConfig registryConfig;

    @BeforeAll
    public static void beforeAll() {
        FrameworkModel.destroyAll();
        registryConfig = new RegistryConfig(ZookeeperRegistryCenterConfig.getConnectionAddress1());
    }

    @AfterAll
    public static void afterAll() throws Exception {
        FrameworkModel.destroyAll();
    }

    private String version1 = "1.0";
    private String version2 = "2.0";
    private String version3 = "3.0";


    @Test
    public void test() throws Exception {


        DubboBootstrap providerBootstrap = null;
        DubboBootstrap consumerBootstrap1 = null;
        DubboBootstrap consumerBootstrap2 = null;

        try {

            // provider app
            providerBootstrap = DubboBootstrap.newInstance();

            ServiceConfig serviceConfig1 = new ServiceConfig();
            serviceConfig1.setInterface(DemoService.class);
            serviceConfig1.setRef(new DemoServiceImpl());
            serviceConfig1.setVersion(version1);
            // set executor1 for serviceConfig1, max threads is 10
            NamedThreadFactory threadFactory1 = new NamedThreadFactory("DemoService-executor");
            ExecutorService executor1 = Executors.newFixedThreadPool(10, threadFactory1);
            serviceConfig1.setExecutor(executor1);

            ServiceConfig serviceConfig2 = new ServiceConfig();
            serviceConfig2.setInterface(HelloService.class);
            serviceConfig2.setRef(new HelloServiceImpl());
            serviceConfig2.setVersion(version2);
            // set executor2 for serviceConfig2, max threads is 100
            NamedThreadFactory threadFactory2 = new NamedThreadFactory("HelloService-executor");
            ExecutorService executor2 = Executors.newFixedThreadPool(100, threadFactory2);
            serviceConfig2.setExecutor(executor2);

            ServiceConfig serviceConfig3 = new ServiceConfig();
            serviceConfig3.setInterface(HelloService.class);
            serviceConfig3.setRef(new HelloServiceImpl());
            serviceConfig3.setVersion(version3);
            // Because executor is not set for serviceConfig3, the default executor of serviceConfig3 is built using
            // the threadpool parameter of the protocolConfig ( FixedThreadpool , max threads is 200)
            serviceConfig3.setExecutor(null);

            // It takes effect only if [executor-management-mode=isolation] is configured
            ApplicationConfig applicationConfig = new ApplicationConfig("provider-app");
            applicationConfig.setExecutorManagementMode(EXECUTOR_MANAGEMENT_MODE_ISOLATION);

            providerBootstrap
                .application(applicationConfig)
                .registry(registryConfig)
                // export with tri and dubbo protocol
                .protocol(new ProtocolConfig("tri", 20001))
                .protocol(new ProtocolConfig("dubbo", 20002))
                .service(serviceConfig1)
                .service(serviceConfig2)
                .service(serviceConfig3);

            providerBootstrap.start();

            // Verify that the executor is the previously configured
            ApplicationModel applicationModel = providerBootstrap.getApplicationModel();
            ExecutorRepository repository = ExecutorRepository.getInstance(applicationModel);
            Assertions.assertTrue(repository instanceof IsolationExecutorRepository);
            Assertions.assertEquals(executor1, repository.getExecutor(serviceConfig1.toUrl()));
            Assertions.assertEquals(executor2, repository.getExecutor(serviceConfig2.toUrl()));
            // the default executor of serviceConfig3 is built using the threadpool parameter of the protocol
            ThreadPoolExecutor executor3 = (ThreadPoolExecutor) repository.getExecutor(serviceConfig3.toUrl());
            Assertions.assertTrue(executor3.getThreadFactory() instanceof NamedInternalThreadFactory);
            NamedInternalThreadFactory threadFactory3 = (NamedInternalThreadFactory) executor3.getThreadFactory();

            // consumer app start with dubbo protocol and rpc call
            consumerBootstrap1 = configConsumerBootstrapWithProtocol("dubbo");
            rpcInvoke(consumerBootstrap1);

            // consumer app start with tri protocol and rpc call
            consumerBootstrap2 = configConsumerBootstrapWithProtocol("tri");
            rpcInvoke(consumerBootstrap2);

            // Verify that when the provider accepts different service requests,
            // whether to use the respective executor(threadFactory) of different services to create threads
            AtomicInteger threadNum1 = threadFactory1.getThreadNum();
            AtomicInteger threadNum2 = threadFactory2.getThreadNum();
            AtomicInteger threadNum3 = threadFactory3.getThreadNum();
            Assertions.assertEquals(threadNum1.get(), 11);
            Assertions.assertEquals(threadNum2.get(), 101);
            Assertions.assertEquals(threadNum3.get(), 201);

        } finally {
            if (providerBootstrap != null) {
                providerBootstrap.destroy();
            }
            if (consumerBootstrap1 != null) {
                consumerBootstrap1.destroy();
            }
            if (consumerBootstrap2 != null) {
                consumerBootstrap2.destroy();
            }
        }
    }

    private void rpcInvoke(DubboBootstrap consumerBootstrap) {
        DemoService demoServiceV1 = consumerBootstrap.getCache().get(DemoService.class.getName() + ":" + version1);
        HelloService helloServiceV2 = consumerBootstrap.getCache().get(HelloService.class.getName() + ":" + version2);
        HelloService helloServiceV3 = consumerBootstrap.getCache().get(HelloService.class.getName() + ":" + version3);
        for (int i = 0; i < 250; i++) {
            demoServiceV1.sayName("name, version = " + version1);
        }
        for (int i = 0; i < 250; i++) {
            helloServiceV2.sayHello("hello, version = " + version2);
        }
        for (int i = 0; i < 250; i++) {
            helloServiceV3.sayHello("hello, version = " + version3);
        }
    }

    private DubboBootstrap configConsumerBootstrapWithProtocol(String protocol) {
        DubboBootstrap consumerBootstrap;
        consumerBootstrap = DubboBootstrap.newInstance();
        consumerBootstrap.application("consumer-app")
            .registry(registryConfig)
            .reference(builder -> builder.interfaceClass(DemoService.class).version(version1).protocol(protocol).injvm(false))
            .reference(builder -> builder.interfaceClass(HelloService.class).version(version2).protocol(protocol).injvm(false))
            .reference(builder -> builder.interfaceClass(HelloService.class).version(version3).protocol(protocol).injvm(false));
        consumerBootstrap.start();
        return consumerBootstrap;
    }

}
