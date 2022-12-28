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
package org.apache.dubbo.config.spring.boot.importxml2;

import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.spring.ReferenceBean;
import org.apache.dubbo.config.spring.api.HelloService;
import org.apache.dubbo.config.spring.context.annotation.DubboComponentScan;
import org.apache.dubbo.config.spring.reference.ReferenceBeanManager;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

import java.util.Map;

@SpringBootTest(properties = {
        //"dubbo.scan.base-packages=org.apache.dubbo.config.spring.boot.importxml2",
        "dubbo.registry.address=N/A",
        "myapp.dubbo.port=20881",
        "myapp.name=dubbo-provider",
        "myapp.group=test"
}, classes = SpringBootImportAndScanTest.class)
@Configuration
@ComponentScan
@DubboComponentScan
@ImportResource("classpath:/org/apache/dubbo/config/spring/boot/importxml2/dubbo-provider.xml")
class SpringBootImportAndScanTest implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    @BeforeAll
    public static void setUp() {
        DubboBootstrap.reset();
    }

    @AfterAll
    public static void tearDown() {
        DubboBootstrap.reset();
    }

    @Autowired
    private HelloService helloService;

    @Test
    void testProvider() {

        String result = helloService.sayHello("dubbo");
        Assertions.assertEquals("Hello, dubbo", result);

        Map<String, ReferenceBean> referenceBeanMap = applicationContext.getBeansOfType(ReferenceBean.class);
        Assertions.assertEquals(1, referenceBeanMap.size());
        Assertions.assertNotNull(referenceBeanMap.get("&helloService"));

        ReferenceBeanManager referenceBeanManager = applicationContext.getBean(ReferenceBeanManager.BEAN_NAME, ReferenceBeanManager.class);
        Assertions.assertNotNull(referenceBeanManager.getById("helloService"));
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }


    @Configuration
    public static class ConsumerConfiguration {

        // Match and reuse 'helloService' reference bean definition in dubbo-provider.xml
        @DubboReference(group = "${myapp.group}")
        private HelloService helloService;

    }

}