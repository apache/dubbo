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
package com.alibaba.dubbo.config.spring.beans.factory.annotation.multiple.consumer;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.spring.api.HelloService;
import com.alibaba.dubbo.config.spring.context.annotation.EnableDubbo;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Multiple Protocols Test
 */

public class MultipleProtocolsServiceConsumer {

    @EnableDubbo
    @PropertySource({
            "classpath:/META-INF/multiple-protocols-consumer.properties",
            "classpath:/META-INF/dubbo-common.properties"
    })
    @Configuration
    static class ConsumerConfiguration {

        @Reference(version = "${hello.service.version}", protocol = "dubbo")
        private HelloService dubboHelloService;

        @Reference(version = "${hello.service.version}", protocol = "rest")
        private HelloService restHelloService;

//        @Bean
//        public ReferenceBean<HelloService> restReferenceBean(@Value("${hello.service.version}") String version) {
//            ReferenceBean<HelloService> referenceBean = new ReferenceBean<HelloService>();
//            referenceBean.setVersion(version);
//            referenceBean.setProtocol("rest");
//            referenceBean.setInterface(HelloService.class);
//            return referenceBean;
//        }

    }

//    @ImportResource("classpath:/META-INF/spring/dubbo-rest-consumer.xml")
//    @Configuration
//    static class ConsumerXMLConfiguration {
//    }


    public static void main(String[] args) {

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(ConsumerConfiguration.class);
        context.refresh();

        ConsumerConfiguration configuration = context.getBean(ConsumerConfiguration.class);
        System.out.println(configuration.dubboHelloService.sayHello("mercyblitz"));
        System.out.println(configuration.restHelloService.sayHello("mercyblitz"));

        context.close();
    }

}
