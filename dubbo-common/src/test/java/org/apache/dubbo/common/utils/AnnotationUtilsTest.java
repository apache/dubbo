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
package org.apache.dubbo.common.utils;

import org.apache.dubbo.common.extension.Adaptive;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.config.annotation.Service;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.apache.dubbo.common.utils.AnnotationUtils.excludedType;
import static org.apache.dubbo.common.utils.AnnotationUtils.filterDefaultValues;
import static org.apache.dubbo.common.utils.AnnotationUtils.findAnnotation;
import static org.apache.dubbo.common.utils.AnnotationUtils.findMetaAnnotation;
import static org.apache.dubbo.common.utils.AnnotationUtils.findMetaAnnotations;
import static org.apache.dubbo.common.utils.AnnotationUtils.getAllDeclaredAnnotations;
import static org.apache.dubbo.common.utils.AnnotationUtils.getAllMetaAnnotations;
import static org.apache.dubbo.common.utils.AnnotationUtils.getAnnotation;
import static org.apache.dubbo.common.utils.AnnotationUtils.getAttribute;
import static org.apache.dubbo.common.utils.AnnotationUtils.getAttributes;
import static org.apache.dubbo.common.utils.AnnotationUtils.getDeclaredAnnotations;
import static org.apache.dubbo.common.utils.AnnotationUtils.getDefaultValue;
import static org.apache.dubbo.common.utils.AnnotationUtils.getMetaAnnotations;
import static org.apache.dubbo.common.utils.AnnotationUtils.getValue;
import static org.apache.dubbo.common.utils.AnnotationUtils.isAnnotationPresent;
import static org.apache.dubbo.common.utils.AnnotationUtils.isAnyAnnotationPresent;
import static org.apache.dubbo.common.utils.AnnotationUtils.isSameType;
import static org.apache.dubbo.common.utils.AnnotationUtils.isType;
import static org.apache.dubbo.common.utils.MethodUtils.findMethod;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link AnnotationUtils} Test
 *
 * @since 2.7.6
 */
public class AnnotationUtilsTest {

    @Test
    public void testIsType() throws NoSuchMethodException {
        // null checking
        assertFalse(isType(null));
        // Method checking
        assertFalse(isType(findMethod(A.class, "execute")));
        // Class checking
        assertTrue(isType(A.class));
    }

    @Test
    public void testIsSameType() {
        assertTrue(isSameType(A.class.getAnnotation(Service.class), Service.class));
        assertFalse(isSameType(A.class.getAnnotation(Service.class), Deprecated.class));
        assertFalse(isSameType(A.class.getAnnotation(Service.class), null));
        assertFalse(isSameType(null, Deprecated.class));
        assertFalse(isSameType(null, null));
    }

    @Test
    public void testExcludedType() {
        assertFalse(excludedType(Service.class).test(A.class.getAnnotation(Service.class)));
        assertTrue(excludedType(Service.class).test(A.class.getAnnotation(Deprecated.class)));
    }

    @Test
    public void testGetAttribute() {
        Annotation annotation = A.class.getAnnotation(Service.class);
        assertEquals("java.lang.CharSequence", getAttribute(annotation, "interfaceName"));
        assertEquals(CharSequence.class, getAttribute(annotation, "interfaceClass"));
        assertEquals("", getAttribute(annotation, "version"));
        assertEquals("", getAttribute(annotation, "group"));
        assertEquals("", getAttribute(annotation, "path"));
        assertEquals(true, getAttribute(annotation, "export"));
        assertEquals(false, getAttribute(annotation, "deprecated"));
    }

    @Test
    public void testGetAttributesMap() {
        Annotation annotation = A.class.getAnnotation(Service.class);
        Map<String, Object> attributes = getAttributes(annotation, false);
        assertEquals("java.lang.CharSequence", attributes.get("interfaceName"));
        assertEquals(CharSequence.class, attributes.get("interfaceClass"));
        assertEquals("", attributes.get("group"));
        assertEquals(getDefaultValue(annotation, "export"), attributes.get("export"));

        Map<String, Object> filteredAttributes = filterDefaultValues(annotation, attributes);
        assertEquals(2, filteredAttributes.size());
        assertEquals("java.lang.CharSequence", filteredAttributes.get("interfaceName"));
        assertEquals(CharSequence.class, filteredAttributes.get("interfaceClass"));
        assertFalse(filteredAttributes.containsKey("group"));
        assertFalse(filteredAttributes.containsKey("export"));

        Map<String, Object> nonDefaultAttributes = getAttributes(annotation, true);
        assertEquals(nonDefaultAttributes, filteredAttributes);
    }

