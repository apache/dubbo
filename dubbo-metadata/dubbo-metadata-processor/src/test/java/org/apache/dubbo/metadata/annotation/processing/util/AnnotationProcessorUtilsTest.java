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
import org.apache.dubbo.metadata.annotation.processing.model.Model;
import org.apache.dubbo.metadata.tools.DefaultTestService;
import org.apache.dubbo.metadata.tools.GenericTestService;
import org.apache.dubbo.metadata.tools.TestService;
import org.apache.dubbo.metadata.tools.TestServiceImpl;

import org.junit.jupiter.api.Test;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.POST;
import java.util.List;
import java.util.Set;

import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationProcessorUtils.findAnnotation;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationProcessorUtils.findMetaAnnotation;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationProcessorUtils.findMethod;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationProcessorUtils.getAllAnnotations;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationProcessorUtils.getAllDeclaredMethods;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationProcessorUtils.getAnnotation;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationProcessorUtils.getAnnotations;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationProcessorUtils.getAttribute;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationProcessorUtils.getField;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationProcessorUtils.getHierarchicalMethods;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationProcessorUtils.getHierarchicalTypes;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationProcessorUtils.getInterfaces;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationProcessorUtils.getMethods;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationProcessorUtils.getOverrideMethod;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationProcessorUtils.getValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link AnnotationProcessorUtils} Test
 *
 * @since 2.7.5
 */
public class AnnotationProcessorUtilsTest extends AbstractAnnotationProcessingTest {

    private TypeElement testType;

    @Override
    protected void beforeEach() {
        testType = getType(TestServiceImpl.class);
    }

    @Override
    protected void addCompiledClasses(Set<Class<?>> classesToBeCompiled) {
        classesToBeCompiled.add(TestServiceImpl.class);
    }

    @Test
    public void testGetHierarchicalTypes() {
        Set<TypeElement> hierarchicalTypes = getHierarchicalTypes(processingEnv, testType);
        assertEquals(5, hierarchicalTypes.size());
        assertTrue(hierarchicalTypes.contains(getType(TestServiceImpl.class)));
        assertTrue(hierarchicalTypes.contains(getType(GenericTestService.class)));
        assertTrue(hierarchicalTypes.contains(getType(TestService.class)));
        assertTrue(hierarchicalTypes.contains(getType(DefaultTestService.class)));
        assertTrue(hierarchicalTypes.contains(getType(Object.class)));
    }

    @Test
    public void testGetAllAnnotations() {
        List<AnnotationMirror> annotations = getAllAnnotations(processingEnv, testType);
        List<AnnotationMirror> legacyAnnotations = getAllAnnotations(processingEnv, com.alibaba.dubbo.config.annotation.Service.class);
        assertEquals(5, annotations.size());
        assertEquals(5, legacyAnnotations.size());
    }

    @Test
    public void testGetAnnotations() {
        List<AnnotationMirror> serviceAnnotations = getAnnotations(processingEnv, testType, Service.class);
        assertEquals(3, serviceAnnotations.size());

        serviceAnnotations = getAnnotations(processingEnv, testType, Override.class);
        assertEquals(0, serviceAnnotations.size());

        List<AnnotationMirror> legacyServiceAnnotations = getAnnotations(processingEnv, testType,
                com.alibaba.dubbo.config.annotation.Service.class);
        assertEquals(1, legacyServiceAnnotations.size());
    }

    @Test
    public void testGetAnnotation() {
        AnnotationMirror serviceAnnotation = getAnnotation(processingEnv, testType, Service.class);
        assertEquals("3.0.0", getAttribute(serviceAnnotation, "version"));
        assertEquals("test", getAttribute(serviceAnnotation, "group"));
        assertEquals("org.apache.dubbo.metadata.tools.EchoService", getAttribute(serviceAnnotation, "interfaceName"));
    }

