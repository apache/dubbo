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

import com.alibaba.spring.context.config.ConfigurationBeanBinder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.ClassUtils;

import java.util.Map;
import java.util.Set;

import static org.apache.dubbo.spring.boot.util.DubboUtils.BASE_PACKAGES_BEAN_NAME;
import static org.apache.dubbo.spring.boot.util.DubboUtils.RELAXED_DUBBO_CONFIG_BINDER_BEAN_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * {@link DubboRelaxedBinding2AutoConfiguration} Test
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = DubboRelaxedBinding2AutoConfigurationTest.class, properties = {
        "dubbo.scan.basePackages = org.apache.dubbo.spring.boot.autoconfigure"
})
@EnableAutoConfiguration
@PropertySource(value = "classpath:/dubbo.properties")
public class DubboRelaxedBinding2AutoConfigurationTest {

    @Autowired
    @Qualifier(BASE_PACKAGES_BEAN_NAME)
    private Set<String> packagesToScan;

    @Autowired
    @Qualifier(RELAXED_DUBBO_CONFIG_BINDER_BEAN_NAME)
    private ConfigurationBeanBinder dubboConfigBinder;

    @Autowired
    private ObjectProvider<ServiceAnnotationPostProcessor> serviceAnnotationPostProcessor;

    @Autowired
    private Environment environment;

    @Autowired
    private Map<String, Environment> environments;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    public void testBeans() {


        assertTrue(ClassUtils.isAssignableValue(BinderDubboConfigBinder.class, dubboConfigBinder));

        assertNotNull(serviceAnnotationPostProcessor);
        assertNotNull(serviceAnnotationPostProcessor.getIfAvailable());

        ReferenceAnnotationBeanPostProcessor referenceAnnotationBeanPostProcessor =  DubboBeanUtils.getReferenceAnnotationBeanPostProcessor(applicationContext);
        assertNotNull(referenceAnnotationBeanPostProcessor);

        assertNotNull(environment);
        assertNotNull(environments);


        assertEquals(1, environments.size());

        assertTrue(environments.containsValue(environment));
    }

}
