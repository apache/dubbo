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
package org.apache.dubbo.config.spring.samples;

import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.spring.api.DemoService;
import org.apache.dubbo.config.spring.context.annotation.EnableDubboConfig;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.PropertySource;

/**
 * Zookeeper Dubbo Spring Provider Bootstrap
 *
 * @since 2.7.8
 */
@EnableDubboConfig
@PropertySource("classpath:/META-INF/service-introspection/zookeeper-dubbb-consumer.properties")
public class ZookeeperDubboSpringConsumerBootstrap {

    @DubboReference(services = "${dubbo.provider.name},${dubbo.provider.name1},${dubbo.provider.name2}")
    private DemoService demoService;

    public static void main(String[] args) throws Exception {
        Class<?> beanType = ZookeeperDubboSpringConsumerBootstrap.class;
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(beanType);

        ZookeeperDubboSpringConsumerBootstrap bootstrap = context.getBean(ZookeeperDubboSpringConsumerBootstrap.class);

        for (int i = 0; i < 100; i++) {
            System.out.println(bootstrap.demoService.sayName("Hello"));
            Thread.sleep(1000L);
        }

        System.in.read();

        context.close();
    }
}
