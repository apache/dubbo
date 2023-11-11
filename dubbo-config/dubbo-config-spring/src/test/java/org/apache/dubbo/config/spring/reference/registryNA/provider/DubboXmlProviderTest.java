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
package org.apache.dubbo.config.spring.reference.registryNA.provider;

import org.apache.dubbo.config.spring.api.HelloService;
import org.apache.dubbo.rpc.RpcException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author <a href = "mailto:kamtohung@gmail.com">KamTo Hung</a>
 */
public class DubboXmlProviderTest {

    @Test
    void testProvider() {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                "classpath:/org/apache/dubbo/config/spring/reference/registryNA/provider/dubbo-provider.xml");
        context.start();
        Object bean = context.getBean("helloService");
        Assertions.assertNotNull(bean);
    }

    @Test
    void testProvider2() {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                "classpath:/org/apache/dubbo/config/spring/reference/registryNA/provider/dubbo-provider.xml");
        context.start();
        Assertions.assertNotNull(context.getBean("helloService"));
        ClassPathXmlApplicationContext context2 = new ClassPathXmlApplicationContext(
                "classpath:/org/apache/dubbo/config/spring/reference/registryNA/consumer/dubbo-consumer.xml");
        context2.start();
        HelloService helloService = context2.getBean("helloService", HelloService.class);
        Assertions.assertNotNull(helloService);
        RpcException exception = Assertions.assertThrows(RpcException.class, () -> helloService.sayHello("dubbo"));
        Assertions.assertTrue(
                exception
                        .getMessage()
                        .contains(
                                "Failed to invoke the method sayHello in the service org.apache.dubbo.config.spring.api.HelloService. No provider available for the service org.apache.dubbo.config.spring.api.HelloService"));
    }
}
