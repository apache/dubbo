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
package org.apache.dubbo.config.spring.issues.issue9172;

import org.apache.dubbo.config.ReferenceConfigBase;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.config.context.ModuleConfigManager;
import org.apache.dubbo.config.spring.api.DemoService;
import org.apache.dubbo.config.spring.api.HelloService;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.apache.dubbo.config.spring.impl.DemoServiceImpl;
import org.apache.dubbo.config.spring.impl.HelloServiceImpl;
import org.apache.dubbo.config.spring.util.DubboBeanUtils;
import org.apache.dubbo.rpc.model.ModuleModel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;

/**
 * Test for issue 9172
 */
public class MultipleConsumerAndProviderTest {

    @Test
    public void test() {

        AnnotationConfigApplicationContext providerContext = null;
        AnnotationConfigApplicationContext consumerContext = null;

        try {
            providerContext = new AnnotationConfigApplicationContext(ProviderConfiguration.class);
            consumerContext = new AnnotationConfigApplicationContext(ConsumerConfiguration.class);

            ModuleModel consumerModuleModel = DubboBeanUtils.getModuleModel(consumerContext);
            ModuleConfigManager consumerConfigManager = consumerModuleModel.getConfigManager();
            ReferenceConfigBase helloServiceOneConfig = consumerConfigManager.getReference("helloServiceOne");
            ReferenceConfigBase demoServiceTwoConfig = consumerConfigManager.getReference("demoServiceTwo");
            Assertions.assertEquals(consumerConfigManager.getConsumer("consumer-one").get(), helloServiceOneConfig.getConsumer());
            Assertions.assertEquals(consumerConfigManager.getConsumer("consumer-two").get(), demoServiceTwoConfig.getConsumer());
            Assertions.assertEquals(consumerConfigManager.getRegistry("registry-one").get(), helloServiceOneConfig.getRegistry());
            Assertions.assertEquals(consumerConfigManager.getRegistry("registry-two").get(), demoServiceTwoConfig.getRegistry());

            HelloService helloServiceOne = consumerContext.getBean("helloServiceOne", HelloService.class);
            DemoService demoServiceTwo = consumerContext.getBean("demoServiceTwo", DemoService.class);
            String sayHello = helloServiceOne.sayHello("dubbo");
            String sayName = demoServiceTwo.sayName("dubbo");
            Assertions.assertEquals("Hello, dubbo", sayHello);
            Assertions.assertEquals("say:dubbo", sayName);
        } finally {
            if (providerContext != null) {
                providerContext.close();
            }
            if (consumerContext != null) {
                consumerContext.close();
            }
        }
    }


    @EnableDubbo(scanBasePackages = "")
    @PropertySource("classpath:/META-INF/issues/issue9172/consumer.properties")
    static class ConsumerConfiguration {

        @DubboReference(consumer = "consumer-one")
        private HelloService helloServiceOne;

        @DubboReference(consumer = "consumer-two")
        private DemoService demoServiceTwo;

    }

    @EnableDubbo(scanBasePackages = "")
    @PropertySource("classpath:/META-INF/issues/issue9172/provider.properties")
    static class ProviderConfiguration {

        @Bean
        @DubboService(provider = "provider-one")
        public HelloService helloServiceOne() {
            return new HelloServiceImpl();
        }

        @Bean
        @DubboService(provider = "provider-two")
        public DemoService demoServiceTwo() {
            return new DemoServiceImpl();
        }
    }
}
