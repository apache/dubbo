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
import org.apache.dubbo.metadata.annotation.processing.model.PrimitiveTypeModel;
import org.apache.dubbo.metadata.tools.DefaultTestService;
import org.apache.dubbo.metadata.tools.GenericTestService;
import org.apache.dubbo.metadata.tools.TestServiceImpl;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.io.File;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.apache.dubbo.metadata.annotation.processing.util.FieldUtils.findField;
import static org.apache.dubbo.metadata.annotation.processing.util.FieldUtils.getDeclaredFields;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.getAllInterfaces;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.getAllSuperTypes;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.getHierarchicalTypes;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.getInterfaces;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.getResource;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.getResourceName;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.getSuperType;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.isAnnotationType;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.isArrayType;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.isClassType;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.isDeclaredType;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.isEnumType;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.isInterfaceType;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.isPrimitiveType;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.isSameType;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.isSimpleType;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.isTypeElement;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.listDeclaredTypes;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.listTypeElements;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.ofDeclaredType;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.ofDeclaredTypes;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.ofTypeElement;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The {@link TypeUtils} Test
 *
 * @since 2.7.6
 */
class TypeUtilsTest extends AbstractAnnotationProcessingTest {

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
    void testIsSimpleType() {

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
        assertTrue(isSimpleType(getType(Object.class)));

        assertFalse(isSimpleType(getType(getClass())));
        assertFalse(isSimpleType((TypeElement) null));
        assertFalse(isSimpleType((TypeMirror) null));
    }

    @Test
    void testIsSameType() {
        assertTrue(isSameType(getType(Void.class).asType(), "java.lang.Void"));
        assertFalse(isSameType(getType(String.class).asType(), "java.lang.Void"));

        assertFalse(isSameType(getType(Void.class).asType(), (Type) null));
        assertFalse(isSameType(null, (Type) null));

        assertFalse(isSameType(getType(Void.class).asType(), (String) null));
        assertFalse(isSameType(null, (String) null));
    }

    @Test
    void testIsArrayType() {
        TypeElement type = getType(ArrayTypeModel.class);
        assertTrue(isArrayType(findField(type.asType(), "integers").asType()));
        assertTrue(isArrayType(findField(type.asType(), "strings").asType()));
        assertTrue(isArrayType(findField(type.asType(), "primitiveTypeModels").asType()));
        assertTrue(isArrayType(findField(type.asType(), "models").asType()));
        assertTrue(isArrayType(findField(type.asType(), "colors").asType()));

        assertFalse(isArrayType((Element) null));
        assertFalse(isArrayType((TypeMirror) null));
    }

    @Test
    void testIsEnumType() {
        TypeElement type = getType(Color.class);
        assertTrue(isEnumType(type.asType()));

        type = getType(ArrayTypeModel.class);
        assertFalse(isEnumType(type.asType()));

        assertFalse(isEnumType((Element) null));
        assertFalse(isEnumType((TypeMirror) null));
    }

    @Test
    void testIsClassType() {
        TypeElement type = getType(ArrayTypeModel.class);
        assertTrue(isClassType(type.asType()));

        type = getType(Model.class);
        assertTrue(isClassType(type.asType()));

        assertFalse(isClassType((Element) null));
        assertFalse(isClassType((TypeMirror) null));
    }

    @Test
    void testIsPrimitiveType() {
        TypeElement type = getType(PrimitiveTypeModel.class);
        getDeclaredFields(type.asType())
                .stream()
                .map(VariableElement::asType)
                .forEach(t -> assertTrue(isPrimitiveType(t)));

        assertFalse(isPrimitiveType(getType(ArrayTypeModel.class)));

        assertFalse(isPrimitiveType((Element) null));
        assertFalse(isPrimitiveType((TypeMirror) null));
    }

    @Test
    void testIsInterfaceType() {
        TypeElement type = getType(CharSequence.class);
        assertTrue(isInterfaceType(type));
        assertTrue(isInterfaceType(type.asType()));

        type = getType(Model.class);
        assertFalse(isInterfaceType(type));
        assertFalse(isInterfaceType(type.asType()));

        assertFalse(isInterfaceType((Element) null));
        assertFalse(isInterfaceType((TypeMirror) null));
    }

    @Test
    void testIsAnnotationType() {
        TypeElement type = getType(Override.class);

        assertTrue(isAnnotationType(type));
        assertTrue(isAnnotationType(type.asType()));

        type = getType(Model.class);
        assertFalse(isAnnotationType(type));
        assertFalse(isAnnotationType(type.asType()));

        assertFalse(isAnnotationType((Element) null));
        assertFalse(isAnnotationType((TypeMirror) null));
    }

