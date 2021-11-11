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
package org.apache.dubbo.config.spring.registry.nacos.demo.consumer;

import org.apache.dubbo.config.spring.registry.nacos.demo.service.DemoService;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.IOException;

/**
 * {@link DemoService} consumer demo XML bootstrap
 */
public class DemoServiceConsumerXmlBootstrap {

    public static void main(String[] args) throws IOException {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext();
        context.setConfigLocation("/META-INF/spring/dubbo-nacos-consumer-context.xml");
        context.refresh();
        System.out.println("DemoService consumer (XML) is starting...");

        for (int i = 1; i <= 5; i++) {
            DemoService demoService = context.getBean("demoService" + i, DemoService.class);
            for (int j = 0; j < 10; j++) {
                System.out.println(demoService.sayName("小马哥（mercyblitz）"));
            }
        }

        System.in.read();
        context.close();
    }
}
