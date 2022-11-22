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
package org.apache.dubbo.spring.boot.autoconfigure;

import org.apache.dubbo.config.spring.beans.factory.annotation.ReferenceAnnotationBeanPostProcessor;
import org.apache.dubbo.config.spring.beans.factory.annotation.ServiceAnnotationPostProcessor;
import org.apache.dubbo.config.spring.util.DubboBeanUtils;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * {@link DubboAutoConfiguration} Test
 * @see DubboAutoConfiguration
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
        CompatibleDubboAutoConfigurationTest.class
}, properties = {
        "dubbo.scan.base-packages = org.apache.dubbo.spring.boot.autoconfigure"
})
@EnableAutoConfiguration
@PropertySource(value = "classpath:/META-INF/dubbo.properties")
public class CompatibleDubboAutoConfigurationTest {

    @Autowired
    private ObjectProvider<ServiceAnnotationPostProcessor> serviceAnnotationPostProcessor;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    public void testBeans() {
        Assert.assertNotNull(serviceAnnotationPostProcessor);
        Assert.assertNotNull(serviceAnnotationPostProcessor.getIfAvailable());

        ReferenceAnnotationBeanPostProcessor referenceAnnotationBeanPostProcessor =  DubboBeanUtils.getReferenceAnnotationBeanPostProcessor(applicationContext);
        Assert.assertNotNull(referenceAnnotationBeanPostProcessor);
    }
}