    @Test
    void testGetHierarchicalTypes() {
        Set hierarchicalTypes = getHierarchicalTypes(testType.asType(), true, true, true);
        Iterator iterator = hierarchicalTypes.iterator();
        assertEquals(8, hierarchicalTypes.size());
        assertEquals("org.apache.dubbo.metadata.tools.TestServiceImpl", iterator.next().toString());
        assertEquals("org.apache.dubbo.metadata.tools.GenericTestService", iterator.next().toString());
        assertEquals("org.apache.dubbo.metadata.tools.DefaultTestService", iterator.next().toString());
        assertEquals("java.lang.Object", iterator.next().toString());
        assertEquals("org.apache.dubbo.metadata.tools.TestService", iterator.next().toString());
        assertEquals("java.lang.AutoCloseable", iterator.next().toString());
        assertEquals("java.io.Serializable", iterator.next().toString());
        assertEquals("java.util.EventListener", iterator.next().toString());

        hierarchicalTypes = getHierarchicalTypes(testType);
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

        hierarchicalTypes = getHierarchicalTypes(testType.asType(), Object.class);
        iterator = hierarchicalTypes.iterator();
        assertEquals(7, hierarchicalTypes.size());
        assertEquals("org.apache.dubbo.metadata.tools.TestServiceImpl", iterator.next().toString());
        assertEquals("org.apache.dubbo.metadata.tools.GenericTestService", iterator.next().toString());
        assertEquals("org.apache.dubbo.metadata.tools.DefaultTestService", iterator.next().toString());
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

        assertTrue(getHierarchicalTypes((TypeElement) null).isEmpty());
        assertTrue(getHierarchicalTypes((TypeMirror) null).isEmpty());
    }


    @Test
    void testGetInterfaces() {
        TypeElement type = getType(Model.class);
        List<TypeMirror> interfaces = getInterfaces(type);
        assertTrue(interfaces.isEmpty());

        interfaces = getInterfaces(testType.asType());

        assertEquals(3, interfaces.size());
        assertEquals("org.apache.dubbo.metadata.tools.TestService", interfaces.get(0).toString());
        assertEquals("java.lang.AutoCloseable", interfaces.get(1).toString());
        assertEquals("java.io.Serializable", interfaces.get(2).toString());

        assertTrue(getInterfaces((TypeElement) null).isEmpty());
        assertTrue(getInterfaces((TypeMirror) null).isEmpty());
    }

    @Test
    void testGetAllInterfaces() {
        Set<? extends TypeMirror> interfaces = getAllInterfaces(testType.asType());
        assertEquals(4, interfaces.size());
        Iterator<? extends TypeMirror> iterator = interfaces.iterator();
        assertEquals("org.apache.dubbo.metadata.tools.TestService", iterator.next().toString());
        assertEquals("java.lang.AutoCloseable", iterator.next().toString());
        assertEquals("java.io.Serializable", iterator.next().toString());
        assertEquals("java.util.EventListener", iterator.next().toString());

        Set<TypeElement> allInterfaces = getAllInterfaces(testType);
        assertEquals(4, interfaces.size());

        Iterator<TypeElement> allIterator = allInterfaces.iterator();
        assertEquals("org.apache.dubbo.metadata.tools.TestService", allIterator.next().toString());
        assertEquals("java.lang.AutoCloseable", allIterator.next().toString());
        assertEquals("java.io.Serializable", allIterator.next().toString());
        assertEquals("java.util.EventListener", allIterator.next().toString());

        assertTrue(getAllInterfaces((TypeElement) null).isEmpty());
        assertTrue(getAllInterfaces((TypeMirror) null).isEmpty());
    }

    @Test
    void testGetType() {
        TypeElement element = TypeUtils.getType(processingEnv, String.class);
        assertEquals(element, TypeUtils.getType(processingEnv, element.asType()));
        assertEquals(element, TypeUtils.getType(processingEnv, "java.lang.String"));

        assertNull(TypeUtils.getType(processingEnv, (Type) null));
        assertNull(TypeUtils.getType(processingEnv, (TypeMirror) null));
        assertNull(TypeUtils.getType(processingEnv, (CharSequence) null));
        assertNull(TypeUtils.getType(null, (CharSequence) null));
    }

    @Test
    void testGetSuperType() {
        TypeElement gtsTypeElement = getSuperType(testType);
        assertEquals(gtsTypeElement, getType(GenericTestService.class));
        TypeElement dtsTypeElement = getSuperType(gtsTypeElement);
        assertEquals(dtsTypeElement, getType(DefaultTestService.class));

        TypeMirror gtsType = getSuperType(testType.asType());
        assertEquals(gtsType, getType(GenericTestService.class).asType());
        TypeMirror dtsType = getSuperType(gtsType);
        assertEquals(dtsType, getType(DefaultTestService.class).asType());

        assertNull(getSuperType((TypeElement) null));
        assertNull(getSuperType((TypeMirror) null));
    }

