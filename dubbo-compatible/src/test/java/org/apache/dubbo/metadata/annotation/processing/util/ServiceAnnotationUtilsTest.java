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

import org.junit.jupiter.api.Test;

import javax.lang.model.element.TypeElement;
import java.util.LinkedHashSet;
import java.util.Set;

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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ServiceAnnotationUtils} Test
 *
 * @since 2.7.6
 */
class ServiceAnnotationUtilsTest extends AbstractAnnotationProcessingTest {

    @Override
    protected void addCompiledClasses(Set<Class<?>> classesToBeCompiled) {

    }

    @Override
    protected void beforeEach() {

    }

    @Test
    void testConstants() {
        assertEquals("org.apache.dubbo.config.annotation.DubboService", DUBBO_SERVICE_ANNOTATION_TYPE);
        assertEquals("org.apache.dubbo.config.annotation.Service", SERVICE_ANNOTATION_TYPE);
        assertEquals("com.alibaba.dubbo.config.annotation.Service", LEGACY_SERVICE_ANNOTATION_TYPE);
        assertEquals("interfaceClass", INTERFACE_CLASS_ATTRIBUTE_NAME);
        assertEquals("interfaceName", INTERFACE_NAME_ATTRIBUTE_NAME);
        assertEquals("group", GROUP_ATTRIBUTE_NAME);
        assertEquals("version", VERSION_ATTRIBUTE_NAME);
        assertEquals(new LinkedHashSet<>(asList("org.apache.dubbo.config.annotation.DubboService", "org.apache.dubbo.config.annotation.Service", "com.alibaba.dubbo.config.annotation.Service")), SUPPORTED_ANNOTATION_TYPES);
    }

    @Test
    void testIsServiceAnnotationPresent() {

        assertTrue(isServiceAnnotationPresent(getType(TestServiceImpl.class)));
        assertTrue(isServiceAnnotationPresent(getType(GenericTestService.class)));
        assertTrue(isServiceAnnotationPresent(getType(DefaultTestService.class)));

        assertFalse(isServiceAnnotationPresent(getType(TestService.class)));
    }

    @Test
    void testGetAnnotation() {
        TypeElement type = getType(TestServiceImpl.class);
        assertEquals("org.apache.dubbo.config.annotation.Service", getAnnotation(type).getAnnotationType().toString());

        type = getType(GenericTestService.class);
        assertEquals("com.alibaba.dubbo.config.annotation.Service", getAnnotation(type).getAnnotationType().toString());

        type = getType(DefaultTestService.class);
        assertEquals("org.apache.dubbo.config.annotation.Service", getAnnotation(type).getAnnotationType().toString());

        assertThrows(IllegalArgumentException.class, () -> getAnnotation(getType(TestService.class)));
    }

    @Test
    void testResolveServiceInterfaceName() {
        TypeElement type = getType(TestServiceImpl.class);
        assertEquals("org.apache.dubbo.metadata.tools.TestService", resolveServiceInterfaceName(type, getAnnotation(type)));

        type = getType(GenericTestService.class);
        assertEquals("org.apache.dubbo.metadata.tools.TestService", resolveServiceInterfaceName(type, getAnnotation(type)));

        type = getType(DefaultTestService.class);
        assertEquals("org.apache.dubbo.metadata.tools.TestService", resolveServiceInterfaceName(type, getAnnotation(type)));
    }

    @Test
    void testGetVersion() {
        TypeElement type = getType(TestServiceImpl.class);
        assertEquals("3.0.0", getVersion(getAnnotation(type)));

        type = getType(GenericTestService.class);
        assertEquals("2.0.0", getVersion(getAnnotation(type)));

        type = getType(DefaultTestService.class);
        assertEquals("1.0.0", getVersion(getAnnotation(type)));
    }

    @Test
    void testGetGroup() {
        TypeElement type = getType(TestServiceImpl.class);
        assertEquals("test", getGroup(getAnnotation(type)));

        type = getType(GenericTestService.class);
        assertEquals("generic", getGroup(getAnnotation(type)));

        type = getType(DefaultTestService.class);
        assertEquals("default", getGroup(getAnnotation(type)));
    }
}
