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
package org.apache.dubbo.metadata.annotation.processing.util;

import org.apache.dubbo.metadata.annotation.processing.AbstractAnnotationProcessingTest;
import org.apache.dubbo.metadata.tools.DefaultTestService;
import org.apache.dubbo.metadata.tools.GenericTestService;
import org.apache.dubbo.metadata.tools.TestService;
import org.apache.dubbo.metadata.tools.TestServiceImpl;

import javax.lang.model.element.TypeElement;

import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static org.apache.dubbo.metadata.annotation.processing.util.ServiceAnnotationUtils.DUBBO_SERVICE_ANNOTATION_TYPE;
import static org.apache.dubbo.metadata.annotation.processing.util.ServiceAnnotationUtils.GROUP_ATTRIBUTE_NAME;
import static org.apache.dubbo.metadata.annotation.processing.util.ServiceAnnotationUtils.INTERFACE_CLASS_ATTRIBUTE_NAME;
import static org.apache.dubbo.metadata.annotation.processing.util.ServiceAnnotationUtils.INTERFACE_NAME_ATTRIBUTE_NAME;
import static org.apache.dubbo.metadata.annotation.processing.util.ServiceAnnotationUtils.LEGACY_SERVICE_ANNOTATION_TYPE;
import static org.apache.dubbo.metadata.annotation.processing.util.ServiceAnnotationUtils.SERVICE_ANNOTATION_TYPE;
import static org.apache.dubbo.metadata.annotation.processing.util.ServiceAnnotationUtils.SUPPORTED_ANNOTATION_TYPES;
import static org.apache.dubbo.metadata.annotation.processing.util.ServiceAnnotationUtils.VERSION_ATTRIBUTE_NAME;
import static org.apache.dubbo.metadata.annotation.processing.util.ServiceAnnotationUtils.getAnnotation;
import static org.apache.dubbo.metadata.annotation.processing.util.ServiceAnnotationUtils.getGroup;
import static org.apache.dubbo.metadata.annotation.processing.util.ServiceAnnotationUtils.getVersion;
import static org.apache.dubbo.metadata.annotation.processing.util.ServiceAnnotationUtils.isServiceAnnotationPresent;
import static org.apache.dubbo.metadata.annotation.processing.util.ServiceAnnotationUtils.resolveServiceInterfaceName;

/**
 * {@link ServiceAnnotationUtils} Test
 *
 * @since 2.7.6
 */
class ServiceAnnotationUtilsTest extends AbstractAnnotationProcessingTest {

    @Override
    protected void addCompiledClasses(Set<Class<?>> classesToBeCompiled) {}

    @Override
    protected void beforeEach() {}

    @Test
    void testConstants() {
        Assertions.assertEquals("org.apache.dubbo.config.annotation.DubboService", DUBBO_SERVICE_ANNOTATION_TYPE);
        Assertions.assertEquals("org.apache.dubbo.config.annotation.Service", SERVICE_ANNOTATION_TYPE);
        Assertions.assertEquals("com.alibaba.dubbo.config.annotation.Service", LEGACY_SERVICE_ANNOTATION_TYPE);
        Assertions.assertEquals("interfaceClass", INTERFACE_CLASS_ATTRIBUTE_NAME);
        Assertions.assertEquals("interfaceName", INTERFACE_NAME_ATTRIBUTE_NAME);
        Assertions.assertEquals("group", GROUP_ATTRIBUTE_NAME);
        Assertions.assertEquals("version", VERSION_ATTRIBUTE_NAME);
        Assertions.assertEquals(
                new LinkedHashSet<>(asList(
                        "org.apache.dubbo.config.annotation.DubboService",
                        "org.apache.dubbo.config.annotation.Service",
                        "com.alibaba.dubbo.config.annotation.Service")),
                SUPPORTED_ANNOTATION_TYPES);
    }

    @Test
    void testIsServiceAnnotationPresent() {

        Assertions.assertTrue(isServiceAnnotationPresent(getType(TestServiceImpl.class)));
        Assertions.assertTrue(isServiceAnnotationPresent(getType(GenericTestService.class)));
        Assertions.assertTrue(isServiceAnnotationPresent(getType(DefaultTestService.class)));

        Assertions.assertFalse(isServiceAnnotationPresent(getType(TestService.class)));
    }

    @Test
    void testGetAnnotation() {
        TypeElement type = getType(TestServiceImpl.class);
        Assertions.assertEquals(
                "org.apache.dubbo.config.annotation.Service",
                getAnnotation(type).getAnnotationType().toString());

        //        type = getType(GenericTestService.class);
        //        assertEquals("com.alibaba.dubbo.config.annotation.Service",
        // getAnnotation(type).getAnnotationType().toString());

        type = getType(DefaultTestService.class);
        Assertions.assertEquals(
                "org.apache.dubbo.config.annotation.Service",
                getAnnotation(type).getAnnotationType().toString());

        Assertions.assertThrows(IllegalArgumentException.class, () -> getAnnotation(getType(TestService.class)));
    }

    @Test
    void testResolveServiceInterfaceName() {
        TypeElement type = getType(TestServiceImpl.class);
        Assertions.assertEquals(
                "org.apache.dubbo.metadata.tools.TestService", resolveServiceInterfaceName(type, getAnnotation(type)));

        type = getType(GenericTestService.class);
        Assertions.assertEquals(
                "org.apache.dubbo.metadata.tools.TestService", resolveServiceInterfaceName(type, getAnnotation(type)));

        type = getType(DefaultTestService.class);
        Assertions.assertEquals(
                "org.apache.dubbo.metadata.tools.TestService", resolveServiceInterfaceName(type, getAnnotation(type)));
    }

    @Test
    void testGetVersion() {
        TypeElement type = getType(TestServiceImpl.class);
        Assertions.assertEquals("3.0.0", getVersion(getAnnotation(type)));

        type = getType(GenericTestService.class);
        Assertions.assertEquals("2.0.0", getVersion(getAnnotation(type)));

        type = getType(DefaultTestService.class);
        Assertions.assertEquals("1.0.0", getVersion(getAnnotation(type)));
    }

    @Test
    void testGetGroup() {
        TypeElement type = getType(TestServiceImpl.class);
        Assertions.assertEquals("test", getGroup(getAnnotation(type)));

        type = getType(GenericTestService.class);
        Assertions.assertEquals("generic", getGroup(getAnnotation(type)));

        type = getType(DefaultTestService.class);
        Assertions.assertEquals("default", getGroup(getAnnotation(type)));
    }
}
