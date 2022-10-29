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
package org.apache.dubbo.config.spring.beans.factory.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = DubboConfigEarlyInitializationTest.class)
@ImportResource(locations = "classpath:/META-INF/spring/dubbo-config-early-initialization.xml")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class DubboConfigEarlyInitializationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    public void testDubboConfigEarlyInitializationPostProcessor() {
        assertTrue(applicationContext instanceof GenericApplicationContext);
        ConfigurableListableBeanFactory clBeanFactory = ((GenericApplicationContext) applicationContext).getBeanFactory();
        assertTrue(clBeanFactory instanceof DefaultListableBeanFactory);
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) clBeanFactory;
        List<BeanPostProcessor> beanPostProcessorList = beanFactory.getBeanPostProcessors();
        assertEquals(beanFactory.getBeanPostProcessorCount(), beanPostProcessorList.size());
        boolean containsDubboConfigEarlyInitializationPostProcessor = false;
        for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
            if (beanPostProcessor instanceof DubboConfigEarlyRegistrationPostProcessor.DubboConfigEarlyInitializationPostProcessor) {
                containsDubboConfigEarlyInitializationPostProcessor = true;
                break;
            }
        }
        assertTrue(containsDubboConfigEarlyInitializationPostProcessor);
    }
}
