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

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.dubbo.config.spring.api.DemoService;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.util.ReflectionUtils;

import static com.alibaba.dubbo.config.spring.beans.factory.annotation.ServiceBeanNameBuilderTest.GROUP;
import static com.alibaba.dubbo.config.spring.beans.factory.annotation.ServiceBeanNameBuilderTest.VERSION;

/**
 * {@link ServiceBeanNameBuilder} Test
 *
 * @see ServiceBeanNameBuilder
 * @since 2.6.5
 */
@Service(interfaceClass = DemoService.class, group = GROUP, version = VERSION,
        application = "application", module = "module", registry = {"1", "2", "3"})
@Deprecated
public class ServiceBeanNameBuilderTest {

    @Reference(interfaceClass = DemoService.class, group = "DUBBO", version = "1.0.0",
            application = "application", module = "module", registry = {"1", "2", "3"})
    static final Class<?> INTERFACE_CLASS = DemoService.class;

    static final String GROUP = "DUBBO";

    static final String VERSION = "1.0.0";

    static final String BEAN_NAME = "ServiceBean:com.alibaba.dubbo.config.spring.api.DemoService:1.0.0:DUBBO";

    private MockEnvironment environment = new MockEnvironment();

    @Test
    public void testRequiredAttributes() {
        ServiceBeanNameBuilder builder = ServiceBeanNameBuilder.create(INTERFACE_CLASS, environment);
        Assert.assertEquals("ServiceBean:com.alibaba.dubbo.config.spring.api.DemoService", builder.build());
    }

    @Test
    public void testServiceAnnotation() {
        Service service = AnnotationUtils.getAnnotation(ServiceBeanNameBuilderTest.class, Service.class);
        ServiceBeanNameBuilder builder = ServiceBeanNameBuilder.create(service, INTERFACE_CLASS, environment);
        Assert.assertEquals(BEAN_NAME,
                builder.build());
    }

    @Test
    public void testReferenceAnnotation() {
        Reference reference = AnnotationUtils.getAnnotation(ReflectionUtils.findField(ServiceBeanNameBuilderTest.class, "INTERFACE_CLASS"), Reference.class);
        ServiceBeanNameBuilder builder = ServiceBeanNameBuilder.create(reference, INTERFACE_CLASS, environment);
        Assert.assertEquals(BEAN_NAME,
                builder.build());
    }

}
