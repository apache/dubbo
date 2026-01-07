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

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.ws.rs.Path;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationUtils.findAnnotation;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationUtils.findMetaAnnotation;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationUtils.getAllAnnotations;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationUtils.getAnnotation;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationUtils.getAnnotations;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationUtils.getAttribute;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationUtils.getValue;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationUtils.isAnnotationPresent;
import static org.apache.dubbo.metadata.annotation.processing.util.MethodUtils.getAllDeclaredMethods;

/**
 * The {@link AnnotationUtils} Test
 *
 * @since 2.7.6
 */
class AnnotationUtilsTest extends AbstractAnnotationProcessingTest {

    private TypeElement testType;

    @Override
    protected void addCompiledClasses(Set<Class<?>> classesToBeCompiled) {}

    @Override
    protected void beforeEach() {
        testType = getType(TestServiceImpl.class);
    }

    @Test
    void testGetAnnotation() {
        AnnotationMirror serviceAnnotation = getAnnotation(testType, Service.class);
        Assertions.assertEquals("3.0.0", getAttribute(serviceAnnotation, "version"));
        Assertions.assertEquals("test", getAttribute(serviceAnnotation, "group"));
        Assertions.assertEquals(
                "org.apache.dubbo.metadata.tools.TestService", getAttribute(serviceAnnotation, "interfaceName"));

        Assertions.assertNull(getAnnotation(testType, (Class) null));
        Assertions.assertNull(getAnnotation(testType, (String) null));

        Assertions.assertNull(getAnnotation(testType.asType(), (Class) null));
        Assertions.assertNull(getAnnotation(testType.asType(), (String) null));

        Assertions.assertNull(getAnnotation((Element) null, (Class) null));
        Assertions.assertNull(getAnnotation((Element) null, (String) null));

        Assertions.assertNull(getAnnotation((TypeElement) null, (Class) null));
        Assertions.assertNull(getAnnotation((TypeElement) null, (String) null));
    }

    @Test
    void testGetAnnotations() {
        List<AnnotationMirror> annotations = getAnnotations(testType);
        Iterator<AnnotationMirror> iterator = annotations.iterator();

        Assertions.assertEquals(1, annotations.size());
        //        assertEquals("com.alibaba.dubbo.config.annotation.Service",
        // iterator.next().getAnnotationType().toString());
        Assertions.assertEquals(
                "org.apache.dubbo.config.annotation.Service",
                iterator.next().getAnnotationType().toString());

        annotations = getAnnotations(testType, Service.class);
        iterator = annotations.iterator();
        Assertions.assertEquals(1, annotations.size());
        Assertions.assertEquals(
                "org.apache.dubbo.config.annotation.Service",
                iterator.next().getAnnotationType().toString());

        annotations = getAnnotations(testType.asType(), Service.class);
        iterator = annotations.iterator();
        Assertions.assertEquals(1, annotations.size());
        Assertions.assertEquals(
                "org.apache.dubbo.config.annotation.Service",
                iterator.next().getAnnotationType().toString());

        annotations = getAnnotations(testType.asType(), Service.class.getTypeName());
        iterator = annotations.iterator();
        Assertions.assertEquals(1, annotations.size());
        Assertions.assertEquals(
                "org.apache.dubbo.config.annotation.Service",
                iterator.next().getAnnotationType().toString());

        annotations = getAnnotations(testType, Override.class);
        Assertions.assertEquals(0, annotations.size());

        //        annotations = getAnnotations(testType, com.alibaba.dubbo.config.annotation.Service.class);
        //        assertEquals(1, annotations.size());

        Assertions.assertTrue(getAnnotations(null, (Class) null).isEmpty());
        Assertions.assertTrue(getAnnotations(null, (String) null).isEmpty());
        Assertions.assertTrue(getAnnotations(testType, (Class) null).isEmpty());
        Assertions.assertTrue(getAnnotations(testType, (String) null).isEmpty());

        Assertions.assertTrue(getAnnotations(null, Service.class).isEmpty());
        Assertions.assertTrue(getAnnotations(null, Service.class.getTypeName()).isEmpty());
    }

