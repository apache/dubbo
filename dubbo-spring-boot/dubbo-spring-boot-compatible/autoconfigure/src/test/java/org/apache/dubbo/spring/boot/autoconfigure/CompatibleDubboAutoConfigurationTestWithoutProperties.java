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

import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.spring.beans.factory.annotation.ReferenceAnnotationBeanPostProcessor;
import org.apache.dubbo.config.spring.beans.factory.annotation.ServiceAnnotationPostProcessor;
import org.apache.dubbo.config.spring.util.DubboBeanUtils;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * {@link DubboAutoConfiguration} Test
 *
 * @see DubboAutoConfiguration
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = CompatibleDubboAutoConfigurationTestWithoutProperties.class, properties = {
        "dubbo.application.name=demo"
})
@EnableAutoConfiguration
public class CompatibleDubboAutoConfigurationTestWithoutProperties {

    @Autowired(required = false)
    private ServiceAnnotationPostProcessor serviceAnnotationPostProcessor;

    @Autowired
    private ApplicationContext applicationContext;

    @Before
    public void init() {
        DubboBootstrap.reset();
    }

    @After
    public void destroy() {
        DubboBootstrap.reset();
    }

    @Test
    public void testBeans() {
        Assert.assertNull(serviceAnnotationPostProcessor);

        ReferenceAnnotationBeanPostProcessor referenceAnnotationBeanPostProcessor =  DubboBeanUtils.getReferenceAnnotationBeanPostProcessor(applicationContext);
        Assert.assertNotNull(referenceAnnotationBeanPostProcessor);
    }
}
