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

import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.StreamSupport.stream;
import static javax.lang.model.util.ElementFilter.fieldsIn;
import static javax.lang.model.util.ElementFilter.methodsIn;

/**
 * The utilities class for "javax.lang.model."
 *
 * @since 2.7.5
 */
public interface ModelUtils {

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

    static AnnotationMirror getAnnotation(AnnotatedConstruct annotatedConstruct,
                                          Class<? extends Annotation> annotationClass) {
        return getAnnotation(annotatedConstruct, annotationClass.getTypeName());
    }

    static AnnotationMirror getAnnotation(AnnotatedConstruct annotatedConstruct, CharSequence annotationClassName) {
        List<AnnotationMirror> annotations = getAnnotations(annotatedConstruct, annotationClassName);
        return annotations.isEmpty() ? null : annotations.get(0);
    }

    static List<AnnotationMirror> getAnnotations(AnnotatedConstruct annotatedConstruct,
                                                 CharSequence annotationClassName) {
        return annotatedConstruct.getAnnotationMirrors()
                .stream()
                .filter(annotation -> isSameType(annotation.getAnnotationType(), annotationClassName))
                .collect(Collectors.toList());
    }

    static <T> T getAttribute(AnnotationMirror annotation, String attributeName) {
        return annotation == null ? null : getAttribute(annotation.getElementValues(), attributeName);
    }

    static <T> T getAttribute(Map<? extends ExecutableElement, ? extends AnnotationValue> attributesMap,
                              String attributeName) {
        T attributeValue = null;
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : attributesMap.entrySet()) {
            ExecutableElement executableElement = entry.getKey();
            if (attributeName.equals(executableElement.getSimpleName().toString())) {
                attributeValue = (T) entry.getValue().getValue();
                break;
            }
        }
        return attributeValue;
    }

    static <T> T getValue(AnnotationMirror annotation) {
        return (T) getAttribute(annotation, "value");
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
        return type == null ? false : TypeKind.DECLARED.equals(type.getKind());
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
                .map(ModelUtils::ofTypeElement)
                .filter(Objects::nonNull)
                .map(Element::asType)
                .map(ModelUtils::ofDeclaredType)
                .filter(Objects::nonNull)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
    }

    static List<DeclaredType> listDeclaredTypes(Iterable<? extends Element> elements) {
        return new ArrayList<>(ofDeclaredTypes(elements));
    }

    static List<TypeElement> listTypeElements(Iterable<? extends TypeMirror> types) {
        return new ArrayList<>(ofTypeElements(types));
    }

    static Set<TypeElement> ofTypeElements(Iterable<? extends TypeMirror> types) {
        return stream(types.spliterator(), false)
                .map(ModelUtils::ofTypeElement)
                .filter(Objects::nonNull)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
    }

    static List<? extends Element> getDeclaredMembers(TypeMirror type) {
        TypeElement element = ofTypeElement(type);
        return element == null ? emptyList() : element.getEnclosedElements();
    }

    static List<? extends Element> getAllDeclaredMembers(TypeMirror type) {
        return getHierarchicalTypes(type)
                .stream()
                .map(ModelUtils::getDeclaredMembers)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    static List<VariableElement> getDeclaredFields(TypeMirror type) {
        return fieldsIn(getDeclaredMembers(type));
    }

    static List<VariableElement> getAllDeclaredFields(TypeMirror type) {
        return getHierarchicalTypes(type)
                .stream()
                .map(ModelUtils::getDeclaredFields)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    static List<ExecutableElement> getDeclaredMethods(TypeMirror type) {
        return methodsIn(getDeclaredMembers(type));
    }

    static List<ExecutableElement> getAllDeclaredMethods(TypeMirror type) {
        return getHierarchicalTypes(type)
                .stream()
                .map(ModelUtils::getDeclaredMethods)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }
}
