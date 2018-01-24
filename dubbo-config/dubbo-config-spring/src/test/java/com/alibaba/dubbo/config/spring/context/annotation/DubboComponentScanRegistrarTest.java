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
package com.alibaba.dubbo.config.spring.context.annotation;

import com.alibaba.dubbo.config.spring.api.DemoService;
import com.alibaba.dubbo.config.spring.context.annotation.consumer.ConsumerConfiguration;
import com.alibaba.dubbo.config.spring.context.annotation.provider.DemoServiceImpl;
import com.alibaba.dubbo.config.spring.context.annotation.provider.ProviderConfiguration;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;

/**
 * {@link DubboComponentScanRegistrar} Test
 *
 * @since 2.5.8
 */
public class DubboComponentScanRegistrarTest {

    @Test
    public void test() {

        AnnotationConfigApplicationContext providerContext = new AnnotationConfigApplicationContext();

        providerContext.register(ProviderConfiguration.class);

        providerContext.refresh();

        DemoService demoService = providerContext.getBean(DemoService.class);

        String value = demoService.sayName("Mercy");

        Assert.assertEquals("Hello,Mercy", value);

        Class<?> beanClass = AopUtils.getTargetClass(demoService);

        // DemoServiceImpl with @Transactional
        Assert.assertEquals(DemoServiceImpl.class, beanClass);

        // Test @Transactional is present or not
        Assert.assertNotNull(findAnnotation(beanClass, Transactional.class));

        AnnotationConfigApplicationContext consumerContext = new AnnotationConfigApplicationContext();

        consumerContext.register(ConsumerConfiguration.class);

        consumerContext.refresh();

        ConsumerConfiguration consumerConfiguration = consumerContext.getBean(ConsumerConfiguration.class);

        demoService = consumerConfiguration.getDemoService();

        value = demoService.sayName("Mercy");

        Assert.assertEquals("Hello,Mercy", value);

        ConsumerConfiguration.Child child = consumerContext.getBean(ConsumerConfiguration.Child.class);

        // From Child

        demoService = child.getDemoServiceFromChild();

        Assert.assertNotNull(demoService);

        value = demoService.sayName("Mercy");

        Assert.assertEquals("Hello,Mercy", value);

        // From Parent

        demoService = child.getDemoServiceFromParent();

        Assert.assertNotNull(demoService);

        value = demoService.sayName("Mercy");

        Assert.assertEquals("Hello,Mercy", value);

        // From Ancestor

        demoService = child.getDemoServiceFromAncestor();

        Assert.assertNotNull(demoService);

        value = demoService.sayName("Mercy");

        Assert.assertEquals("Hello,Mercy", value);

        providerContext.close();
        consumerContext.close();


    }


}
