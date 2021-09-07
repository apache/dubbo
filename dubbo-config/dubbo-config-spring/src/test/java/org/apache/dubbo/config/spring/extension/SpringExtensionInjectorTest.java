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

import org.apache.dubbo.common.extension.ExtensionInjector;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.spring.api.DemoService;
import org.apache.dubbo.config.spring.api.HelloService;
import org.apache.dubbo.config.spring.impl.DemoServiceImpl;
import org.apache.dubbo.config.spring.impl.HelloServiceImpl;
import org.apache.dubbo.rpc.Protocol;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringExtensionInjectorTest {

    private SpringExtensionInjector springExtensionFactory = new SpringExtensionInjector();
    private AnnotationConfigApplicationContext context1;
    private AnnotationConfigApplicationContext context2;

    @BeforeEach
    public void init() {
        DubboBootstrap.reset();

        // init SpringExtensionInjector
        ExtensionLoader.getExtensionLoader(ExtensionInjector.class).getExtension("spring");

        context1 = new AnnotationConfigApplicationContext();
        context1.setDisplayName("Context1");
        context1.register(getClass());
        context1.refresh();
        context2 = new AnnotationConfigApplicationContext();
        context2.setDisplayName("Context2");
        context2.register(BeanForContext2.class);
        context2.refresh();
        SpringExtensionInjector.addApplicationContext(context1);
        SpringExtensionInjector.addApplicationContext(context2);
    }

    @AfterEach
    public void destroy() {
        context1.close();
        context2.close();
        SpringExtensionInjector.clearContexts();
    }

    @Test
    public void testGetExtensionBySPI() {
        Protocol protocol = springExtensionFactory.getInstance(Protocol.class, "protocol");
        Assertions.assertNull(protocol);
    }

    @Test
    public void testGetExtensionByName() {
        DemoService bean = springExtensionFactory.getInstance(DemoService.class, "bean1");
        Assertions.assertNotNull(bean);
        HelloService hello = springExtensionFactory.getInstance(HelloService.class, "hello");
        Assertions.assertNotNull(hello);
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
}
