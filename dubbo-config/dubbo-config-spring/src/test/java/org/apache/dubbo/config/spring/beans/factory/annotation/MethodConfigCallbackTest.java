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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
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

    @DubboReference(check = false, async = true,
            parameters = { "refId", "ref-1" },
            methods = {@Method(name = "sayHello",
                    oninvoke = "methodCallback.oninvoke",
                    onreturn = "methodCallback.onreturn",
                    onthrow = "methodCallback.onthrow")})
    private HelloService helloServiceMethodCallBack;

    @DubboReference(check = false, async = true,
            parameters = { "refId", "ref-2" },
            methods = {@Method(name = "sayHello",
                    oninvoke = "methodCallback.oninvoke",
                    onreturn = "methodCallback.onreturn",
                    onthrow = "methodCallback.onthrow")})
    private HelloService helloServiceMethodCallBack2;

    @BeforeEach
    public void setUpEach() {
        MethodCallbackImpl.cnt.set(0);
        MethodCallbackImpl.cnt1.set(0);
        MethodCallbackImpl.cnt2.set(0);
    }

    @RepeatedTest(2)
    @Test
    public void testMethodAnnotationCallBack() {
        // the reason of creating new thread for each call:
        // rpcContext that is still using in callback process will be override at following invoke in the same thread.  
        new Thread(() -> {
            helloServiceMethodCallBack.sayHello("dubbo");
            helloServiceMethodCallBack2.sayHello("dubbo(2)");
        }).start();
        int i = 0;
        while (MethodCallbackImpl.cnt.get() < 2 && i < 50){
            // wait for async callback finished
            try {
                i++;
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }
        MethodCallback notify = (MethodCallback) context.getBean("methodCallback");
        Assertions.assertEquals("dubbo invoke success,dubbo invoke success(2)", notify.getOnInvoke());
        Assertions.assertEquals("dubbo return success,dubbo return success(2)", notify.getOnReturn());
    }

    @Configuration
    static class MethodCallbackConfiguration {

        @Bean("methodCallback")
        public MethodCallback methodCallback() {
            return new MethodCallbackImpl();
        }

    }
}