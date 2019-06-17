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
package org.apache.dubbo.config;

import org.apache.dubbo.config.api.DemoService;

import java.io.IOException;

/**
 * Dubbo Consumer Bootstrap
 *
 * @since 2.7.3
 */
public class DubboConsumerBootstrap {

    public static void main(String[] args) throws IOException, InterruptedException {

        ApplicationConfig application = new ApplicationConfig();
        application.setName("dubbo-consumer-demo");

        // 连接注册中心配置
        RegistryConfig registry = new RegistryConfig();
        registry.setAddress("zookeeper://127.0.0.1:2181?registry-type=service");

        // 服务提供者协议配置
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setName("dubbo");
        protocol.setPort(12345);

        ReferenceConfig<DemoService> reference = new ReferenceConfig<DemoService>(); // 此实例很重，封装了与注册中心的连接以及与提供者的连接，请自行缓存，否则可能造成内存和连接泄漏
        reference.setApplication(application);
        reference.setRegistry(registry); // 多个注册中心可以用setRegistries()
        reference.setInterface(DemoService.class);
        reference.setVersion("1.0.0");
        reference.setProtocol("dubbo");
        reference.setCheck(false);

        // 和本地bean一样使用xxxService
        DemoService demoService1 = reference.get();

        for (int i = 0; i < 1000; i++) {
            System.out.println(demoService1.sayName("Hello,World"));
            Thread.sleep(1000);
        }

        System.in.read();
    }

}