    @Test
    public void testGetValue() {
        Adaptive adaptive = A.class.getAnnotation(Adaptive.class);
        String[] value = getValue(adaptive);
        assertEquals(asList("a", "b", "c"), asList(value));
    }

    @Test
    public void testGetDeclaredAnnotations() {
        List<Annotation> annotations = getDeclaredAnnotations(A.class);
        assertADeclaredAnnotations(annotations, 0);

        annotations = getDeclaredAnnotations(A.class, a -> isSameType(a, Service.class));
        assertEquals(1, annotations.size());
        Service service = (Service) annotations.get(0);
        assertEquals("java.lang.CharSequence", service.interfaceName());
        assertEquals(CharSequence.class, service.interfaceClass());
    }

    @Test
    public void testGetAllDeclaredAnnotations() {
        List<Annotation> annotations = getAllDeclaredAnnotations(A.class);
        assertADeclaredAnnotations(annotations, 0);

        annotations = getAllDeclaredAnnotations(B.class);
        assertTrue(isSameType(annotations.get(0), Service5.class));
        assertADeclaredAnnotations(annotations, 1);

        annotations = new LinkedList<>(getAllDeclaredAnnotations(C.class));
        assertTrue(isSameType(annotations.get(0), MyAdaptive.class));
        assertTrue(isSameType(annotations.get(1), Service5.class));
        assertADeclaredAnnotations(annotations, 2);

        annotations = getAllDeclaredAnnotations(findMethod(A.class, "execute"));
        MyAdaptive myAdaptive = (MyAdaptive) annotations.get(0);
        assertArrayEquals(new String[]{"e"}, myAdaptive.value());

        annotations = getAllDeclaredAnnotations(findMethod(B.class, "execute"));
        Adaptive adaptive = (Adaptive) annotations.get(0);
        assertArrayEquals(new String[]{"f"}, adaptive.value());
    }

    @Test
    public void testGetMetaAnnotations() {
        List<Annotation> metaAnnotations = getMetaAnnotations(Service.class, a -> isSameType(a, Inherited.class));
        assertEquals(1, metaAnnotations.size());
        assertEquals(Inherited.class, metaAnnotations.get(0).annotationType());

        metaAnnotations = getMetaAnnotations(Service.class);
        HashSet<Object> set1 = new HashSet<>();
        metaAnnotations.forEach(t -> set1.add(t.annotationType()));
        HashSet<Object> set2 = new HashSet<>();
        set2.add(Inherited.class);
        set2.add(Deprecated.class);
        assertEquals(2, metaAnnotations.size());
        assertEquals(set1, set2);
    }

    @Test
    public void testGetAllMetaAnnotations() {
        List<Annotation> metaAnnotations = getAllMetaAnnotations(Service5.class);
        int offset = 0;

        HashSet<Object> set1 = new HashSet<>();
        metaAnnotations.forEach(t -> set1.add(t.annotationType()));
        HashSet<Object> set2 = new HashSet<>();
        set2.add(Inherited.class);
        set2.add(DubboService.class);
        set2.add(Service4.class);
        set2.add(Service3.class);
        set2.add(Service2.class);
        assertEquals(9, metaAnnotations.size());
        assertEquals(set1, set2);

        metaAnnotations = getAllMetaAnnotations(MyAdaptive.class);
        HashSet<Object> set3 = new HashSet<>();
        metaAnnotations.forEach(t -> set3.add(t.annotationType()));
        HashSet<Object> set4 = new HashSet<>();
        metaAnnotations.forEach(t -> set3.add(t.annotationType()));
        set4.add(Inherited.class);
        set4.add(Adaptive.class);
        assertEquals(2, metaAnnotations.size());
        assertEquals(set3, set4);
    }


    @Test
    public void testIsAnnotationPresent() {
        assertTrue(isAnnotationPresent(A.class, true, Service.class));
        assertTrue(isAnnotationPresent(A.class, true, Service.class, com.alibaba.dubbo.config.annotation.Service.class));
        assertTrue(isAnnotationPresent(A.class, Service.class));
        assertTrue(isAnnotationPresent(A.class, "org.apache.dubbo.config.annotation.Service"));
        assertTrue(AnnotationUtils.isAllAnnotationPresent(A.class, Service.class, Service.class, com.alibaba.dubbo.config.annotation.Service.class));
        assertTrue(isAnnotationPresent(A.class, Deprecated.class));
    }

    @Test
    public void testIsAnyAnnotationPresent() {
        assertTrue(isAnyAnnotationPresent(A.class, Service.class, com.alibaba.dubbo.config.annotation.Service.class, Deprecated.class));
        assertTrue(isAnyAnnotationPresent(A.class, Service.class, com.alibaba.dubbo.config.annotation.Service.class));
        assertTrue(isAnyAnnotationPresent(A.class, Service.class, Deprecated.class));
        assertTrue(isAnyAnnotationPresent(A.class, com.alibaba.dubbo.config.annotation.Service.class, Deprecated.class));
        assertTrue(isAnyAnnotationPresent(A.class, Service.class));
        assertTrue(isAnyAnnotationPresent(A.class, com.alibaba.dubbo.config.annotation.Service.class));
        assertTrue(isAnyAnnotationPresent(A.class, Deprecated.class));
    }

