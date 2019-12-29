/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.dubbo.config.spring.context.annotation.consumer;

import org.apache.dubbo.config.spring.api.DemoService;
import org.apache.dubbo.config.spring.context.annotation.consumer.test.component.TestDemoServiceComponentAnnotationWithTrueinitFalse;
import org.apache.dubbo.config.spring.context.annotation.consumer.test.component.TestDemoServiceComponentAnnotationWithTrueinitTrue;
import org.apache.dubbo.config.spring.context.annotation.consumer.test.component.TestDemoServiceComponentAnnotationWithoutTrueinit;
import org.apache.dubbo.config.spring.context.annotation.consumer.test.component.TestDemoServiceComponentXmlWithTrueinitFalse;
import org.apache.dubbo.config.spring.context.annotation.provider.ProviderConfiguration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Map;

public class ConsumerConfigTrueinitTest {
    /**
     * 'dubbo.consumer.trueinit=true'
     *
     * Test true lazy: init util actually use, not when injected.
     * This could cut down the startup time in develop or test env.
     */
    @Test
    public void testConsumerConfigTrueinitFalse() {
        AnnotationConfigApplicationContext providerContext = null;
        AnnotationConfigApplicationContext consumerContext = null;
        try {
            providerContext = new AnnotationConfigApplicationContext();
            providerContext.register(ProviderConfiguration.class);
            providerContext.refresh();

            consumerContext = new AnnotationConfigApplicationContext();
            consumerContext.register(TestDemoServiceComponentAnnotationWithTrueinitFalse.class);
            consumerContext.refresh();

            TestDemoServiceComponentAnnotationWithTrueinitFalse testDemoServiceComponentAnnotationWithTrueinitFalse = consumerContext.getBean(TestDemoServiceComponentAnnotationWithTrueinitFalse.class);
            Map<String, DemoService> beansBefore = consumerContext.getBeansOfType(DemoService.class);
            Assertions.assertTrue(beansBefore.isEmpty());

            long begin = System.currentTimeMillis();
            String value = testDemoServiceComponentAnnotationWithTrueinitFalse.sayHello("Zhai");
            Assertions.assertEquals("Hello,Zhai", value);
            System.out.println("######## testConsumerConfigTrueinitFalse - sayHello() cost: " + (System.currentTimeMillis() - begin)
                + " ms, because initializing ReferenceBean");

            Map<String, DemoService> beansAfter = consumerContext.getBeansOfType(DemoService.class);
            Assertions.assertEquals(1, beansAfter.size());

            long beginAgain = System.currentTimeMillis();
            String valueAgain = testDemoServiceComponentAnnotationWithTrueinitFalse.sayHello("Zhai");
            Assertions.assertEquals("Hello,Zhai", valueAgain);
            System.out.println("######## testConsumerConfigTrueinitFalse - sayHello() again cost: " + (System.currentTimeMillis() - beginAgain)
                + " ms, because ReferenceBean has already init");
        }
        finally {
            if (providerContext != null) {
                providerContext.close();
            }
            if (consumerContext != null) {
                consumerContext.close();
            }
        }
    }

    /**
     * without 'dubbo.consumer.trueinit'
     */
    @Test
    public void testConsumerConfigTrueinitWithout() {
        AnnotationConfigApplicationContext providerContext = null;
        AnnotationConfigApplicationContext consumerContext = null;
        try {
            providerContext = new AnnotationConfigApplicationContext();
            providerContext.register(ProviderConfiguration.class);
            providerContext.refresh();

            consumerContext = new AnnotationConfigApplicationContext();
            consumerContext.register(TestDemoServiceComponentAnnotationWithoutTrueinit.class);
            consumerContext.refresh();

            TestDemoServiceComponentAnnotationWithoutTrueinit testDemoServiceComponentAnnotationWithoutTrueinit = consumerContext.getBean(TestDemoServiceComponentAnnotationWithoutTrueinit.class);
            Map<String, DemoService> beansBefore = consumerContext.getBeansOfType(DemoService.class);
            Assertions.assertEquals(1, beansBefore.size());

            long begin = System.currentTimeMillis();
            String value = testDemoServiceComponentAnnotationWithoutTrueinit.sayHello("Zhai");
            Assertions.assertEquals("Hello,Zhai", value);
            System.out.println("######## testConsumerConfigTrueinitWithout - sayHello() cost: " + (System.currentTimeMillis() - begin)
                + " ms, because ReferenceBean has already init");

            Map<String, DemoService> beansAfter = consumerContext.getBeansOfType(DemoService.class);
            Assertions.assertEquals(1, beansAfter.size());

            long beginAgain = System.currentTimeMillis();
            String valueAgain = testDemoServiceComponentAnnotationWithoutTrueinit.sayHello("Zhai");
            Assertions.assertEquals("Hello,Zhai", valueAgain);
            System.out.println("######## testConsumerConfigTrueinitWithout - sayHello() again cost: " + (System.currentTimeMillis() - beginAgain)
                + " ms, because ReferenceBean has already init");
        }
        finally {
            if (providerContext != null) {
                providerContext.close();
            }
            if (consumerContext != null) {
                consumerContext.close();
            }
        }
    }

