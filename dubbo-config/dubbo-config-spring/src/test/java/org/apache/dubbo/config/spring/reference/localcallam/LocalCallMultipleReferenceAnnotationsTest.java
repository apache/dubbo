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
package org.apache.dubbo.config.spring.reference.localcallam;

import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.spring.ReferenceBean;
import org.apache.dubbo.config.spring.api.HelloService;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.apache.dubbo.rpc.RpcContext;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.net.InetSocketAddress;
import java.util.Map;

import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;

@EnableDubbo
@ExtendWith(SpringExtension.class)
@PropertySource("classpath:/org/apache/dubbo/config/spring/reference/localcallam/local-call-config.properties")
@ContextConfiguration(classes = {LocalCallMultipleReferenceAnnotationsTest.class, LocalCallMultipleReferenceAnnotationsTest.LocalCallConfiguration.class})
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
class LocalCallMultipleReferenceAnnotationsTest {

    @BeforeAll
    public static void setUp() {
        DubboBootstrap.reset();
    }

    @Autowired
    private HelloService helloService;

    @Autowired
    private HelloService demoHelloService;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void testLocalCall() {
        // see also: org.apache.dubbo.rpc.protocol.injvm.InjvmInvoker.doInvoke
        // InjvmInvoker set remote address to 127.0.0.1:0
        String result = helloService.sayHello("world");
        Assertions.assertEquals("Hello world, response from provider: " + InetSocketAddress.createUnresolved("127.0.0.1", 0), result);

        String demoResult = demoHelloService.sayHello("world");
        Assertions.assertEquals("Hello world, response from provider: " + InetSocketAddress.createUnresolved("127.0.0.1", 0), demoResult);

        Map<String, ReferenceBean> referenceBeanMap = applicationContext.getBeansOfType(ReferenceBean.class);
        Assertions.assertEquals(2, referenceBeanMap.size());
        Assertions.assertTrue(referenceBeanMap.containsKey("&helloService"));
        Assertions.assertTrue(referenceBeanMap.containsKey("&demoHelloService"));

        //helloService3 and demoHelloService share the same ReferenceConfig instance
        ReferenceBean helloService3ReferenceBean = applicationContext.getBean("&helloService3", ReferenceBean.class);
        ReferenceBean demoHelloServiceReferenceBean = applicationContext.getBean("&demoHelloService", ReferenceBean.class);
        Assertions.assertSame(helloService3ReferenceBean, demoHelloServiceReferenceBean);

    }

    @Configuration
    public static class LocalCallConfiguration {
        @DubboReference
        private HelloService helloService;

        @DubboReference(group = "demo", version = "2.0.0")
        private HelloService demoHelloService;

        @DubboReference(group = "${biz.group}", version = "${biz.version}")
        private HelloService helloService3;

    }

    @Configuration
    public static class LocalCallConfiguration2 {

        @DubboReference
        private HelloService helloService;

        @DubboReference(group = "${biz.group}", version = "2.0.0")
        private HelloService demoHelloService;

    }

    @Configuration
    public static class LocalCallConfiguration3 {

        @DubboReference(group = "foo")
        private HelloService demoHelloService;

    }

    @DubboService
    public static class AnotherLocalHelloServiceImpl implements HelloService {
        @Override
        public String sayHello(String name) {
            return "Hello " + name + ", response from provider: " + RpcContext.getContext().getLocalAddress();
        }
    }

    @DubboService(group = "demo", version = "2.0.0")
    public static class DemoHelloServiceImpl implements HelloService {
        @Override
        public String sayHello(String name) {
            return "Hello " + name + ", response from provider: " + RpcContext.getContext().getLocalAddress();
        }
    }
}