    @Test
    public void testGetAnnotation() {
        assertNotNull(getAnnotation(A.class, "org.apache.dubbo.config.annotation.Service"));
        assertNotNull(getAnnotation(A.class, "com.alibaba.dubbo.config.annotation.Service"));
        assertNotNull(getAnnotation(A.class, "org.apache.dubbo.common.extension.Adaptive"));
        assertNull(getAnnotation(A.class, "java.lang.Deprecated"));
        assertNull(getAnnotation(A.class, "java.lang.String"));
        assertNull(getAnnotation(A.class, "NotExistedClass"));
    }

    @Test
    public void testFindAnnotation() {
        Service service = findAnnotation(A.class, Service.class);
        assertEquals("java.lang.CharSequence", service.interfaceName());
        assertEquals(CharSequence.class, service.interfaceClass());

        service = findAnnotation(B.class, Service.class);
        assertEquals(CharSequence.class, service.interfaceClass());
    }

    @Test
    public void testFindMetaAnnotations() {
        List<DubboService> services = findMetaAnnotations(B.class, DubboService.class);
        assertEquals(1, services.size());

        DubboService service = services.get(0);
        assertEquals("", service.interfaceName());
        assertEquals(Cloneable.class, service.interfaceClass());

        services = findMetaAnnotations(Service5.class, DubboService.class);
        assertEquals(1, services.size());

        service = services.get(0);
        assertEquals("", service.interfaceName());
        assertEquals(Cloneable.class, service.interfaceClass());
    }

    @Test
    public void testFindMetaAnnotation() {
        DubboService service = findMetaAnnotation(B.class, DubboService.class);
        assertEquals(Cloneable.class, service.interfaceClass());

        service = findMetaAnnotation(B.class, "org.apache.dubbo.config.annotation.DubboService");
        assertEquals(Cloneable.class, service.interfaceClass());

        service = findMetaAnnotation(Service5.class, DubboService.class);
        assertEquals(Cloneable.class, service.interfaceClass());
    }

    @Service(interfaceName = "java.lang.CharSequence", interfaceClass = CharSequence.class)
    @com.alibaba.dubbo.config.annotation.Service(interfaceName = "java.lang.CharSequence", interfaceClass = CharSequence.class)
    @Adaptive(value = {"a", "b", "c"})
    static class A {

        @MyAdaptive("e")
        public void execute() {

        }


    }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    @Inherited
    @DubboService(interfaceClass = Cloneable.class)
    @interface Service2 {


    }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    @Inherited
    @Service2
    @interface Service3 {


    }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    @Inherited
    @Service3
    @interface Service4 {


    }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    @Inherited
    @Service4
    @interface Service5 {


    }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Inherited
    @Adaptive
    @interface MyAdaptive {

        String[] value() default {};

    }

    @Service5
    static class B extends A {

        @Adaptive("f")
        @Override
        public void execute() {

        }


    }

    @MyAdaptive
    static class C extends B {

    }

    private void assertADeclaredAnnotations(List<Annotation> annotations, int offset) {
        int size = 3 + offset;
        assertEquals(size, annotations.size());
        boolean apacheServiceFound = false;
        boolean alibabaServiceFound = false;
        boolean adaptiveFound = false;

        for (Annotation annotation: annotations) {
            if (!apacheServiceFound && (annotation instanceof Service)) {
                assertEquals("java.lang.CharSequence", ((Service)annotation).interfaceName());
                assertEquals(CharSequence.class, ((Service)annotation).interfaceClass());
                apacheServiceFound = true;
                continue;
            }
            if (!alibabaServiceFound && (annotation instanceof com.alibaba.dubbo.config.annotation.Service)) {
                assertEquals("java.lang.CharSequence", ((com.alibaba.dubbo.config.annotation.Service)annotation).interfaceName());
                assertEquals(CharSequence.class, ((com.alibaba.dubbo.config.annotation.Service)annotation).interfaceClass());
                alibabaServiceFound = true;
                continue;
            }
            if (!adaptiveFound && (annotation instanceof Adaptive)) {
                assertArrayEquals(new String[]{"a", "b", "c"}, ((Adaptive)annotation).value());
                adaptiveFound = true;
                continue;
            }
        }
        assertTrue(apacheServiceFound && alibabaServiceFound && adaptiveFound);
    }
}