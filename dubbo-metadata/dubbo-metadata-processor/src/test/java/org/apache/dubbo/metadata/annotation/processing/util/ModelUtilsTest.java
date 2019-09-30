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
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.ws.rs.Path;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static javax.lang.model.util.ElementFilter.fieldsIn;
import static org.apache.dubbo.metadata.annotation.processing.util.ModelUtils.getAllDeclaredFields;
import static org.apache.dubbo.metadata.annotation.processing.util.ModelUtils.getAllDeclaredMembers;
import static org.apache.dubbo.metadata.annotation.processing.util.ModelUtils.getAllDeclaredMethods;
import static org.apache.dubbo.metadata.annotation.processing.util.ModelUtils.getAllInterfaces;
import static org.apache.dubbo.metadata.annotation.processing.util.ModelUtils.getAllSuperTypes;
import static org.apache.dubbo.metadata.annotation.processing.util.ModelUtils.getAnnotation;
import static org.apache.dubbo.metadata.annotation.processing.util.ModelUtils.getAttribute;
import static org.apache.dubbo.metadata.annotation.processing.util.ModelUtils.getDeclaredFields;
import static org.apache.dubbo.metadata.annotation.processing.util.ModelUtils.getDeclaredMembers;
import static org.apache.dubbo.metadata.annotation.processing.util.ModelUtils.getDeclaredMethods;
import static org.apache.dubbo.metadata.annotation.processing.util.ModelUtils.getHierarchicalTypes;
import static org.apache.dubbo.metadata.annotation.processing.util.ModelUtils.getInterfaces;
import static org.apache.dubbo.metadata.annotation.processing.util.ModelUtils.getSuperType;
import static org.apache.dubbo.metadata.annotation.processing.util.ModelUtils.getValue;
import static org.apache.dubbo.metadata.annotation.processing.util.ModelUtils.isDeclaredType;
import static org.apache.dubbo.metadata.annotation.processing.util.ModelUtils.isSameType;
import static org.apache.dubbo.metadata.annotation.processing.util.ModelUtils.isSimpleType;
import static org.apache.dubbo.metadata.annotation.processing.util.ModelUtils.isTypeElement;
import static org.apache.dubbo.metadata.annotation.processing.util.ModelUtils.listDeclaredTypes;
import static org.apache.dubbo.metadata.annotation.processing.util.ModelUtils.listTypeElements;
import static org.apache.dubbo.metadata.annotation.processing.util.ModelUtils.ofDeclaredType;
import static org.apache.dubbo.metadata.annotation.processing.util.ModelUtils.ofTypeElement;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ModelUtils} Test
 *
 * @since 2.7.5
 */
public class ModelUtilsTest extends AbstractAnnotationProcessingTest {

    private TypeElement testType;

    @Override
    protected void addCompiledClasses(Set<Class<?>> classesToBeCompiled) {
    }

    @Override
    protected void beforeEach() {
        testType = getType(TestServiceImpl.class);
    }

    @Test
    public void testIsSimpleType() {

        assertTrue(isSimpleType(getType(Void.class)));
        assertTrue(isSimpleType(getType(Boolean.class)));
        assertTrue(isSimpleType(getType(Character.class)));
        assertTrue(isSimpleType(getType(Byte.class)));
        assertTrue(isSimpleType(getType(Short.class)));
        assertTrue(isSimpleType(getType(Integer.class)));
        assertTrue(isSimpleType(getType(Long.class)));
        assertTrue(isSimpleType(getType(Float.class)));
        assertTrue(isSimpleType(getType(Double.class)));
        assertTrue(isSimpleType(getType(String.class)));
        assertTrue(isSimpleType(getType(BigDecimal.class)));
        assertTrue(isSimpleType(getType(BigInteger.class)));
        assertTrue(isSimpleType(getType(Date.class)));

        assertFalse(isSimpleType(getType(getClass())));
    }

