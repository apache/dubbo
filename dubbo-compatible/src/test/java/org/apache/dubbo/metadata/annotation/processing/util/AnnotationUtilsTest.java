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
import org.apache.dubbo.metadata.rest.SpringRestService;
import org.apache.dubbo.metadata.tools.TestService;
import org.apache.dubbo.metadata.tools.TestServiceImpl;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.ws.rs.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationUtils.findAnnotation;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationUtils.findMetaAnnotation;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationUtils.getAllAnnotations;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationUtils.getAnnotation;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationUtils.getAnnotations;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationUtils.getAttribute;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationUtils.getValue;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationUtils.isAnnotationPresent;
import static org.apache.dubbo.metadata.annotation.processing.util.MethodUtils.findMethod;
import static org.apache.dubbo.metadata.annotation.processing.util.MethodUtils.getAllDeclaredMethods;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The {@link AnnotationUtils} Test
 *
 * @since 2.7.6
 */
class AnnotationUtilsTest extends AbstractAnnotationProcessingTest {

    private TypeElement testType;

    @Override
    protected void addCompiledClasses(Set<Class<?>> classesToBeCompiled) {
    }

    @Override
    protected void beforeEach() {
        testType = getType(TestServiceImpl.class);
    }

    @Test
    void testGetAnnotation() {
        AnnotationMirror serviceAnnotation = getAnnotation(testType, Service.class);
        assertEquals("3.0.0", getAttribute(serviceAnnotation, "version"));
        assertEquals("test", getAttribute(serviceAnnotation, "group"));
        assertEquals("org.apache.dubbo.metadata.tools.TestService", getAttribute(serviceAnnotation, "interfaceName"));

        assertNull(getAnnotation(testType, (Class) null));
        assertNull(getAnnotation(testType, (String) null));

        assertNull(getAnnotation(testType.asType(), (Class) null));
        assertNull(getAnnotation(testType.asType(), (String) null));

        assertNull(getAnnotation((Element) null, (Class) null));
        assertNull(getAnnotation((Element) null, (String) null));

        assertNull(getAnnotation((TypeElement) null, (Class) null));
        assertNull(getAnnotation((TypeElement) null, (String) null));
    }

    @Test
    void testGetAnnotations() {
        List<AnnotationMirror> annotations = getAnnotations(testType);
        Iterator<AnnotationMirror> iterator = annotations.iterator();

        assertEquals(2, annotations.size());
        assertEquals("com.alibaba.dubbo.config.annotation.Service", iterator.next().getAnnotationType().toString());
        assertEquals("org.apache.dubbo.config.annotation.Service", iterator.next().getAnnotationType().toString());

        annotations = getAnnotations(testType, Service.class);
        iterator = annotations.iterator();
        assertEquals(1, annotations.size());
        assertEquals("org.apache.dubbo.config.annotation.Service", iterator.next().getAnnotationType().toString());

        annotations = getAnnotations(testType.asType(), Service.class);
        iterator = annotations.iterator();
        assertEquals(1, annotations.size());
        assertEquals("org.apache.dubbo.config.annotation.Service", iterator.next().getAnnotationType().toString());

        annotations = getAnnotations(testType.asType(), Service.class.getTypeName());
        iterator = annotations.iterator();
        assertEquals(1, annotations.size());
        assertEquals("org.apache.dubbo.config.annotation.Service", iterator.next().getAnnotationType().toString());

        annotations = getAnnotations(testType, Override.class);
        assertEquals(0, annotations.size());

        annotations = getAnnotations(testType, com.alibaba.dubbo.config.annotation.Service.class);
        assertEquals(1, annotations.size());

        assertTrue(getAnnotations(null, (Class) null).isEmpty());
        assertTrue(getAnnotations(null, (String) null).isEmpty());
        assertTrue(getAnnotations(testType, (Class) null).isEmpty());
        assertTrue(getAnnotations(testType, (String) null).isEmpty());

        assertTrue(getAnnotations(null, Service.class).isEmpty());
        assertTrue(getAnnotations(null, Service.class.getTypeName()).isEmpty());
    }

