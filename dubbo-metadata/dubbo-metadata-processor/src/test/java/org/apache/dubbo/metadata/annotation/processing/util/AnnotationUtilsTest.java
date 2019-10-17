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

import org.apache.dubbo.config.annotation.Service;
import org.apache.dubbo.metadata.annotation.processing.AbstractAnnotationProcessingTest;
import org.apache.dubbo.metadata.tools.TestService;
import org.apache.dubbo.metadata.tools.TestServiceImpl;

import org.junit.jupiter.api.Test;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import javax.ws.rs.Path;
import java.util.List;
import java.util.Set;

import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationUtils.getAllAnnotations;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationUtils.getAnnotation;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationUtils.getAnnotations;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationUtils.getAttribute;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationUtils.getValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The {@link AnnotationUtils} Test
 *
 * @since 2.7.5
 */
public class AnnotationUtilsTest extends AbstractAnnotationProcessingTest {

    private TypeElement testType;

    @Override
    protected void addCompiledClasses(Set<Class<?>> classesToBeCompiled) {
    }

    @Override
    protected void beforeEach() {
        testType = getType(TestServiceImpl.class);
    }

    @Test
    public void testGetAnnotation() {
        AnnotationMirror serviceAnnotation = getAnnotation(testType, Service.class);
        assertEquals("3.0.0", getAttribute(serviceAnnotation, "version"));
        assertEquals("test", getAttribute(serviceAnnotation, "group"));
        assertEquals("org.apache.dubbo.metadata.tools.EchoService", getAttribute(serviceAnnotation, "interfaceName"));
    }

    @Test
    public void testGetValue() {
        AnnotationMirror pathAnnotation = getAnnotation(getType(TestService.class), Path.class);
        assertEquals("/echo", getValue(pathAnnotation));
    }

    @Test
    public void testGetAllAnnotations() {
        List<AnnotationMirror> annotations = getAllAnnotations(testType);
        List<AnnotationMirror> legacyAnnotations = getAllAnnotations(processingEnv, com.alibaba.dubbo.config.annotation.Service.class);
        assertEquals(5, annotations.size());
        assertEquals(5, legacyAnnotations.size());
    }

    @Test
    public void testGetAnnotations() {
        List<AnnotationMirror> serviceAnnotations = getAnnotations(testType, Service.class);
        assertEquals(3, serviceAnnotations.size());

        serviceAnnotations = getAnnotations(testType, Override.class);
        assertEquals(0, serviceAnnotations.size());

        List<AnnotationMirror> legacyServiceAnnotations = getAnnotations(testType,
                com.alibaba.dubbo.config.annotation.Service.class);
        assertEquals(1, legacyServiceAnnotations.size());
    }
}
