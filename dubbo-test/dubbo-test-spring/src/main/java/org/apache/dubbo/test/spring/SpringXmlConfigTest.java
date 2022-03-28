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
import org.apache.dubbo.test.common.SysProps;
import org.apache.dubbo.test.common.api.DemoService;
import org.apache.dubbo.test.common.api.GreetingService;
import org.apache.dubbo.test.common.api.RestDemoService;
import org.apache.dubbo.test.spring.context.MockSpringInitCustomizer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.apache.dubbo.common.constants.CommonConstants.SHUTDOWN_WAIT_KEY;

@DisabledForJreRange(min = JRE.JAVA_16)
public class SpringXmlConfigTest {

    private static ClassPathXmlApplicationContext providerContext;

    @BeforeAll
    public static void beforeAll() {
        DubboBootstrap.reset();
    }

    @AfterAll
    public static void afterAll(){
        DubboBootstrap.reset();
        providerContext.close();
    }

    private void startProvider() {
        providerContext = new ClassPathXmlApplicationContext("/spring/dubbo-demo-provider.xml");
    }

    @Test
    public void test() {
        SysProps.setProperty(SHUTDOWN_WAIT_KEY, "2000");
        // start provider context
        startProvider();
        // start consumer context
        ClassPathXmlApplicationContext applicationContext = null;
        try {
            applicationContext = new ClassPathXmlApplicationContext("/spring/dubbo-demo.xml");

            GreetingService greetingService = applicationContext.getBean("greetingService", GreetingService.class);
            String greeting = greetingService.hello();
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
}
