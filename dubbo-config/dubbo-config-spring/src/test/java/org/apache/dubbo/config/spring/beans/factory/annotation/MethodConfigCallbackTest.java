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
package org.apache.dubbo.config.spring.beans.factory.annotation;

import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.Method;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.spring.api.HelloService;
import org.apache.dubbo.config.spring.api.MethodCallback;
import org.apache.dubbo.config.spring.context.annotation.provider.ProviderConfiguration;
import org.apache.dubbo.config.spring.impl.MethodCallbackImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(
        classes = {
                ProviderConfiguration.class,
                MethodConfigCallbackTest.class,
                MethodConfigCallbackTest.MethodCallbackConfiguration.class
        })
@TestPropertySource(properties = {
        "dubbo.protocols.dubbo.port=-1",
        //"dubbo.registries.my-registry.address=zookeeper://127.0.0.1:2181"
})
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class MethodConfigCallbackTest {

    @BeforeAll
    public static void setUp() {
        DubboBootstrap.reset();
    }

    @Autowired
    private ConfigurableApplicationContext context;

    @DubboReference(check = false,
            methods = {@Method(name = "sayHello",
                    oninvoke = "methodCallback.oninvoke",
                    onreturn = "methodCallback.onreturn",
                    onthrow = "methodCallback.onthrow")})
    private HelloService helloServiceMethodCallBack;

    @Test
    public void testMethodAnnotationCallBack() {
        helloServiceMethodCallBack.sayHello("dubbo");
        MethodCallback notify = (MethodCallback) context.getBean("methodCallback");
        Assertions.assertEquals("dubbo invoke success", notify.getOnInvoke());
        Assertions.assertEquals("dubbo return success", notify.getOnReturn());
    }

    @Configuration
    static class MethodCallbackConfiguration {

        @Bean("methodCallback")
        public MethodCallback methodCallback() {
            return new MethodCallbackImpl();
        }

    }
}
