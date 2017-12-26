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
package com.alibaba.dubbo.config.spring.beans.factory.annotation;

import com.alibaba.dubbo.config.spring.api.DemoService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * {@link ServiceAnnotationBeanPostProcessor} Test
 *
 * @since 2.5.8
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(
        classes = {ServiceAnnotationBeanPostProcessorTest.TestConfiguration.class})
@TestPropertySource(properties = {
        "package1 = com.alibaba.dubbo.config.spring.context.annotation",
        "packagesToScan = ${package1}"
})
public class ServiceAnnotationBeanPostProcessorTest {

    @Autowired
    private DemoService demoService;

    @Test
    public void test() {

        String value = demoService.sayName("Mercy");

        Assert.assertEquals("Hello,Mercy", value);

    }

    @ImportResource("META-INF/spring/dubbo-annotation-provider.xml")
    @PropertySource("META-INF/default.properties")
    public static class TestConfiguration {

        @Bean
        public ServiceAnnotationBeanPostProcessor serviceAnnotationBeanPostProcessor
                (@Value("${packagesToScan}") String... packagesToScan) {
            return new ServiceAnnotationBeanPostProcessor(packagesToScan);
        }


    }

}
