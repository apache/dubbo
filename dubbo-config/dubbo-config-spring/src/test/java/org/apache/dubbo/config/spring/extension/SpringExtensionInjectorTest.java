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
package org.apache.dubbo.config.spring.extension;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.spring.api.DemoService;
import org.apache.dubbo.config.spring.api.HelloService;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.apache.dubbo.config.spring.impl.DemoServiceImpl;
import org.apache.dubbo.config.spring.impl.HelloServiceImpl;
import org.apache.dubbo.config.spring.util.DubboBeanUtils;
import org.apache.dubbo.rpc.Protocol;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableDubbo(scanBasePackages = "")
@Configuration
public class SpringExtensionInjectorTest {


    @BeforeEach
    public void init() {
        DubboBootstrap.reset();
    }

    @AfterEach
    public void destroy() {
    }

    @Test
    public void testSpringInjector() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        try {
            context.setDisplayName("Context1");
            context.register(getClass());
            context.refresh();

            SpringExtensionInjector springExtensionInjector = SpringExtensionInjector.get(DubboBeanUtils.getApplicationModel(context));
            Protocol protocol = springExtensionInjector.getInstance(Protocol.class, "protocol");
            Assertions.assertNull(protocol);

            DemoService bean = springExtensionInjector.getInstance(DemoService.class, "bean1");
            Assertions.assertNotNull(bean);
            HelloService hello = springExtensionInjector.getInstance(HelloService.class, "hello");
            Assertions.assertNotNull(hello);
        } finally {
            context.close();
        }
    }

    @Bean("bean1")
    public DemoService bean1() {
        return new DemoServiceImpl();
    }

    @Bean("bean2")
    public DemoService bean2() {
        return new DemoServiceImpl();
    }

    @Bean("hello")
    public HelloService helloService() {
        return new HelloServiceImpl();
    }

    @Bean
    public ApplicationConfig applicationConfig() {
        return new ApplicationConfig("test-app");
    }
}
