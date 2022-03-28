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

import org.junit.jupiter.api.AfterAll;
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
    "dubbo.protocol.port=-1",
    "dubbo.registry.address=${zookeeper.connection.address}"
})
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class MethodConfigCallbackTest {

    @BeforeAll
    public static void beforeAll() {
        DubboBootstrap.reset();
    }

    @AfterAll
    public static void afterAll() {
        DubboBootstrap.reset();
    }

    @Autowired
    private ConfigurableApplicationContext context;

    @DubboReference(check = false, async = true,
        injvm = false, // Currently local call is not supported method callback cause by Injvm protocol is not supported ClusterFilter
        methods = {@Method(name = "sayHello",
        oninvoke = "methodCallback.oninvoke1",
        onreturn = "methodCallback.onreturn1",
        onthrow = "methodCallback.onthrow1")})
    private HelloService helloServiceMethodCallBack;

    @DubboReference(check = false, async = true,
            injvm = false, // Currently local call is not supported method callback cause by Injvm protocol is not supported ClusterFilter
            methods = {@Method(name = "sayHello",
            oninvoke = "methodCallback.oninvoke2",
            onreturn = "methodCallback.onreturn2",
            onthrow = "methodCallback.onthrow2")})
    private HelloService helloServiceMethodCallBack2;

    @Test
    public void testMethodAnnotationCallBack() {
        int threadCnt = Math.min(4, Runtime.getRuntime().availableProcessors());
        int callCnt = 2 * threadCnt;
        for (int i = 0; i < threadCnt; i++) {
            new Thread(() -> {
                for (int j = 0; j < callCnt; j++) {
                    helloServiceMethodCallBack.sayHello("dubbo");
                    helloServiceMethodCallBack2.sayHello("dubbo(2)");
                }
            }).start();
        }
        int i = 0;
        while (MethodCallbackImpl.cnt.get() < ( 2 * threadCnt * callCnt)){
            // wait for async callback finished
            try {
                i++;
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }
        MethodCallback notify = (MethodCallback) context.getBean("methodCallback");
        StringBuilder invoke1Builder = new StringBuilder();
        StringBuilder invoke2Builder = new StringBuilder();
        StringBuilder return1Builder = new StringBuilder();
        StringBuilder return2Builder = new StringBuilder();
        for (i = 0; i < threadCnt * callCnt; i++) {
            invoke1Builder.append("dubbo invoke success!");
            invoke2Builder.append("dubbo invoke success(2)!");
            return1Builder.append("dubbo return success!");
            return2Builder.append("dubbo return success(2)!");
        }
        Assertions.assertEquals(invoke1Builder.toString(), notify.getOnInvoke1());
        Assertions.assertEquals(return1Builder.toString(), notify.getOnReturn1());
        Assertions.assertEquals(invoke2Builder.toString(), notify.getOnInvoke2());
        Assertions.assertEquals(return2Builder.toString(), notify.getOnReturn2());
    }

    @Configuration
    static class MethodCallbackConfiguration {

        @Bean("methodCallback")
        public MethodCallback methodCallback() {
            return new MethodCallbackImpl();
        }

    }
}