    @Test
    void testGetAllAnnotations() {

        List<AnnotationMirror> annotations = getAllAnnotations(testType);
        assertEquals(5, annotations.size());

        annotations = getAllAnnotations(testType.asType(), annotation -> true);
        assertEquals(5, annotations.size());

        annotations = getAllAnnotations(processingEnv, TestServiceImpl.class);
        assertEquals(5, annotations.size());

        annotations = getAllAnnotations(testType.asType(), Service.class);
        assertEquals(2, annotations.size());

        annotations = getAllAnnotations(testType, Override.class);
        assertEquals(0, annotations.size());

        annotations = getAllAnnotations(testType.asType(), com.alibaba.dubbo.config.annotation.Service.class);
        assertEquals(2, annotations.size());

        assertTrue(getAllAnnotations((Element) null, (Class) null).isEmpty());
        assertTrue(getAllAnnotations((TypeMirror) null, (String) null).isEmpty());
        assertTrue(getAllAnnotations((ProcessingEnvironment) null, (Class) null).isEmpty());
        assertTrue(getAllAnnotations((ProcessingEnvironment) null, (String) null).isEmpty());

        assertTrue(getAllAnnotations((Element) null).isEmpty());
        assertTrue(getAllAnnotations((TypeMirror) null).isEmpty());
        assertTrue(getAllAnnotations(processingEnv, (Class) null).isEmpty());
        assertTrue(getAllAnnotations(processingEnv, (String) null).isEmpty());


        assertTrue(getAllAnnotations(testType, (Class) null).isEmpty());
        assertTrue(getAllAnnotations(testType.asType(), (Class) null).isEmpty());

        assertTrue(getAllAnnotations(testType, (String) null).isEmpty());
        assertTrue(getAllAnnotations(testType.asType(), (String) null).isEmpty());

        assertTrue(getAllAnnotations((Element) null, Service.class).isEmpty());
        assertTrue(getAllAnnotations((TypeMirror) null, Service.class.getTypeName()).isEmpty());
    }


    @Test
    void testFindAnnotation() {

        assertEquals("org.apache.dubbo.config.annotation.Service", findAnnotation(testType, Service.class).getAnnotationType().toString());
        assertEquals("com.alibaba.dubbo.config.annotation.Service", findAnnotation(testType, com.alibaba.dubbo.config.annotation.Service.class).getAnnotationType().toString());
        assertEquals("javax.ws.rs.Path", findAnnotation(testType, Path.class).getAnnotationType().toString());
        assertEquals("javax.ws.rs.Path", findAnnotation(testType.asType(), Path.class).getAnnotationType().toString());
        assertEquals("javax.ws.rs.Path", findAnnotation(testType.asType(), Path.class.getTypeName()).getAnnotationType().toString());
        assertNull(findAnnotation(testType, Override.class));

        assertNull(findAnnotation((Element) null, (Class) null));
        assertNull(findAnnotation((Element) null, (String) null));
        assertNull(findAnnotation((TypeMirror) null, (Class) null));
        assertNull(findAnnotation((TypeMirror) null, (String) null));

        assertNull(findAnnotation(testType, (Class) null));
        assertNull(findAnnotation(testType, (String) null));
        assertNull(findAnnotation(testType.asType(), (Class) null));
        assertNull(findAnnotation(testType.asType(), (String) null));
    }

    @Test
    void testFindMetaAnnotation() {
        getAllDeclaredMethods(getType(TestService.class)).forEach(method -> {
            assertEquals("javax.ws.rs.HttpMethod", findMetaAnnotation(method, "javax.ws.rs.HttpMethod").getAnnotationType().toString());
        });
    }

    @Test
    void testGetAttribute() {
        assertEquals("org.apache.dubbo.metadata.tools.TestService", getAttribute(findAnnotation(testType, Service.class), "interfaceName"));
        assertEquals("org.apache.dubbo.metadata.tools.TestService", getAttribute(findAnnotation(testType, Service.class).getElementValues(), "interfaceName"));
        assertEquals("/echo", getAttribute(findAnnotation(testType, Path.class), "value"));

        assertNull(getAttribute(findAnnotation(testType, Path.class), null));
        assertNull(getAttribute(findAnnotation(testType, (Class) null), null));

        ExecutableElement method = findMethod(getType(SpringRestService.class), "param", String.class);

        AnnotationMirror annotation = findAnnotation(method, GetMapping.class);

        assertArrayEquals(new String[]{"/param"}, (String[]) getAttribute(annotation, "value"));
        assertNull(getAttribute(annotation, "path"));
    }

    @Test
    void testGetValue() {
        AnnotationMirror pathAnnotation = getAnnotation(getType(TestService.class), Path.class);
        assertEquals("/echo", getValue(pathAnnotation));
    }

    @Test
    void testIsAnnotationPresent() {
        assertTrue(isAnnotationPresent(testType, "org.apache.dubbo.config.annotation.Service"));
        assertTrue(isAnnotationPresent(testType, "com.alibaba.dubbo.config.annotation.Service"));
        assertTrue(isAnnotationPresent(testType, "javax.ws.rs.Path"));
    }
}