    @Test
    public void testIsSameType() {
        assertTrue(isSameType(getType(Void.class).asType(), "java.lang.Void"));
        assertFalse(isSameType(getType(String.class).asType(), "java.lang.Void"));
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
    public void testDeclaredType() {
        TypeMirror type = testType.asType();
        assertTrue(isDeclaredType(type));
        assertEquals(type, ModelUtils.ofDeclaredType(type));
        assertEquals(ModelUtils.ofDeclaredType(type), ofDeclaredType(testType));

        assertFalse(isDeclaredType(null));
        assertFalse(isDeclaredType(types.getNullType()));
        assertFalse(isDeclaredType(types.getPrimitiveType(TypeKind.BYTE)));
        assertFalse(isDeclaredType(types.getArrayType(types.getPrimitiveType(TypeKind.BYTE))));
        assertNull(ofDeclaredType((Element) null));
    }

    @Test
    public void testTypeElement() {
        assertTrue(isTypeElement(testType));
        assertEquals(testType, ModelUtils.ofTypeElement(testType));
        assertEquals(testType, ofTypeElement(testType.asType()));
    }

    @Test
    public void testListDeclaredTypes() {
        List<DeclaredType> types = listDeclaredTypes(asList(testType, testType, testType));
        assertEquals(1, types.size());
        assertEquals(ofDeclaredType(testType), types.get(0));

        types = listDeclaredTypes(asList(new Element[]{null}));
        assertTrue(types.isEmpty());
    }

    @Test
    public void testOfTypeElements() {
        List<TypeElement> typeElements = listTypeElements(asList(testType.asType(), ofDeclaredType(testType)));
        assertEquals(1, typeElements.size());
        assertEquals(testType, typeElements.get(0));

        typeElements = listTypeElements(asList(types.getPrimitiveType(TypeKind.BYTE), types.getNullType(), types.getNoType(TypeKind.NONE)));
        assertTrue(typeElements.isEmpty());

        typeElements = listTypeElements(asList(new TypeMirror[]{null}));
        assertTrue(typeElements.isEmpty());
    }


    @Test
    public void testGetSuperType() {
        TypeElement gtsTypeElement = getSuperType(testType);
        assertEquals(gtsTypeElement, getType(GenericTestService.class));
        TypeElement dtsTypeElement = getSuperType(gtsTypeElement);
        assertEquals(dtsTypeElement, getType(DefaultTestService.class));

        TypeMirror gtsType = getSuperType(testType.asType());
        assertEquals(gtsType, getType(GenericTestService.class).asType());
        TypeMirror dtsType = getSuperType(gtsType);
        assertEquals(dtsType, getType(DefaultTestService.class).asType());
    }

    @Test
    public void testGetAllSuperTypes() {
        List<TypeElement> allSuperTypes = getAllSuperTypes(testType);
        assertEquals(3, allSuperTypes.size());
        assertEquals(allSuperTypes.get(0), getType(GenericTestService.class));
        assertEquals(allSuperTypes.get(1), getType(DefaultTestService.class));
        assertEquals(allSuperTypes.get(2), getType(Object.class));
    }

    @Test
    public void testGetInterfaces() {
        List<? extends TypeMirror> interfaces = getInterfaces(testType.asType());
        assertEquals("org.apache.dubbo.metadata.tools.TestService", interfaces.get(0).toString());
        assertEquals("java.lang.AutoCloseable", interfaces.get(1).toString());
        assertEquals("java.io.Serializable", interfaces.get(2).toString());
    }

    @Test
    public void testGetAllInterfaces() {
        Set<? extends TypeMirror> interfaces = getAllInterfaces(testType.asType());
        Iterator<? extends TypeMirror> iterator = interfaces.iterator();
        assertEquals("org.apache.dubbo.metadata.tools.TestService", iterator.next().toString());
        assertEquals("java.lang.AutoCloseable", iterator.next().toString());
        assertEquals("java.io.Serializable", iterator.next().toString());
        assertEquals("java.util.EventListener", iterator.next().toString());
    }

    @Test
    public void testGetHierarchicalTypes() {
        Set<DeclaredType> hierarchicalTypes = getHierarchicalTypes(testType.asType(), true, true, true);
        Iterator<? extends TypeMirror> iterator = hierarchicalTypes.iterator();
        assertEquals(8, hierarchicalTypes.size());
        assertEquals("org.apache.dubbo.metadata.tools.TestServiceImpl", iterator.next().toString());
        assertEquals("org.apache.dubbo.metadata.tools.GenericTestService", iterator.next().toString());
        assertEquals("org.apache.dubbo.metadata.tools.DefaultTestService", iterator.next().toString());
        assertEquals("java.lang.Object", iterator.next().toString());
        assertEquals("org.apache.dubbo.metadata.tools.TestService", iterator.next().toString());
        assertEquals("java.lang.AutoCloseable", iterator.next().toString());
        assertEquals("java.io.Serializable", iterator.next().toString());
        assertEquals("java.util.EventListener", iterator.next().toString());

        hierarchicalTypes = getHierarchicalTypes(testType.asType());
        iterator = hierarchicalTypes.iterator();
        assertEquals(8, hierarchicalTypes.size());
        assertEquals("org.apache.dubbo.metadata.tools.TestServiceImpl", iterator.next().toString());
        assertEquals("org.apache.dubbo.metadata.tools.GenericTestService", iterator.next().toString());
        assertEquals("org.apache.dubbo.metadata.tools.DefaultTestService", iterator.next().toString());
        assertEquals("java.lang.Object", iterator.next().toString());
        assertEquals("org.apache.dubbo.metadata.tools.TestService", iterator.next().toString());
        assertEquals("java.lang.AutoCloseable", iterator.next().toString());
        assertEquals("java.io.Serializable", iterator.next().toString());
        assertEquals("java.util.EventListener", iterator.next().toString());

        hierarchicalTypes = getHierarchicalTypes(testType.asType(), true, true, false);
        iterator = hierarchicalTypes.iterator();
        assertEquals(4, hierarchicalTypes.size());
        assertEquals("org.apache.dubbo.metadata.tools.TestServiceImpl", iterator.next().toString());
        assertEquals("org.apache.dubbo.metadata.tools.GenericTestService", iterator.next().toString());
        assertEquals("org.apache.dubbo.metadata.tools.DefaultTestService", iterator.next().toString());
        assertEquals("java.lang.Object", iterator.next().toString());

        hierarchicalTypes = getHierarchicalTypes(testType.asType(), true, false, true);
        iterator = hierarchicalTypes.iterator();
        assertEquals(5, hierarchicalTypes.size());
        assertEquals("org.apache.dubbo.metadata.tools.TestServiceImpl", iterator.next().toString());
        assertEquals("org.apache.dubbo.metadata.tools.TestService", iterator.next().toString());
        assertEquals("java.lang.AutoCloseable", iterator.next().toString());
        assertEquals("java.io.Serializable", iterator.next().toString());
        assertEquals("java.util.EventListener", iterator.next().toString());

        hierarchicalTypes = getHierarchicalTypes(testType.asType(), false, false, true);
        iterator = hierarchicalTypes.iterator();
        assertEquals(4, hierarchicalTypes.size());
        assertEquals("org.apache.dubbo.metadata.tools.TestService", iterator.next().toString());
        assertEquals("java.lang.AutoCloseable", iterator.next().toString());
        assertEquals("java.io.Serializable", iterator.next().toString());
        assertEquals("java.util.EventListener", iterator.next().toString());

        hierarchicalTypes = getHierarchicalTypes(testType.asType(), true, false, false);
        iterator = hierarchicalTypes.iterator();
        assertEquals(1, hierarchicalTypes.size());
        assertEquals("org.apache.dubbo.metadata.tools.TestServiceImpl", iterator.next().toString());

        hierarchicalTypes = getHierarchicalTypes(testType.asType(), false, false, false);
        assertEquals(0, hierarchicalTypes.size());
    }

    @Test
    public void testDeclaredMembers() {
        TypeElement type = getType(Model.class);
        List<? extends Element> members = getDeclaredMembers(type.asType());
        List<VariableElement> fields = fieldsIn(members);
        assertEquals(19, members.size());
        assertEquals(6, fields.size());
        assertEquals("f", fields.get(0).getSimpleName().toString());
        assertEquals("d", fields.get(1).getSimpleName().toString());
        assertEquals("tu", fields.get(2).getSimpleName().toString());
        assertEquals("str", fields.get(3).getSimpleName().toString());
        assertEquals("bi", fields.get(4).getSimpleName().toString());
        assertEquals("bd", fields.get(5).getSimpleName().toString());

        members = getAllDeclaredMembers(type.asType());
        fields = fieldsIn(members);
        assertEquals(11, fields.size());
        assertEquals("f", fields.get(0).getSimpleName().toString());
        assertEquals("d", fields.get(1).getSimpleName().toString());
        assertEquals("tu", fields.get(2).getSimpleName().toString());
        assertEquals("str", fields.get(3).getSimpleName().toString());
        assertEquals("bi", fields.get(4).getSimpleName().toString());
        assertEquals("bd", fields.get(5).getSimpleName().toString());
        assertEquals("b", fields.get(6).getSimpleName().toString());
        assertEquals("s", fields.get(7).getSimpleName().toString());
        assertEquals("i", fields.get(8).getSimpleName().toString());
        assertEquals("l", fields.get(9).getSimpleName().toString());
        assertEquals("z", fields.get(10).getSimpleName().toString());
    }

    @Test
    public void testDeclaredFields() {
        TypeElement type = getType(Model.class);
        List<VariableElement> fields = getDeclaredFields(type.asType());
        assertEquals(6, fields.size());
        assertEquals("f", fields.get(0).getSimpleName().toString());
        assertEquals("d", fields.get(1).getSimpleName().toString());
        assertEquals("tu", fields.get(2).getSimpleName().toString());
        assertEquals("str", fields.get(3).getSimpleName().toString());
        assertEquals("bi", fields.get(4).getSimpleName().toString());
        assertEquals("bd", fields.get(5).getSimpleName().toString());

        fields = getAllDeclaredFields(type.asType());
        assertEquals(11, fields.size());
        assertEquals("f", fields.get(0).getSimpleName().toString());
        assertEquals("d", fields.get(1).getSimpleName().toString());
        assertEquals("tu", fields.get(2).getSimpleName().toString());
        assertEquals("str", fields.get(3).getSimpleName().toString());
        assertEquals("bi", fields.get(4).getSimpleName().toString());
        assertEquals("bd", fields.get(5).getSimpleName().toString());
        assertEquals("b", fields.get(6).getSimpleName().toString());
        assertEquals("s", fields.get(7).getSimpleName().toString());
        assertEquals("i", fields.get(8).getSimpleName().toString());
        assertEquals("l", fields.get(9).getSimpleName().toString());
        assertEquals("z", fields.get(10).getSimpleName().toString());
    }

    @Test
    public void testDeclaredMethods() {
        TypeElement type = getType(Model.class);
        List<ExecutableElement> methods = getDeclaredMethods(type.asType());
        assertEquals(12, methods.size());

        methods = getAllDeclaredMethods(type.asType());
        assertEquals(34, methods.size());
    }
}