    @Test
    void testGetAllSuperTypes() {
        Set<?> allSuperTypes = getAllSuperTypes(testType);
        Iterator<?> iterator = allSuperTypes.iterator();
        assertEquals(3, allSuperTypes.size());
        assertEquals(iterator.next(), getType(GenericTestService.class));
        assertEquals(iterator.next(), getType(DefaultTestService.class));
        assertEquals(iterator.next(), getType(Object.class));

        allSuperTypes = getAllSuperTypes(testType);
        iterator = allSuperTypes.iterator();
        assertEquals(3, allSuperTypes.size());
        assertEquals(iterator.next(), getType(GenericTestService.class));
        assertEquals(iterator.next(), getType(DefaultTestService.class));
        assertEquals(iterator.next(), getType(Object.class));

        assertTrue(getAllSuperTypes((TypeElement) null).isEmpty());
        assertTrue(getAllSuperTypes((TypeMirror) null).isEmpty());
    }

    @Test
    void testIsDeclaredType() {
        assertTrue(isDeclaredType(testType));
        assertTrue(isDeclaredType(testType.asType()));
        assertFalse(isDeclaredType((Element) null));
        assertFalse(isDeclaredType((TypeMirror) null));
        assertFalse(isDeclaredType(types.getNullType()));
        assertFalse(isDeclaredType(types.getPrimitiveType(TypeKind.BYTE)));
        assertFalse(isDeclaredType(types.getArrayType(types.getPrimitiveType(TypeKind.BYTE))));
    }

    @Test
    void testOfDeclaredType() {
        assertEquals(testType.asType(), ofDeclaredType(testType));
        assertEquals(testType.asType(), ofDeclaredType(testType.asType()));
        assertEquals(ofDeclaredType(testType), ofDeclaredType(testType.asType()));

        assertNull(ofDeclaredType((Element) null));
        assertNull(ofDeclaredType((TypeMirror) null));
    }

    @Test
    void testIsTypeElement() {
        assertTrue(isTypeElement(testType));
        assertTrue(isTypeElement(testType.asType()));

        assertFalse(isTypeElement((Element) null));
        assertFalse(isTypeElement((TypeMirror) null));
    }

    @Test
    void testOfTypeElement() {
        assertEquals(testType, ofTypeElement(testType));
        assertEquals(testType, ofTypeElement(testType.asType()));

        assertNull(ofTypeElement((Element) null));
        assertNull(ofTypeElement((TypeMirror) null));
    }

    @Test
    void testOfDeclaredTypes() {
        Set<DeclaredType> declaredTypes = ofDeclaredTypes(asList(getType(String.class), getType(TestServiceImpl.class), getType(Color.class)));
        assertTrue(declaredTypes.contains(getType(String.class).asType()));
        assertTrue(declaredTypes.contains(getType(TestServiceImpl.class).asType()));
        assertTrue(declaredTypes.contains(getType(Color.class).asType()));

        assertTrue(ofDeclaredTypes(null).isEmpty());
    }

    @Test
    void testListDeclaredTypes() {
        List<DeclaredType> types = listDeclaredTypes(asList(testType, testType, testType));
        assertEquals(1, types.size());
        assertEquals(ofDeclaredType(testType), types.get(0));

        types = listDeclaredTypes(asList(new Element[]{null}));
        assertTrue(types.isEmpty());
    }

    @Test
    void testListTypeElements() {
        List<TypeElement> typeElements = listTypeElements(asList(testType.asType(), ofDeclaredType(testType)));
        assertEquals(1, typeElements.size());
        assertEquals(testType, typeElements.get(0));

        typeElements = listTypeElements(asList(types.getPrimitiveType(TypeKind.BYTE), types.getNullType(), types.getNoType(TypeKind.NONE)));
        assertTrue(typeElements.isEmpty());

        typeElements = listTypeElements(asList(new TypeMirror[]{null}));
        assertTrue(typeElements.isEmpty());

        typeElements = listTypeElements(null);
        assertTrue(typeElements.isEmpty());
    }

    @Test
    @Disabled
    public void testGetResource() throws URISyntaxException {
        URL resource = getResource(processingEnv, testType);
        assertNotNull(resource);
        assertTrue(new File(resource.toURI()).exists());
        assertEquals(resource, getResource(processingEnv, testType.asType()));
        assertEquals(resource, getResource(processingEnv, "org.apache.dubbo.metadata.tools.TestServiceImpl"));

        assertThrows(RuntimeException.class, () -> getResource(processingEnv, "NotFound"));
    }

    @Test
    void testGetResourceName() {
        assertEquals("java/lang/String.class", getResourceName("java.lang.String"));
        assertNull(getResourceName(null));
    }
}
