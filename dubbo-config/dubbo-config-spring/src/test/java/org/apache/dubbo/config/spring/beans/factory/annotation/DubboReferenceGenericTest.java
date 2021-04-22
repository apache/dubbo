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
import org.apache.dubbo.config.spring.api.HelloService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.apache.dubbo.config.spring.beans.factory.annotation.ReferenceAnnotationBeanPostProcessor.BEAN_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link DubboReference @DubboReference} of Generic injection test
 *
 * @see DubboReference
 * @since 2.7.9
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(
        classes = {
                ServiceAnnotationTestConfiguration.class,
                DubboReferenceGenericTest.class
        })
@TestPropertySource(properties = {
        "packagesToScan = org.apache.dubbo.config.spring.context.annotation.provider",
        "consumer.version = ${demo.service.version}",
        "consumer.url = dubbo://127.0.0.1:12345?version=2.5.7",
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class DubboReferenceGenericTest {

    @Bean
    public HelloServiceManager helloServiceManager() {
        return new HelloServiceManager();
    }

    @Bean(BEAN_NAME)
    public ReferenceAnnotationBeanPostProcessor referenceAnnotationBeanPostProcessor() {
        return new ReferenceAnnotationBeanPostProcessor();
    }

    @Bean
    public String helloWorld() {
        return "Hello,World";
    }

    @Bean
    public StringService stringService() {
        return new StringService();
    }

    @DubboReference
    private HelloService helloService;

    @Autowired
    private StringService stringService;

    @Autowired
    private HelloServiceManager helloServiceManager;

    @Test
    public void test() {
        assertEquals("Hello,World", stringService.getS());
        HelloService helloService = helloServiceManager.getService();
        assertEquals("Greeting, Mercy", helloService.sayHello("Mercy"));
    }


    static abstract class AbstractServiceManager<S> {

        @DubboReference
        protected S service;

        public S getService() {
            return service;
        }
    }

    static class HelloServiceManager extends AbstractServiceManager<HelloService> {

    }

    static abstract class AbstractService<S> {

        @Autowired
        private S s;

        public S getS() {
            return s;
        }
    }

    static class StringService extends AbstractService<String> {

    }

}
