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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

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

        Assertions.assertTrue(isSimpleType(getType(Void.class)));
        Assertions.assertTrue(isSimpleType(getType(Boolean.class)));
        Assertions.assertTrue(isSimpleType(getType(Character.class)));
        Assertions.assertTrue(isSimpleType(getType(Byte.class)));
        Assertions.assertTrue(isSimpleType(getType(Short.class)));
        Assertions.assertTrue(isSimpleType(getType(Integer.class)));
        Assertions.assertTrue(isSimpleType(getType(Long.class)));
        Assertions.assertTrue(isSimpleType(getType(Float.class)));
        Assertions.assertTrue(isSimpleType(getType(Double.class)));
        Assertions.assertTrue(isSimpleType(getType(String.class)));
        Assertions.assertTrue(isSimpleType(getType(BigDecimal.class)));
        Assertions.assertTrue(isSimpleType(getType(BigInteger.class)));
        Assertions.assertTrue(isSimpleType(getType(Date.class)));
        Assertions.assertTrue(isSimpleType(getType(Object.class)));

        Assertions.assertFalse(isSimpleType(getType(getClass())));
        Assertions.assertFalse(isSimpleType((TypeElement) null));
        Assertions.assertFalse(isSimpleType((TypeMirror) null));
    }

    @Test
    void testIsSameType() {
        Assertions.assertTrue(isSameType(getType(Void.class).asType(), "java.lang.Void"));
        Assertions.assertFalse(isSameType(getType(String.class).asType(), "java.lang.Void"));

        Assertions.assertFalse(isSameType(getType(Void.class).asType(), (Type) null));
        Assertions.assertFalse(isSameType(null, (Type) null));

        Assertions.assertFalse(isSameType(getType(Void.class).asType(), (String) null));
        Assertions.assertFalse(isSameType(null, (String) null));
    }

    @Test
    void testIsArrayType() {
        TypeElement type = getType(ArrayTypeModel.class);
        Assertions.assertTrue(isArrayType(findField(type.asType(), "integers").asType()));
        Assertions.assertTrue(isArrayType(findField(type.asType(), "strings").asType()));
        Assertions.assertTrue(
                isArrayType(findField(type.asType(), "primitiveTypeModels").asType()));
        Assertions.assertTrue(isArrayType(findField(type.asType(), "models").asType()));
        Assertions.assertTrue(isArrayType(findField(type.asType(), "colors").asType()));

        Assertions.assertFalse(isArrayType((Element) null));
        Assertions.assertFalse(isArrayType((TypeMirror) null));
    }

    @Test
    void testIsEnumType() {
        TypeElement type = getType(Color.class);
        Assertions.assertTrue(isEnumType(type.asType()));

        type = getType(ArrayTypeModel.class);
        Assertions.assertFalse(isEnumType(type.asType()));

        Assertions.assertFalse(isEnumType((Element) null));
        Assertions.assertFalse(isEnumType((TypeMirror) null));
    }

    @Test
    void testIsClassType() {
        TypeElement type = getType(ArrayTypeModel.class);
        Assertions.assertTrue(isClassType(type.asType()));

        type = getType(Model.class);
        Assertions.assertTrue(isClassType(type.asType()));

        Assertions.assertFalse(isClassType((Element) null));
        Assertions.assertFalse(isClassType((TypeMirror) null));
    }

    @Test
    void testIsPrimitiveType() {
        TypeElement type = getType(PrimitiveTypeModel.class);
        getDeclaredFields(type.asType()).stream()
                .map(VariableElement::asType)
                .forEach(t -> Assertions.assertTrue(isPrimitiveType(t)));

        Assertions.assertFalse(isPrimitiveType(getType(ArrayTypeModel.class)));

        Assertions.assertFalse(isPrimitiveType((Element) null));
        Assertions.assertFalse(isPrimitiveType((TypeMirror) null));
    }

    @Test
    void testIsInterfaceType() {
        TypeElement type = getType(CharSequence.class);
        Assertions.assertTrue(isInterfaceType(type));
        Assertions.assertTrue(isInterfaceType(type.asType()));

        type = getType(Model.class);
        Assertions.assertFalse(isInterfaceType(type));
        Assertions.assertFalse(isInterfaceType(type.asType()));

        Assertions.assertFalse(isInterfaceType((Element) null));
        Assertions.assertFalse(isInterfaceType((TypeMirror) null));
    }

    @Test
    void testIsAnnotationType() {
        TypeElement type = getType(Override.class);

        Assertions.assertTrue(isAnnotationType(type));
        Assertions.assertTrue(isAnnotationType(type.asType()));

        type = getType(Model.class);
        Assertions.assertFalse(isAnnotationType(type));
        Assertions.assertFalse(isAnnotationType(type.asType()));

        Assertions.assertFalse(isAnnotationType((Element) null));
        Assertions.assertFalse(isAnnotationType((TypeMirror) null));
    }

    @Test
    void testGetHierarchicalTypes() {
        Set hierarchicalTypes = getHierarchicalTypes(testType.asType(), true, true, true);
        Iterator iterator = hierarchicalTypes.iterator();
        Assertions.assertEquals(8, hierarchicalTypes.size());
        Assertions.assertEquals(
                "org.apache.dubbo.metadata.tools.TestServiceImpl",
                iterator.next().toString());
        Assertions.assertEquals(
                "org.apache.dubbo.metadata.tools.GenericTestService",
                iterator.next().toString());
        Assertions.assertEquals(
                "org.apache.dubbo.metadata.tools.DefaultTestService",
                iterator.next().toString());
        Assertions.assertEquals("java.lang.Object", iterator.next().toString());
        Assertions.assertEquals(
                "org.apache.dubbo.metadata.tools.TestService", iterator.next().toString());
        Assertions.assertEquals("java.lang.AutoCloseable", iterator.next().toString());
        Assertions.assertEquals("java.io.Serializable", iterator.next().toString());
        Assertions.assertEquals("java.util.EventListener", iterator.next().toString());

        hierarchicalTypes = getHierarchicalTypes(testType);
        iterator = hierarchicalTypes.iterator();
        Assertions.assertEquals(8, hierarchicalTypes.size());
        Assertions.assertEquals(
                "org.apache.dubbo.metadata.tools.TestServiceImpl",
                iterator.next().toString());
        Assertions.assertEquals(
                "org.apache.dubbo.metadata.tools.GenericTestService",
                iterator.next().toString());
        Assertions.assertEquals(
                "org.apache.dubbo.metadata.tools.DefaultTestService",
                iterator.next().toString());
        Assertions.assertEquals("java.lang.Object", iterator.next().toString());
        Assertions.assertEquals(
                "org.apache.dubbo.metadata.tools.TestService", iterator.next().toString());
        Assertions.assertEquals("java.lang.AutoCloseable", iterator.next().toString());
        Assertions.assertEquals("java.io.Serializable", iterator.next().toString());
        Assertions.assertEquals("java.util.EventListener", iterator.next().toString());

        hierarchicalTypes = getHierarchicalTypes(testType.asType(), Object.class);
        iterator = hierarchicalTypes.iterator();
        Assertions.assertEquals(7, hierarchicalTypes.size());
        Assertions.assertEquals(
                "org.apache.dubbo.metadata.tools.TestServiceImpl",
                iterator.next().toString());
        Assertions.assertEquals(
                "org.apache.dubbo.metadata.tools.GenericTestService",
                iterator.next().toString());
        Assertions.assertEquals(
                "org.apache.dubbo.metadata.tools.DefaultTestService",
                iterator.next().toString());
        Assertions.assertEquals(
                "org.apache.dubbo.metadata.tools.TestService", iterator.next().toString());
        Assertions.assertEquals("java.lang.AutoCloseable", iterator.next().toString());
        Assertions.assertEquals("java.io.Serializable", iterator.next().toString());
        Assertions.assertEquals("java.util.EventListener", iterator.next().toString());

        hierarchicalTypes = getHierarchicalTypes(testType.asType(), true, true, false);
        iterator = hierarchicalTypes.iterator();
        Assertions.assertEquals(4, hierarchicalTypes.size());
        Assertions.assertEquals(
                "org.apache.dubbo.metadata.tools.TestServiceImpl",
                iterator.next().toString());
        Assertions.assertEquals(
                "org.apache.dubbo.metadata.tools.GenericTestService",
                iterator.next().toString());
        Assertions.assertEquals(
                "org.apache.dubbo.metadata.tools.DefaultTestService",
                iterator.next().toString());
        Assertions.assertEquals("java.lang.Object", iterator.next().toString());

        hierarchicalTypes = getHierarchicalTypes(testType.asType(), true, false, true);
        iterator = hierarchicalTypes.iterator();
        Assertions.assertEquals(5, hierarchicalTypes.size());
        Assertions.assertEquals(
                "org.apache.dubbo.metadata.tools.TestServiceImpl",
                iterator.next().toString());
        Assertions.assertEquals(
                "org.apache.dubbo.metadata.tools.TestService", iterator.next().toString());
        Assertions.assertEquals("java.lang.AutoCloseable", iterator.next().toString());
        Assertions.assertEquals("java.io.Serializable", iterator.next().toString());
        Assertions.assertEquals("java.util.EventListener", iterator.next().toString());

        hierarchicalTypes = getHierarchicalTypes(testType.asType(), false, false, true);
        iterator = hierarchicalTypes.iterator();
        Assertions.assertEquals(4, hierarchicalTypes.size());
        Assertions.assertEquals(
                "org.apache.dubbo.metadata.tools.TestService", iterator.next().toString());
        Assertions.assertEquals("java.lang.AutoCloseable", iterator.next().toString());
        Assertions.assertEquals("java.io.Serializable", iterator.next().toString());
        Assertions.assertEquals("java.util.EventListener", iterator.next().toString());

        hierarchicalTypes = getHierarchicalTypes(testType.asType(), true, false, false);
        iterator = hierarchicalTypes.iterator();
        Assertions.assertEquals(1, hierarchicalTypes.size());
        Assertions.assertEquals(
                "org.apache.dubbo.metadata.tools.TestServiceImpl",
                iterator.next().toString());

        hierarchicalTypes = getHierarchicalTypes(testType.asType(), false, false, false);
        Assertions.assertEquals(0, hierarchicalTypes.size());

        Assertions.assertTrue(getHierarchicalTypes((TypeElement) null).isEmpty());
        Assertions.assertTrue(getHierarchicalTypes((TypeMirror) null).isEmpty());
    }

    @Test
    void testGetInterfaces() {
        TypeElement type = getType(Model.class);
        List<TypeMirror> interfaces = getInterfaces(type);
        Assertions.assertTrue(interfaces.isEmpty());

        interfaces = getInterfaces(testType.asType());

        Assertions.assertEquals(3, interfaces.size());
        Assertions.assertEquals(
                "org.apache.dubbo.metadata.tools.TestService", interfaces.get(0).toString());
        Assertions.assertEquals("java.lang.AutoCloseable", interfaces.get(1).toString());
        Assertions.assertEquals("java.io.Serializable", interfaces.get(2).toString());

        Assertions.assertTrue(getInterfaces((TypeElement) null).isEmpty());
        Assertions.assertTrue(getInterfaces((TypeMirror) null).isEmpty());
    }

    @Test
    void testGetAllInterfaces() {
        Set<? extends TypeMirror> interfaces = getAllInterfaces(testType.asType());
        Assertions.assertEquals(4, interfaces.size());
        Iterator<? extends TypeMirror> iterator = interfaces.iterator();
        Assertions.assertEquals(
                "org.apache.dubbo.metadata.tools.TestService", iterator.next().toString());
        Assertions.assertEquals("java.lang.AutoCloseable", iterator.next().toString());
        Assertions.assertEquals("java.io.Serializable", iterator.next().toString());
        Assertions.assertEquals("java.util.EventListener", iterator.next().toString());

        Set<TypeElement> allInterfaces = getAllInterfaces(testType);
        Assertions.assertEquals(4, interfaces.size());

        Iterator<TypeElement> allIterator = allInterfaces.iterator();
        Assertions.assertEquals(
                "org.apache.dubbo.metadata.tools.TestService",
                allIterator.next().toString());
        Assertions.assertEquals("java.lang.AutoCloseable", allIterator.next().toString());
        Assertions.assertEquals("java.io.Serializable", allIterator.next().toString());
        Assertions.assertEquals("java.util.EventListener", allIterator.next().toString());

        Assertions.assertTrue(getAllInterfaces((TypeElement) null).isEmpty());
        Assertions.assertTrue(getAllInterfaces((TypeMirror) null).isEmpty());
    }

    @Test
    void testGetType() {
        TypeElement element = TypeUtils.getType(processingEnv, String.class);
        Assertions.assertEquals(element, TypeUtils.getType(processingEnv, element.asType()));
        Assertions.assertEquals(element, TypeUtils.getType(processingEnv, "java.lang.String"));

        Assertions.assertNull(TypeUtils.getType(processingEnv, (Type) null));
        Assertions.assertNull(TypeUtils.getType(processingEnv, (TypeMirror) null));
        Assertions.assertNull(TypeUtils.getType(processingEnv, (CharSequence) null));
        Assertions.assertNull(TypeUtils.getType(null, (CharSequence) null));
    }

    @Test
    void testGetSuperType() {
        TypeElement gtsTypeElement = getSuperType(testType);
        Assertions.assertEquals(gtsTypeElement, getType(GenericTestService.class));
        TypeElement dtsTypeElement = getSuperType(gtsTypeElement);
        Assertions.assertEquals(dtsTypeElement, getType(DefaultTestService.class));

        TypeMirror gtsType = getSuperType(testType.asType());
        Assertions.assertEquals(gtsType, getType(GenericTestService.class).asType());
        TypeMirror dtsType = getSuperType(gtsType);
        Assertions.assertEquals(dtsType, getType(DefaultTestService.class).asType());

        Assertions.assertNull(getSuperType((TypeElement) null));
        Assertions.assertNull(getSuperType((TypeMirror) null));
    }

    @Test
    void testGetAllSuperTypes() {
        Set<?> allSuperTypes = getAllSuperTypes(testType);
        Iterator<?> iterator = allSuperTypes.iterator();
        Assertions.assertEquals(3, allSuperTypes.size());
        Assertions.assertEquals(iterator.next(), getType(GenericTestService.class));
        Assertions.assertEquals(iterator.next(), getType(DefaultTestService.class));
        Assertions.assertEquals(iterator.next(), getType(Object.class));

        allSuperTypes = getAllSuperTypes(testType);
        iterator = allSuperTypes.iterator();
        Assertions.assertEquals(3, allSuperTypes.size());
        Assertions.assertEquals(iterator.next(), getType(GenericTestService.class));
        Assertions.assertEquals(iterator.next(), getType(DefaultTestService.class));
        Assertions.assertEquals(iterator.next(), getType(Object.class));

        Assertions.assertTrue(getAllSuperTypes((TypeElement) null).isEmpty());
        Assertions.assertTrue(getAllSuperTypes((TypeMirror) null).isEmpty());
    }

    @Test
    void testIsDeclaredType() {
        Assertions.assertTrue(isDeclaredType(testType));
        Assertions.assertTrue(isDeclaredType(testType.asType()));
        Assertions.assertFalse(isDeclaredType((Element) null));
        Assertions.assertFalse(isDeclaredType((TypeMirror) null));
        Assertions.assertFalse(isDeclaredType(types.getNullType()));
        Assertions.assertFalse(isDeclaredType(types.getPrimitiveType(TypeKind.BYTE)));
        Assertions.assertFalse(isDeclaredType(types.getArrayType(types.getPrimitiveType(TypeKind.BYTE))));
    }

    @Test
    void testOfDeclaredType() {
        Assertions.assertEquals(testType.asType(), ofDeclaredType(testType));
        Assertions.assertEquals(testType.asType(), ofDeclaredType(testType.asType()));
        Assertions.assertEquals(ofDeclaredType(testType), ofDeclaredType(testType.asType()));

        Assertions.assertNull(ofDeclaredType((Element) null));
        Assertions.assertNull(ofDeclaredType((TypeMirror) null));
    }

    @Test
    void testIsTypeElement() {
        Assertions.assertTrue(isTypeElement(testType));
        Assertions.assertTrue(isTypeElement(testType.asType()));

        Assertions.assertFalse(isTypeElement((Element) null));
        Assertions.assertFalse(isTypeElement((TypeMirror) null));
    }

    @Test
    void testOfTypeElement() {
        Assertions.assertEquals(testType, ofTypeElement(testType));
        Assertions.assertEquals(testType, ofTypeElement(testType.asType()));

        Assertions.assertNull(ofTypeElement((Element) null));
        Assertions.assertNull(ofTypeElement((TypeMirror) null));
    }

    @Test
    void testOfDeclaredTypes() {
        Set<DeclaredType> declaredTypes =
                ofDeclaredTypes(asList(getType(String.class), getType(TestServiceImpl.class), getType(Color.class)));
        Assertions.assertTrue(declaredTypes.contains(getType(String.class).asType()));
        Assertions.assertTrue(
                declaredTypes.contains(getType(TestServiceImpl.class).asType()));
        Assertions.assertTrue(declaredTypes.contains(getType(Color.class).asType()));

        Assertions.assertTrue(ofDeclaredTypes(null).isEmpty());
    }

    @Test
    void testListDeclaredTypes() {
        List<DeclaredType> types = listDeclaredTypes(asList(testType, testType, testType));
        Assertions.assertEquals(1, types.size());
        Assertions.assertEquals(ofDeclaredType(testType), types.get(0));

        types = listDeclaredTypes(asList(new Element[] {null}));
        Assertions.assertTrue(types.isEmpty());
    }

    @Test
    void testListTypeElements() {
        List<TypeElement> typeElements = listTypeElements(asList(testType.asType(), ofDeclaredType(testType)));
        Assertions.assertEquals(1, typeElements.size());
        Assertions.assertEquals(testType, typeElements.get(0));

        typeElements = listTypeElements(
                asList(types.getPrimitiveType(TypeKind.BYTE), types.getNullType(), types.getNoType(TypeKind.NONE)));
        Assertions.assertTrue(typeElements.isEmpty());

        typeElements = listTypeElements(asList(new TypeMirror[] {null}));
        Assertions.assertTrue(typeElements.isEmpty());

        typeElements = listTypeElements(null);
        Assertions.assertTrue(typeElements.isEmpty());
    }

    @Test
    @Disabled
    public void testGetResource() throws URISyntaxException {
        URL resource = getResource(processingEnv, testType);
        Assertions.assertNotNull(resource);
        Assertions.assertTrue(new File(resource.toURI()).exists());
        Assertions.assertEquals(resource, getResource(processingEnv, testType.asType()));
        Assertions.assertEquals(
                resource, getResource(processingEnv, "org.apache.dubbo.metadata.tools.TestServiceImpl"));

        Assertions.assertThrows(RuntimeException.class, () -> getResource(processingEnv, "NotFound"));
    }

    @Test
    void testGetResourceName() {
        Assertions.assertEquals("java/lang/String.class", getResourceName("java.lang.String"));
        Assertions.assertNull(getResourceName(null));
    }
}
