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

import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.config.annotation.Service;
import org.apache.dubbo.config.spring.api.DemoService;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.util.ReflectionUtils;

import static org.apache.dubbo.config.spring.beans.factory.annotation.AnnotationBeanNameBuilderTest.GROUP;
import static org.apache.dubbo.config.spring.beans.factory.annotation.AnnotationBeanNameBuilderTest.VERSION;

/**
 * {@link AnnotationBeanNameBuilder} Test
 *
 * @see AnnotationBeanNameBuilder
 * @since 2.6.6
 */
@Service(interfaceClass = DemoService.class, group = GROUP, version = VERSION,
        application = "application", module = "module", registry = {"1", "2", "3"})
public class AnnotationBeanNameBuilderTest {

    @Reference(interfaceClass = DemoService.class, group = "DUBBO", version = "${dubbo.version}",
            application = "application", module = "module", registry = {"1", "2", "3"})
    static final Class<?> INTERFACE_CLASS = DemoService.class;

    static final String GROUP = "DUBBO";

    static final String VERSION = "1.0.0";

    private MockEnvironment environment;

    @Before
    public void prepare() {
        environment = new MockEnvironment();
        environment.setProperty("dubbo.version", "1.0.0");
    }

    @Test
    public void testServiceAnnotation() {
        Service service = AnnotationUtils.getAnnotation(AnnotationBeanNameBuilderTest.class, Service.class);
        AnnotationBeanNameBuilder builder = AnnotationBeanNameBuilder.create(service, INTERFACE_CLASS);
        Assert.assertEquals("providers:dubbo:org.apache.dubbo.config.spring.api.DemoService:1.0.0:DUBBO",
                builder.build());

        builder.environment(environment);
        Assert.assertEquals("providers:dubbo:org.apache.dubbo.config.spring.api.DemoService:1.0.0:DUBBO",
                builder.build());
    }

    @Test
    public void testReferenceAnnotation() {
        Reference reference = AnnotationUtils.getAnnotation(ReflectionUtils.findField(AnnotationBeanNameBuilderTest.class, "INTERFACE_CLASS"), Reference.class);
        AnnotationBeanNameBuilder builder = AnnotationBeanNameBuilder.create(reference, INTERFACE_CLASS);
        Assert.assertEquals("consumers:dubbo:org.apache.dubbo.config.spring.api.DemoService:${dubbo.version}:DUBBO",
                builder.build());

        builder.environment(environment);
        Assert.assertEquals("consumers:dubbo:org.apache.dubbo.config.spring.api.DemoService:1.0.0:DUBBO",
                builder.build());
    }

}