    /**
     * 'dubbo.consumer.trueinit=true'
     */
    @Test
    public void testConsumerConfigTrueinitTrue() {
        AnnotationConfigApplicationContext providerContext = null;
        AnnotationConfigApplicationContext consumerContext = null;
        try {
            providerContext = new AnnotationConfigApplicationContext();
            providerContext.register(ProviderConfiguration.class);
            providerContext.refresh();

            consumerContext = new AnnotationConfigApplicationContext();
            consumerContext.register(TestDemoServiceComponentAnnotationWithTrueinitTrue.class);
            consumerContext.refresh();

            TestDemoServiceComponentAnnotationWithTrueinitTrue testDemoServiceComponentAnnotationWithTrueinitTrue = consumerContext.getBean(TestDemoServiceComponentAnnotationWithTrueinitTrue.class);
            Map<String, DemoService> beansBefore = consumerContext.getBeansOfType(DemoService.class);
            Assertions.assertEquals(1, beansBefore.size());

            long begin = System.currentTimeMillis();
            String value = testDemoServiceComponentAnnotationWithTrueinitTrue.sayHello("Zhai");
            Assertions.assertEquals("Hello,Zhai", value);
            System.out.println("######## testConsumerConfigTrueinitTrue - sayHello() cost: " + (System.currentTimeMillis() - begin)
                + " ms, because ReferenceBean has already init");

            Map<String, DemoService> beansAfter = consumerContext.getBeansOfType(DemoService.class);
            Assertions.assertEquals(1, beansAfter.size());

            long beginAgain = System.currentTimeMillis();
            String valueAgain = testDemoServiceComponentAnnotationWithTrueinitTrue.sayHello("Zhai");
            Assertions.assertEquals("Hello,Zhai", valueAgain);
            System.out.println("######## testConsumerConfigTrueinitTrue - sayHello() again cost: " + (System.currentTimeMillis() - beginAgain)
                + " ms, because ReferenceBean has already init");
        }
        finally {
            if (providerContext != null) {
                providerContext.close();
            }
            if (consumerContext != null) {
                consumerContext.close();
            }
        }
    }

    @Test
    public void testConsumerConfigXmlTrueinitFalse() {
        AnnotationConfigApplicationContext providerContext = null;
        AnnotationConfigApplicationContext consumerContext = null;
        try {
            providerContext = new AnnotationConfigApplicationContext();
            providerContext.register(ProviderConfiguration.class);
            providerContext.refresh();

            consumerContext = new AnnotationConfigApplicationContext();
            consumerContext.register(TestDemoServiceComponentXmlWithTrueinitFalse.class);
            consumerContext.refresh();

            // test demoService inject
            TestDemoServiceComponentXmlWithTrueinitFalse testDemoServiceComponentXmlWithTrueinitFalse = consumerContext.getBean(TestDemoServiceComponentXmlWithTrueinitFalse.class);
            Map<String, DemoService> beansBefore = consumerContext.getBeansOfType(DemoService.class);
            Assertions.assertTrue(beansBefore.isEmpty());

            long begin = System.currentTimeMillis();
            String value = testDemoServiceComponentXmlWithTrueinitFalse.sayName("Zhai");
            Assertions.assertEquals("Hello,Zhai", value);
            System.out.println("######## testConsumerConfigXmlTrueinitFalse - sayName() cost: " + (System.currentTimeMillis() - begin)
                + " ms, because initializing ReferenceBean");

            Map<String, DemoService> beansAfter = consumerContext.getBeansOfType(DemoService.class);
            Assertions.assertEquals(1, beansAfter.size());

            long beginAgain = System.currentTimeMillis();
            String valueAgain = testDemoServiceComponentXmlWithTrueinitFalse.sayName("Zhai");
            Assertions.assertEquals("Hello,Zhai", valueAgain);
            System.out.println("######## testConsumerConfigXmlTrueinitFalse - sayName() again cost: " + (System.currentTimeMillis() - beginAgain)
                + " ms, because ReferenceBean has already init");

            // test helloService inject
            Assertions.assertTrue(AopUtils.isAopProxy(testDemoServiceComponentXmlWithTrueinitFalse.getHelloService2()));

            long beginHello = System.currentTimeMillis();
            String valueHello = testDemoServiceComponentXmlWithTrueinitFalse.sayHello("Zhai");
            Assertions.assertEquals("Greeting, Zhai", valueHello);
            System.out.println("######## testConsumerConfigXmlTrueinitFalse - sayHello() cost: " + (System.currentTimeMillis() - beginHello)
                + " ms, because initializing ReferenceBean");

            long beginHelloAgain = System.currentTimeMillis();
            String valueHelloAgain = testDemoServiceComponentXmlWithTrueinitFalse.sayHello("Zhai");
            Assertions.assertEquals("Greeting, Zhai", valueHelloAgain);
            System.out.println("######## testConsumerConfigXmlTrueinitFalse - sayHello() again cost: " + (System.currentTimeMillis() - beginHelloAgain)
                + " ms, because ReferenceBean has already init");
        }
        finally {
            if (providerContext != null) {
                providerContext.close();
            }
            if (consumerContext != null) {
                consumerContext.close();
            }
        }
    }
}