    @Test
    void testGetAllAnnotations() {

        List<AnnotationMirror> annotations = getAllAnnotations(testType);
        Assertions.assertEquals(4, annotations.size());

        annotations = getAllAnnotations(testType.asType(), annotation -> true);
        Assertions.assertEquals(4, annotations.size());

        annotations = getAllAnnotations(processingEnv, TestServiceImpl.class);
        Assertions.assertEquals(4, annotations.size());

        annotations = getAllAnnotations(testType.asType(), Service.class);
        Assertions.assertEquals(3, annotations.size());

        annotations = getAllAnnotations(testType, Override.class);
        Assertions.assertEquals(0, annotations.size());

        //        annotations = getAllAnnotations(testType.asType(), com.alibaba.dubbo.config.annotation.Service.class);
        //        assertEquals(2, annotations.size());

        Assertions.assertTrue(getAllAnnotations((Element) null, (Class) null).isEmpty());
        Assertions.assertTrue(
                getAllAnnotations((TypeMirror) null, (String) null).isEmpty());
        Assertions.assertTrue(
                getAllAnnotations((ProcessingEnvironment) null, (Class) null).isEmpty());
        Assertions.assertTrue(
                getAllAnnotations((ProcessingEnvironment) null, (String) null).isEmpty());

        Assertions.assertTrue(getAllAnnotations((Element) null).isEmpty());
        Assertions.assertTrue(getAllAnnotations((TypeMirror) null).isEmpty());
        Assertions.assertTrue(getAllAnnotations(processingEnv, (Class) null).isEmpty());
        Assertions.assertTrue(getAllAnnotations(processingEnv, (String) null).isEmpty());

        Assertions.assertTrue(getAllAnnotations(testType, (Class) null).isEmpty());
        Assertions.assertTrue(getAllAnnotations(testType.asType(), (Class) null).isEmpty());

        Assertions.assertTrue(getAllAnnotations(testType, (String) null).isEmpty());
        Assertions.assertTrue(
                getAllAnnotations(testType.asType(), (String) null).isEmpty());

        Assertions.assertTrue(getAllAnnotations((Element) null, Service.class).isEmpty());
        Assertions.assertTrue(getAllAnnotations((TypeMirror) null, Service.class.getTypeName())
                .isEmpty());
    }

    @Test
    void testFindAnnotation() {

        Assertions.assertEquals(
                "org.apache.dubbo.config.annotation.Service",
                findAnnotation(testType, Service.class).getAnnotationType().toString());
        //        assertEquals("com.alibaba.dubbo.config.annotation.Service", findAnnotation(testType,
        // com.alibaba.dubbo.config.annotation.Service.class).getAnnotationType().toString());
        Assertions.assertEquals(
                "javax.ws.rs.Path",
                findAnnotation(testType, Path.class).getAnnotationType().toString());
        Assertions.assertEquals(
                "javax.ws.rs.Path",
                findAnnotation(testType.asType(), Path.class)
                        .getAnnotationType()
                        .toString());
        Assertions.assertEquals(
                "javax.ws.rs.Path",
                findAnnotation(testType.asType(), Path.class.getTypeName())
                        .getAnnotationType()
                        .toString());
        Assertions.assertNull(findAnnotation(testType, Override.class));

        Assertions.assertNull(findAnnotation((Element) null, (Class) null));
        Assertions.assertNull(findAnnotation((Element) null, (String) null));
        Assertions.assertNull(findAnnotation((TypeMirror) null, (Class) null));
        Assertions.assertNull(findAnnotation((TypeMirror) null, (String) null));

        Assertions.assertNull(findAnnotation(testType, (Class) null));
        Assertions.assertNull(findAnnotation(testType, (String) null));
        Assertions.assertNull(findAnnotation(testType.asType(), (Class) null));
        Assertions.assertNull(findAnnotation(testType.asType(), (String) null));
    }

    @Test
    void testFindMetaAnnotation() {
        getAllDeclaredMethods(getType(TestService.class)).forEach(method -> {
            Assertions.assertEquals(
                    "javax.ws.rs.HttpMethod",
                    findMetaAnnotation(method, "javax.ws.rs.HttpMethod")
                            .getAnnotationType()
                            .toString());
        });
    }

    @Test
    void testGetAttribute() {
        Assertions.assertEquals(
                "org.apache.dubbo.metadata.tools.TestService",
                getAttribute(findAnnotation(testType, Service.class), "interfaceName"));
        Assertions.assertEquals(
                "org.apache.dubbo.metadata.tools.TestService",
                getAttribute(findAnnotation(testType, Service.class).getElementValues(), "interfaceName"));
        Assertions.assertEquals("/echo", getAttribute(findAnnotation(testType, Path.class), "value"));

        Assertions.assertNull(getAttribute(findAnnotation(testType, Path.class), null));
        Assertions.assertNull(getAttribute(findAnnotation(testType, (Class) null), null));
    }

    @Test
    void testGetValue() {
        AnnotationMirror pathAnnotation = getAnnotation(getType(TestService.class), Path.class);
        Assertions.assertEquals("/echo", getValue(pathAnnotation));
    }

    @Test
    void testIsAnnotationPresent() {
        Assertions.assertTrue(isAnnotationPresent(testType, "org.apache.dubbo.config.annotation.Service"));
        //        assertTrue(isAnnotationPresent(testType, "com.alibaba.dubbo.config.annotation.Service"));
        Assertions.assertTrue(isAnnotationPresent(testType, "javax.ws.rs.Path"));
    }
}