    @Test
    public void testMethods() {
        List<? extends ExecutableElement> methods = getMethods(processingEnv, testType, Object.class);
        assertEquals(2, methods.size());
        ExecutableElement method = methods.get(1);
        ExecutableElement overrideMethod = getOverrideMethod(processingEnv, testType, method);
        assertNull(overrideMethod);

        List<? extends ExecutableElement> declaringMethods = getMethods(processingEnv, TestService.class, Object.class);
        ExecutableElement declaringMethod = declaringMethods.get(0);
        overrideMethod = getOverrideMethod(processingEnv, testType, declaringMethod);
        assertEquals(method, overrideMethod);
    }

    @Test
    public void testFindMethod() {
        ExecutableElement method = findMethod(processingEnv, testType, "echo", String.class);
        assertEquals("echo", method.getSimpleName().toString());

        method = findMethod(processingEnv, testType, "model", Model.class);
        assertEquals("model", method.getSimpleName().toString());

        method = findMethod(processingEnv, testType, "hashCode");
        assertEquals("hashCode", method.getSimpleName().toString());
    }

    @Test
    public void testGetOverrideMethod() {
        ExecutableElement method = findMethod(processingEnv, testType, "echo", String.class);
        ExecutableElement declaringMethod = findMethod(processingEnv, getType(TestService.class), "echo", String.class);
        assertEquals(method, getOverrideMethod(processingEnv, testType, declaringMethod));
    }

    @Test
    public void testGetHierarchicalMethods() {
        List<? extends ExecutableElement> methods = getMethods(processingEnv, testType, Object.class);
        methods.forEach(method -> {
            Set<ExecutableElement> m = getHierarchicalMethods(processingEnv, method);
            if ("echo".equals(method.getSimpleName().toString())) {
                assertEquals(4, m.size());
            } else {
                assertEquals(2, m.size());
            }
        });
    }

    @Test
    public void testGetAllDeclaredMethods() {
        Set<ExecutableElement> methods = getAllDeclaredMethods(processingEnv, testType, Object.class);
        assertEquals(6, methods.size());
    }

    @Test
    public void testGetField() {

        TypeElement type = getType(Model.class);

        assertNotNull(getField(processingEnv, type, "z"));
        assertNotNull(getField(processingEnv, type, "b"));
        assertNotNull(getField(processingEnv, type, "s"));
        assertNotNull(getField(processingEnv, type, "i"));
        assertNotNull(getField(processingEnv, type, "l"));
        assertNotNull(getField(processingEnv, type, "f"));
        assertNotNull(getField(processingEnv, type, "d"));
        assertNotNull(getField(processingEnv, type, "tu"));
        assertNotNull(getField(processingEnv, type, "str"));
        assertNotNull(getField(processingEnv, type, "bi"));
        assertNotNull(getField(processingEnv, type, "bd"));

        assertNull(getField(processingEnv, type, "notFound"));
    }

    @Test
    public void testGetInterfaces() {
        TypeElement type = getType(Model.class);

        Set<TypeElement> interfaces = getInterfaces(processingEnv, type);

        assertTrue(interfaces.isEmpty());

        interfaces = getInterfaces(processingEnv, testType);

        assertEquals(1, interfaces.size());
        assertEquals(getType(TestService.class), interfaces.iterator().next());
    }

    @Test
    public void testGetAllInterfaces() {
        Set<TypeElement> interfaces = getInterfaces(processingEnv, testType);
        assertEquals(1, interfaces.size());
        assertEquals(getType(TestService.class), interfaces.iterator().next());
    }

    @Test
    public void testFindAnnotation() {
        ExecutableElement modelMethodFromEchoService = findMethod(processingEnv, getType(TestService.class), "model", Model.class);
        ExecutableElement modelMethodFromTestEchoService = findMethod(processingEnv, testType, "model", Model.class);
        AnnotationMirror annotation = findAnnotation(processingEnv, modelMethodFromEchoService, POST.class);
        AnnotationMirror annotation2 = findAnnotation(processingEnv, modelMethodFromTestEchoService, POST.class);
        assertEquals(annotation, annotation2);
    }

    @Test
    public void testFindMetaAnnotation() {
        ExecutableElement method = findMethod(processingEnv, testType, "model", Model.class);
        AnnotationMirror annotation = findMetaAnnotation(processingEnv, method, HttpMethod.class);
        assertEquals("POST", getValue(annotation));
    }
}


