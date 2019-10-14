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

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.StreamSupport.stream;
import static javax.lang.model.element.ElementKind.ANNOTATION_TYPE;
import static javax.lang.model.element.ElementKind.CLASS;
import static javax.lang.model.element.ElementKind.ENUM;
import static javax.lang.model.element.ElementKind.INTERFACE;
import static javax.lang.model.type.TypeKind.DECLARED;

/**
 * The utilities class for type in the package "javax.lang.model.*"
 *
 * @since 2.7.5
 */
public interface TypeUtils {

    List<String> SIMPLE_TYPES = asList(
            Void.class.getName(),
            Boolean.class.getName(),
            Character.class.getName(),
            Byte.class.getName(),
            Short.class.getName(),
            Integer.class.getName(),
            Long.class.getName(),
            Float.class.getName(),
            Double.class.getName(),
            String.class.getName(),
            BigDecimal.class.getName(),
            BigInteger.class.getName(),
            Date.class.getName()
    );

    static boolean isSimpleType(Element element) {
        return isSimpleType(element.asType());
    }

    static boolean isSimpleType(TypeMirror type) {
        return SIMPLE_TYPES.contains(type.toString());
    }

    static boolean isSameType(TypeMirror type, CharSequence typeName) {
        return Objects.equals(type.toString(), typeName);
    }

    static boolean isArrayType(TypeMirror type) {
        return type != null && TypeKind.ARRAY.equals(type.getKind());
    }

    static boolean isEnumType(TypeMirror type) {
        DeclaredType declaredType = ofDeclaredType(type);
        return declaredType != null && ENUM.equals(declaredType.asElement().getKind());
    }

    static boolean isClassType(TypeMirror type) {
        DeclaredType declaredType = ofDeclaredType(type);
        return declaredType != null && CLASS.equals(declaredType.asElement().getKind());
    }

    static boolean isPrimitiveType(TypeMirror type) {
        return type != null && type.getKind().isPrimitive();
    }

    static boolean isInterfaceType(TypeMirror type) {
        DeclaredType declaredType = ofDeclaredType(type);
        return declaredType == null ? false : INTERFACE.equals(declaredType.asElement().getKind());
    }

    static boolean isAnnotationType(TypeMirror type) {
        DeclaredType declaredType = ofDeclaredType(type);
        return declaredType == null ? false : ANNOTATION_TYPE.equals(declaredType.asElement().getKind());
    }

    static Set<TypeElement> getHierarchicalTypes(TypeElement type) {
        return getHierarchicalTypes(type, true, true, true);
    }

    static Set<DeclaredType> getHierarchicalTypes(TypeMirror type) {
        return ofDeclaredTypes(getHierarchicalTypes(ofTypeElement(type)));
    }

    static Set<TypeElement> getHierarchicalTypes(Element element,
                                                 boolean includeSelf,
                                                 boolean includeSuperTypes,
                                                 boolean includeSuperInterfaces) {

        Set<TypeElement> hierarchicalTypes = new LinkedHashSet<>();

        if (includeSelf) {
            TypeElement current = ofTypeElement(element);
            hierarchicalTypes.add(current);
        }

        if (includeSuperTypes) {
            hierarchicalTypes.addAll(getAllSuperTypes(element));
        }

        if (includeSuperInterfaces) {
            hierarchicalTypes.addAll(getAllInterfaces(element));
        }

        return hierarchicalTypes;
    }

    static Set<DeclaredType> getHierarchicalTypes(TypeMirror type,
                                                  boolean includeSelf,
                                                  boolean includeSuperTypes,
                                                  boolean includeSuperInterfaces) {
        return ofDeclaredTypes(getHierarchicalTypes(ofTypeElement(type),
                includeSelf,
                includeSuperTypes,
                includeSuperInterfaces));
    }

    static List<? extends TypeMirror> getInterfaces(Element type) {
        TypeElement typeElement = ofTypeElement(type);
        return typeElement == null ? emptyList() : typeElement.getInterfaces();
    }

    static List<? extends TypeMirror> getInterfaces(TypeMirror type) {
        TypeElement currentType = ofTypeElement(type);
        return getInterfaces(currentType);
    }

    static Set<? extends TypeMirror> getAllInterfaces(TypeMirror type) {
        Set<TypeMirror> allInterfaces = new LinkedHashSet<>();
        getInterfaces(type).forEach(i -> {
            // Add current type's interfaces
            allInterfaces.add(i);
            // Add
            allInterfaces.addAll(getAllInterfaces(i));
        });
        // Add all super types' interfaces
        getAllSuperTypes(type).forEach(superType -> allInterfaces.addAll(getAllInterfaces(superType)));
        return allInterfaces;
    }

    static Set<TypeElement> getAllInterfaces(Element element) {
        return ofTypeElements(getAllInterfaces(element.asType()));
    }

    static TypeElement getSuperType(Element element) {
        TypeElement currentType = ofTypeElement(element);
        return currentType == null ? null : ofTypeElement(currentType.getSuperclass());
    }

    static DeclaredType getSuperType(TypeMirror type) {
        return ofDeclaredType(getSuperType(ofTypeElement(type)).asType());
    }

    static List<TypeElement> getAllSuperTypes(Element element) {
        List<TypeElement> allSuperTypes = new LinkedList<>();
        TypeElement superType = getSuperType(element);
        if (superType != null) {
            // add super type
            allSuperTypes.add(superType);
            // add ancestors' types
            allSuperTypes.addAll(getAllSuperTypes(superType));
        }
        return allSuperTypes;
    }

    static Set<DeclaredType> getAllSuperTypes(TypeMirror type) {
        return ofDeclaredTypes(getAllSuperTypes(ofTypeElement(type)));
    }

    static boolean isDeclaredType(TypeMirror type) {
        return type == null ? false :
                type instanceof DeclaredType ? true : DECLARED.equals(type.getKind());
    }

    static DeclaredType ofDeclaredType(Element element) {
        return element == null ? null : ofDeclaredType(element.asType());
    }

    static DeclaredType ofDeclaredType(TypeMirror type) {
        return isDeclaredType(type) ? DeclaredType.class.cast(type) : null;
    }

    static boolean isTypeElement(Element element) {
        return element instanceof TypeElement;
    }

    static TypeElement ofTypeElement(Element element) {
        return isTypeElement(element) ? TypeElement.class.cast(element) : null;
    }

    static TypeElement ofTypeElement(TypeMirror type) {
        DeclaredType declaredType = ofDeclaredType(type);
        if (declaredType != null) {
            return ofTypeElement(declaredType.asElement());
        }
        return null;
    }

    static Set<DeclaredType> ofDeclaredTypes(Iterable<? extends Element> elements) {
        return stream(elements.spliterator(), false)
                .map(TypeUtils::ofTypeElement)
                .filter(Objects::nonNull)
                .map(Element::asType)
                .map(TypeUtils::ofDeclaredType)
                .filter(Objects::nonNull)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
    }

    static Set<TypeElement> ofTypeElements(Iterable<? extends TypeMirror> types) {
        return stream(types.spliterator(), false)
                .map(TypeUtils::ofTypeElement)
                .filter(Objects::nonNull)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
    }

    static List<DeclaredType> listDeclaredTypes(Iterable<? extends Element> elements) {
        return new ArrayList<>(ofDeclaredTypes(elements));
    }

    static List<TypeElement> listTypeElements(Iterable<? extends TypeMirror> types) {
        return new ArrayList<>(ofTypeElements(types));
    }
}
