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
import org.apache.dubbo.metadata.annotation.processing.model.ArrayTypeModel;
import org.apache.dubbo.metadata.annotation.processing.model.Color;
import org.apache.dubbo.metadata.annotation.processing.model.Model;
import org.apache.dubbo.metadata.tools.DefaultTestService;
import org.apache.dubbo.metadata.tools.GenericTestService;
import org.apache.dubbo.metadata.tools.TestServiceImpl;

import org.junit.jupiter.api.Test;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.apache.dubbo.metadata.annotation.processing.util.FieldUtils.findField;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.getAllInterfaces;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.getAllSuperTypes;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.getHierarchicalTypes;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.getInterfaces;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.getSuperType;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.isAnnotationType;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.isArrayType;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.isClassType;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.isDeclaredType;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.isEnumType;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.isInterfaceType;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.isSameType;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.isSimpleType;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.isTypeElement;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.listDeclaredTypes;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.listTypeElements;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.ofDeclaredType;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.ofTypeElement;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The {@link TypeUtils} Test
 *
 * @since 2.7.5
 */
public class TypeUtilsTest extends AbstractAnnotationProcessingTest {

    private TypeElement testType;

    @Override
    protected void addCompiledClasses(Set<Class<?>> classesToBeCompiled) {
        classesToBeCompiled.add(ArrayTypeModel.class);
        classesToBeCompiled.add(Color.class);
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
    public void testIsArrayType() {
        TypeElement type = getType(ArrayTypeModel.class);
        assertTrue(isArrayType(findField(type.asType(), "integers").asType()));
        assertTrue(isArrayType(findField(type.asType(), "strings").asType()));
        assertTrue(isArrayType(findField(type.asType(), "primitiveTypeModels").asType()));
        assertTrue(isArrayType(findField(type.asType(), "models").asType()));
        assertTrue(isArrayType(findField(type.asType(), "colors").asType()));

        assertFalse(isArrayType(null));
    }

    @Test
    public void testIsEnumType() {
        TypeElement type = getType(Color.class);
        assertTrue(isEnumType(type.asType()));

        type = getType(ArrayTypeModel.class);
        assertFalse(isEnumType(type.asType()));

        assertFalse(isEnumType(null));
    }

    @Test
    public void testIsClassType() {
        TypeElement type = getType(ArrayTypeModel.class);
        assertTrue(isClassType(type.asType()));

        type = getType(Model.class);
        assertTrue(isClassType(type.asType()));

        assertFalse(isClassType(null));
    }

    @Test
    public void testIsInterfaceType() {
        TypeElement type = getType(CharSequence.class);
        assertTrue(isInterfaceType(type.asType()));

        type = getType(Model.class);
        assertFalse(isInterfaceType(type.asType()));

        assertFalse(isInterfaceType(null));
    }

    @Test
    public void testIsAnnotationType() {
        TypeElement type = getType(Override.class);

        assertTrue(isAnnotationType(type.asType()));

        assertFalse(isAnnotationType(null));
    }


    @Test
    public void testDeclaredType() {
        TypeMirror type = testType.asType();
        assertTrue(isDeclaredType(type));
        assertEquals(type, ofDeclaredType(type));
        assertEquals(ofDeclaredType(type), ofDeclaredType(testType));

        assertFalse(isDeclaredType(null));
        assertFalse(isDeclaredType(types.getNullType()));
        assertFalse(isDeclaredType(types.getPrimitiveType(TypeKind.BYTE)));
        assertFalse(isDeclaredType(types.getArrayType(types.getPrimitiveType(TypeKind.BYTE))));
        assertNull(ofDeclaredType((Element) null));
    }

    @Test
    public void testTypeElement() {
        assertTrue(isTypeElement(testType));
        assertEquals(testType, ofTypeElement(testType));
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
}
