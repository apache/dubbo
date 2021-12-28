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
package org.apache.dubbo.config.spring.reference.localcall;

import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.spring.api.HelloService;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.net.InetSocketAddress;

import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:/org/apache/dubbo/config/spring/reference/localcall/local-call-provider.xml"})
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
public class LocalCallTest2 {

    @BeforeAll
    public static void setUp() {
        DubboBootstrap.reset();
    }

    @AfterAll
    public static void tearDown() {
        DubboBootstrap.reset();
    }

    @DubboReference
    private HelloService helloService;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    public void testLocalCall() {
        // see also: org.apache.dubbo.rpc.protocol.injvm.InjvmInvoker.doInvoke
        // InjvmInvoker set remote address to 127.0.0.1:0
        String result = helloService.sayHello("world");
        Assertions.assertEquals("Hello world, response from provider: " + InetSocketAddress.createUnresolved("127.0.0.1", 0), result);
    }

}
