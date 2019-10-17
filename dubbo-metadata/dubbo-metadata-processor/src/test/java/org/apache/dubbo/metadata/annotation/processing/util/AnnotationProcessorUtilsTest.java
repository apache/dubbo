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
import java.io.Serializable;
import java.util.EventListener;
import java.util.List;
import java.util.Set;

import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationUtils.getAnnotation;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationUtils.getValue;
import static org.apache.dubbo.metadata.annotation.processing.util.MethodUtils.findMethod;
import static org.apache.dubbo.metadata.annotation.processing.util.MethodUtils.getOverrideMethod;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.getHierarchicalTypes;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
        Set<TypeElement> hierarchicalTypes = getHierarchicalTypes(testType);
        assertEquals(8, hierarchicalTypes.size());
        assertTrue(hierarchicalTypes.contains(getType(TestServiceImpl.class)));
        assertTrue(hierarchicalTypes.contains(getType(GenericTestService.class)));
        assertTrue(hierarchicalTypes.contains(getType(TestService.class)));
        assertTrue(hierarchicalTypes.contains(getType(DefaultTestService.class)));
        assertTrue(hierarchicalTypes.contains(getType(Object.class)));
        assertTrue(hierarchicalTypes.contains(getType(AutoCloseable.class)));
        assertTrue(hierarchicalTypes.contains(getType(Serializable.class)));
        assertTrue(hierarchicalTypes.contains(getType(EventListener.class)));
    }

    @Test
    public void testMethods() {
    }

    @Test
    public void testFindMethod() {
        ExecutableElement method = findMethod(testType, "echo", String.class);
        assertEquals("echo", method.getSimpleName().toString());

        method = findMethod(testType, "model", Model.class);
        assertEquals("model", method.getSimpleName().toString());

        method = findMethod(testType, "hashCode");
        assertEquals("hashCode", method.getSimpleName().toString());
    }

    @Test
    public void testGetOverrideMethod() {
        ExecutableElement method = findMethod(testType, "echo", String.class);
        ExecutableElement declaringMethod = findMethod(getType(TestService.class), "echo", String.class);
        assertEquals(method, getOverrideMethod(processingEnv, testType, declaringMethod));
    }

    @Test
    public void testGetHierarchicalMethods() {
//        List<? extends ExecutableElement> methods = getMethods(testType, Object.class);
//        methods.forEach(method -> {
//            List<ExecutableElement> m = getAllDeclaredMethods(method);
//            if ("echo".equals(method.getSimpleName().toString())) {
//                assertEquals(4, m.size());
//            } else {
//                assertEquals(2, m.size());
//            }
//        });
    }

    @Test
    public void testGetAllDeclaredMethods() {
        List<ExecutableElement> methods = MethodUtils.getAllDeclaredMethods(testType, Object.class);
        assertEquals(6, methods.size());
    }

    @Test
    public void testFindAnnotation() {
        ExecutableElement modelMethodFromEchoService = findMethod(getType(TestService.class), "model", Model.class);
        ExecutableElement modelMethodFromTestEchoService = findMethod(testType, "model", Model.class);
        AnnotationMirror annotation = getAnnotation(modelMethodFromEchoService, POST.class);
        AnnotationMirror annotation2 = getAnnotation(modelMethodFromTestEchoService, POST.class);
        assertEquals(annotation, annotation2);
    }

    @Test
    public void testFindMetaAnnotation() {
        ExecutableElement method = findMethod(testType, "model", Model.class);
        AnnotationMirror annotation = getAnnotation(method, HttpMethod.class);
        assertEquals("POST", getValue(annotation));
    }
}


