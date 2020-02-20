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
import org.apache.dubbo.metadata.tools.TestService;
import org.apache.dubbo.metadata.tools.TestServiceImpl;

import org.junit.jupiter.api.Test;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.Set;

import static org.apache.dubbo.metadata.annotation.processing.util.MethodUtils.findMethod;
import static org.apache.dubbo.metadata.annotation.processing.util.MethodUtils.getAllDeclaredMethods;
import static org.apache.dubbo.metadata.annotation.processing.util.MethodUtils.getDeclaredMethods;
import static org.apache.dubbo.metadata.annotation.processing.util.MethodUtils.getMethodName;
import static org.apache.dubbo.metadata.annotation.processing.util.MethodUtils.getMethodParameterTypes;
import static org.apache.dubbo.metadata.annotation.processing.util.MethodUtils.getOverrideMethod;
import static org.apache.dubbo.metadata.annotation.processing.util.MethodUtils.getPublicNonStaticMethods;
import static org.apache.dubbo.metadata.annotation.processing.util.MethodUtils.getReturnType;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link MethodUtils} Test
 *
 * @since 2.7.6
 */
public class MethodUtilsTest extends AbstractAnnotationProcessingTest {

    private TypeElement testType;

    @Override
    protected void addCompiledClasses(Set<Class<?>> classesToBeCompiled) {
    }

    @Override
    protected void beforeEach() {
        testType = getType(TestServiceImpl.class);
    }

    @Test
    public void testDeclaredMethods() {
        TypeElement type = getType(Model.class);
        List<ExecutableElement> methods = getDeclaredMethods(type);
        assertEquals(12, methods.size());

        methods = getAllDeclaredMethods(type);
        assertEquals(34, methods.size());

        assertTrue(getAllDeclaredMethods((TypeElement) null).isEmpty());
        assertTrue(getAllDeclaredMethods((TypeMirror) null).isEmpty());
    }

    private List<? extends ExecutableElement> doGetAllDeclaredMethods() {
        return getAllDeclaredMethods(testType, Object.class);
    }

    @Test
    public void testGetAllDeclaredMethods() {
        List<? extends ExecutableElement> methods = doGetAllDeclaredMethods();
        assertEquals(14, methods.size());
    }

    @Test
    public void testGetPublicNonStaticMethods() {
        List<? extends ExecutableElement> methods = getPublicNonStaticMethods(testType, Object.class);
        assertEquals(14, methods.size());

        methods = getPublicNonStaticMethods(testType.asType(), Object.class);
        assertEquals(14, methods.size());
    }

    @Test
    public void testIsMethod() {
        List<? extends ExecutableElement> methods = getPublicNonStaticMethods(testType, Object.class);
        assertEquals(14, methods.stream().map(MethodUtils::isMethod).count());
    }

    @Test
    public void testIsPublicNonStaticMethod() {
        List<? extends ExecutableElement> methods = getPublicNonStaticMethods(testType, Object.class);
        assertEquals(14, methods.stream().map(MethodUtils::isPublicNonStaticMethod).count());
    }

    @Test
    public void testFindMethod() {
        TypeElement type = getType(Model.class);
        // Test methods from java.lang.Object
        // Object#toString()
        String methodName = "toString";
        ExecutableElement method = findMethod(type.asType(), methodName);
        assertEquals(method.getSimpleName().toString(), methodName);

        // Object#hashCode()
        methodName = "hashCode";
        method = findMethod(type.asType(), methodName);
        assertEquals(method.getSimpleName().toString(), methodName);

        // Object#getClass()
        methodName = "getClass";
        method = findMethod(type.asType(), methodName);
        assertEquals(method.getSimpleName().toString(), methodName);

        // Object#finalize()
        methodName = "finalize";
        method = findMethod(type.asType(), methodName);
        assertEquals(method.getSimpleName().toString(), methodName);

        // Object#clone()
        methodName = "clone";
        method = findMethod(type.asType(), methodName);
        assertEquals(method.getSimpleName().toString(), methodName);

        // Object#notify()
        methodName = "notify";
        method = findMethod(type.asType(), methodName);
        assertEquals(method.getSimpleName().toString(), methodName);

        // Object#notifyAll()
        methodName = "notifyAll";
        method = findMethod(type.asType(), methodName);
        assertEquals(method.getSimpleName().toString(), methodName);

        // Object#wait(long)
        methodName = "wait";
        method = findMethod(type.asType(), methodName, long.class);
        assertEquals(method.getSimpleName().toString(), methodName);

        // Object#wait(long,int)
        methodName = "wait";
        method = findMethod(type.asType(), methodName, long.class, int.class);
        assertEquals(method.getSimpleName().toString(), methodName);

        // Object#equals(Object)
        methodName = "equals";
        method = findMethod(type.asType(), methodName, Object.class);
        assertEquals(method.getSimpleName().toString(), methodName);
    }

    @Test
    public void testGetOverrideMethod() {
        List<? extends ExecutableElement> methods = doGetAllDeclaredMethods();

        ExecutableElement overrideMethod = getOverrideMethod(processingEnv, testType, methods.get(0));
        assertNull(overrideMethod);

        ExecutableElement declaringMethod = findMethod(getType(TestService.class), "echo", "java.lang.String");

        overrideMethod = getOverrideMethod(processingEnv, testType, declaringMethod);
        assertEquals(methods.get(0), overrideMethod);
    }

    @Test
    public void testGetMethodName() {
        ExecutableElement method = findMethod(testType, "echo", "java.lang.String");
        assertEquals("echo", getMethodName(method));
        assertNull(getMethodName(null));
    }

    @Test
    public void testReturnType() {
        ExecutableElement method = findMethod(testType, "echo", "java.lang.String");
        assertEquals("java.lang.String", getReturnType(method));
        assertNull(getReturnType(null));
    }

    @Test
    public void testMatchParameterTypes() {
        ExecutableElement method = findMethod(testType, "echo", "java.lang.String");
        assertArrayEquals(new String[]{"java.lang.String"}, getMethodParameterTypes(method));
        assertTrue(getMethodParameterTypes(null).length == 0);
    }
}
