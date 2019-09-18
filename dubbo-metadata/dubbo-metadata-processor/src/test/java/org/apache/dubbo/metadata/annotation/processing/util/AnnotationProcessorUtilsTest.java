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
import org.apache.dubbo.metadata.tools.Compiler;
import org.apache.dubbo.metadata.tools.DefaultEchoService;
import org.apache.dubbo.metadata.tools.EchoService;
import org.apache.dubbo.metadata.tools.GenericEchoService;
import org.apache.dubbo.metadata.tools.TestEchoService;
import org.apache.dubbo.metadata.tools.TestProcessor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationProcessorUtils.getAllAnnotations;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationProcessorUtils.getAnnotation;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationProcessorUtils.getAnnotations;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationProcessorUtils.getAttribute;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationProcessorUtils.getHierarchicalTypes;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationProcessorUtils.getMethods;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationProcessorUtils.getOverrideMethod;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link AnnotationProcessorUtils} Test
 *
 * @since 2.7.5
 */
public class AnnotationProcessorUtilsTest {

    private ProcessingEnvironment processingEnvironment;

    private Elements elements;

    private Types types;

    private TypeElement testType;

    @BeforeEach
    public void init() throws IOException {
        TestProcessor testProcessor = new TestProcessor();
        Compiler compiler = new Compiler();
        compiler.processors(testProcessor);
        compiler.compile(TestEchoService.class);
        processingEnvironment = testProcessor.getProcessingEnvironment();
        elements = processingEnvironment.getElementUtils();
        types = processingEnvironment.getTypeUtils();
        testType = elements.getTypeElement(TestEchoService.class.getName());
    }

    @Test
    public void testGetHierarchicalTypes() {
        Set<TypeElement> hierarchicalTypes = getHierarchicalTypes(processingEnvironment, testType);
        assertEquals(5, hierarchicalTypes.size());
        assertTrue(hierarchicalTypes.contains(elements.getTypeElement(TestEchoService.class.getName())));
        assertTrue(hierarchicalTypes.contains(elements.getTypeElement(GenericEchoService.class.getName())));
        assertTrue(hierarchicalTypes.contains(elements.getTypeElement(EchoService.class.getName())));
        assertTrue(hierarchicalTypes.contains(elements.getTypeElement(DefaultEchoService.class.getName())));
        assertTrue(hierarchicalTypes.contains(elements.getTypeElement(Object.class.getName())));
    }

    @Test
    public void testGetAllAnnotations() {
        List<AnnotationMirror> annotations = getAllAnnotations(processingEnvironment, testType);
        List<AnnotationMirror> legacyAnnotations = getAllAnnotations(processingEnvironment, com.alibaba.dubbo.config.annotation.Service.class);
        assertEquals(5, annotations.size());
        assertEquals(5, legacyAnnotations.size());
    }

    @Test
    public void testGetAnnotations() {
        List<AnnotationMirror> serviceAnnotations = getAnnotations(processingEnvironment, testType, Service.class);
        assertEquals(3, serviceAnnotations.size());

        serviceAnnotations = getAnnotations(processingEnvironment, testType, Override.class);
        assertEquals(0, serviceAnnotations.size());

        List<AnnotationMirror> legacyServiceAnnotations = getAnnotations(processingEnvironment, testType,
                com.alibaba.dubbo.config.annotation.Service.class);
        assertEquals(1, legacyServiceAnnotations.size());
    }

    @Test
    public void testGetAnnotation() {
        AnnotationMirror serviceAnnotation = getAnnotation(processingEnvironment, testType, Service.class);
        assertEquals("3.0.0", getAttribute(serviceAnnotation, "version"));
        assertEquals("test", getAttribute(serviceAnnotation, "group"));
        assertEquals("org.apache.dubbo.metadata.tools.EchoService", getAttribute(serviceAnnotation, "interfaceName"));
    }

    @Test
    public void testMethods() {
        List<? extends ExecutableElement> methods = getMethods(processingEnvironment, testType, Object.class);
        assertEquals(1, methods.size());
        ExecutableElement method = methods.get(0);
        ExecutableElement overrideMethod = getOverrideMethod(processingEnvironment, testType, method);
        assertNull(overrideMethod);

        List<? extends ExecutableElement> declaringMethods = getMethods(processingEnvironment, EchoService.class, Object.class);
        ExecutableElement declaringMethod = declaringMethods.get(0);
        overrideMethod = getOverrideMethod(processingEnvironment, testType, declaringMethod);
        assertEquals(method, overrideMethod);
    }
}


