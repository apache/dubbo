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
package org.apache.dubbo.test.spring;

import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.registry.client.ServiceDiscoveryRegistryDirectory;
import org.apache.dubbo.registry.client.migration.ServiceDiscoveryMigrationInvoker;
import org.apache.dubbo.remoting.exchange.ExchangeClient;
import org.apache.dubbo.rpc.cluster.ClusterInvoker;
import org.apache.dubbo.rpc.listener.ListenerInvokerWrapper;
import org.apache.dubbo.rpc.protocol.dubbo.DubboInvoker;
import org.apache.dubbo.rpc.proxy.InvokerInvocationHandler;
import org.apache.dubbo.test.common.SysProps;
import org.apache.dubbo.test.common.api.DemoService;
import org.apache.dubbo.test.common.api.GreetingService;
import org.apache.dubbo.test.common.api.RestDemoService;
import org.apache.dubbo.test.spring.context.MockSpringInitCustomizer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.aop.target.AbstractLazyCreationTargetSource;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.apache.dubbo.common.constants.CommonConstants.SHUTDOWN_WAIT_KEY;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class SpringXmlConfigTest {

    @BeforeAll
    public static void beforeAll() {
        DubboBootstrap.reset();
    }

    @AfterAll
    public static void afterAll(){
        DubboBootstrap.reset();
    }

    @Test
    public void test() {
        SysProps.setProperty(SHUTDOWN_WAIT_KEY, "2000");
        ClassPathXmlApplicationContext applicationContext = null;
        try {
            applicationContext = new ClassPathXmlApplicationContext("/spring/dubbo-demo.xml");

            GreetingService greetingService = applicationContext.getBean("greetingService", GreetingService.class);
            String greeting = greetingService.hello();
            Assertions.assertEquals(greeting, "Greetings!");

            // get greetingService's ReferenceCountExchangeClient.
            ExchangeClient client = getDubboClient(greetingService);
            // replace ReferenceCountExchangeClient with LazyConnectExchangeClient by close().
            client.close();

            // wait close done.
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Assertions.fail();
            }
            Assertions.assertFalse(client.isClosed(), "client status close");

            // revive ReferenceCountExchangeClient
            greeting = greetingService.hello();
            Assertions.assertEquals(greeting, "Greetings!");

            DemoService demoService = applicationContext.getBean("demoService", DemoService.class);
            String sayHelloResult = demoService.sayHello("dubbo");
            Assertions.assertTrue(sayHelloResult.startsWith("Hello dubbo"), sayHelloResult);

            RestDemoService restDemoService = applicationContext.getBean("restDemoService", RestDemoService.class);
            String resetHelloResult = restDemoService.sayHello("dubbo");
            Assertions.assertEquals("Hello, dubbo", resetHelloResult);

            // check initialization customizer
            MockSpringInitCustomizer.checkCustomizer(applicationContext);
        } finally {
            SysProps.clear();
            if (applicationContext != null) {
                applicationContext.close();
            }
        }

    }

    private ExchangeClient getDubboClient(Object dubboService) {
        return getDubboClients(dubboService)[0];
    }

    @SuppressWarnings("rawtypes")
    private ExchangeClient[] getDubboClients(Object dubboService) {
        try {

            Method getTargetSourceMethod = dubboService.getClass().getDeclaredMethod("getTargetSource");
            getTargetSourceMethod.setAccessible(true);
            AbstractLazyCreationTargetSource targetSource = (AbstractLazyCreationTargetSource) getTargetSourceMethod.invoke(dubboService);
            Object lazyTarget = targetSource.getTarget();
            Field handlerField = lazyTarget.getClass().getDeclaredField("handler");
            handlerField.setAccessible(true);
            InvokerInvocationHandler handler = (InvokerInvocationHandler) handlerField.get(lazyTarget);
            Field invokerField = handler.getClass().getDeclaredField("invoker");
            invokerField.setAccessible(true);
            ServiceDiscoveryMigrationInvoker invoker = (ServiceDiscoveryMigrationInvoker) invokerField.get(handler);
            ClusterInvoker clusterInvoker = invoker.getCurrentAvailableInvoker();
            ServiceDiscoveryRegistryDirectory directory = (ServiceDiscoveryRegistryDirectory) clusterInvoker.getDirectory();
            ListenerInvokerWrapper wrapper = (ListenerInvokerWrapper) directory.getInvokers().get(0);            
            DubboInvoker dubboInvoker = (DubboInvoker) wrapper.getInvoker();
            Field clientsField = dubboInvoker.getClass().getDeclaredField("clients");
            clientsField.setAccessible(true);
            ExchangeClient[] clients = (ExchangeClient[]) clientsField.get(dubboInvoker);
            return clients;
        }  catch (Exception e) {
            e.printStackTrace();
            Assertions.fail(e.getMessage());
            throw new RuntimeException(e);
        }        
    }
}
