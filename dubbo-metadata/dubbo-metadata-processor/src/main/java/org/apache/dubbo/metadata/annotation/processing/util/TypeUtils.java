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

import org.apache.dubbo.common.utils.ClassUtils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import static java.lang.String.valueOf;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.of;
import static java.util.stream.StreamSupport.stream;
import static javax.lang.model.element.ElementKind.ANNOTATION_TYPE;
import static javax.lang.model.element.ElementKind.CLASS;
import static javax.lang.model.element.ElementKind.ENUM;
import static javax.lang.model.element.ElementKind.INTERFACE;
import static org.apache.dubbo.common.function.Predicates.EMPTY_ARRAY;
import static org.apache.dubbo.common.function.Streams.filterAll;
import static org.apache.dubbo.common.function.Streams.filterFirst;
import static org.apache.dubbo.common.utils.MethodUtils.invokeMethod;

/**
 * The utilities class for type in the package "javax.lang.model.*"
 *
 * @since 2.7.6
 */
public interface TypeUtils {

    List<String> SIMPLE_TYPES = asList(
            ClassUtils.SIMPLE_TYPES
                    .stream()
                    .map(Class::getName)
                    .toArray(String[]::new)
    );

    static boolean isSimpleType(Element element) {
        return element != null && isSimpleType(element.asType());
    }

    static boolean isSimpleType(TypeMirror type) {
        return type != null && SIMPLE_TYPES.contains(type.toString());
    }

    static boolean isSameType(TypeMirror type, CharSequence typeName) {
        if (type == null || typeName == null) {
            return false;
        }
        return Objects.equals(valueOf(type), valueOf(typeName));
    }

    static boolean isSameType(TypeMirror typeMirror, Type type) {
        return type != null && isSameType(typeMirror, type.getTypeName());
    }

    static boolean isArrayType(TypeMirror type) {
        return type != null && TypeKind.ARRAY.equals(type.getKind());
    }

    static boolean isArrayType(Element element) {
        return element != null && isArrayType(element.asType());
    }

    static boolean isEnumType(TypeMirror type) {
        DeclaredType declaredType = ofDeclaredType(type);
        return declaredType != null && ENUM.equals(declaredType.asElement().getKind());
    }

    static boolean isEnumType(Element element) {
        return element != null && isEnumType(element.asType());
    }

    static boolean isClassType(TypeMirror type) {
        DeclaredType declaredType = ofDeclaredType(type);
        return declaredType != null && isClassType(declaredType.asElement());
    }

    static boolean isClassType(Element element) {
        return element != null && CLASS.equals(element.getKind());
    }

    static boolean isPrimitiveType(TypeMirror type) {
        return type != null && type.getKind().isPrimitive();
    }

    static boolean isPrimitiveType(Element element) {
        return element != null && isPrimitiveType(element.asType());
    }

    static boolean isInterfaceType(TypeMirror type) {
        DeclaredType declaredType = ofDeclaredType(type);
        return declaredType != null && isInterfaceType(declaredType.asElement());
    }

    static boolean isInterfaceType(Element element) {
        return element != null && INTERFACE.equals(element.getKind());
    }

    static boolean isAnnotationType(TypeMirror type) {
        DeclaredType declaredType = ofDeclaredType(type);
        return declaredType != null && isAnnotationType(declaredType.asElement());
    }

    static boolean isAnnotationType(Element element) {
        return element != null && ANNOTATION_TYPE.equals(element.getKind());
    }

    static Set<TypeElement> getHierarchicalTypes(TypeElement type) {
        return getHierarchicalTypes(type, true, true, true);
    }

    static Set<DeclaredType> getHierarchicalTypes(TypeMirror type) {
        return getHierarchicalTypes(type, EMPTY_ARRAY);
    }

    static Set<DeclaredType> getHierarchicalTypes(TypeMirror type, Predicate<DeclaredType>... typeFilters) {
        return filterAll(ofDeclaredTypes(getHierarchicalTypes(ofTypeElement(type))), typeFilters);
    }

    static Set<DeclaredType> getHierarchicalTypes(TypeMirror type, Type... excludedTypes) {
        return getHierarchicalTypes(type, of(excludedTypes).map(Type::getTypeName).toArray(String[]::new));
    }

    static Set<DeclaredType> getHierarchicalTypes(TypeMirror type, CharSequence... excludedTypeNames) {
        Set<String> typeNames = of(excludedTypeNames).map(CharSequence::toString).collect(toSet());
        return getHierarchicalTypes(type, t -> !typeNames.contains(t.toString()));
    }

    static Set<TypeElement> getHierarchicalTypes(TypeElement type,
                                                 boolean includeSelf,
                                                 boolean includeSuperTypes,
                                                 boolean includeSuperInterfaces,
                                                 Predicate<TypeElement>... typeFilters) {

        if (type == null) {
            return emptySet();
        }

        Set<TypeElement> hierarchicalTypes = new LinkedHashSet<>();

        if (includeSelf) {
            hierarchicalTypes.add(type);
        }

        if (includeSuperTypes) {
            hierarchicalTypes.addAll(getAllSuperTypes(type));
        }

        if (includeSuperInterfaces) {
            hierarchicalTypes.addAll(getAllInterfaces(type));
        }

        return filterAll(hierarchicalTypes, typeFilters);
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

    static List<TypeMirror> getInterfaces(TypeElement type, Predicate<TypeMirror>... interfaceFilters) {
        return type == null ? emptyList() : filterAll((List<TypeMirror>) ofTypeElement(type).getInterfaces(), interfaceFilters);
    }

    static List<TypeMirror> getInterfaces(TypeMirror type, Predicate<TypeMirror>... interfaceFilters) {
        return getInterfaces(ofTypeElement(type), interfaceFilters);
    }

    static Set<TypeElement> getAllInterfaces(TypeElement type, Predicate<TypeElement>... interfaceFilters) {
        return type == null ? emptySet() : filterAll(ofTypeElements(getAllInterfaces(type.asType())), interfaceFilters);
    }

    static Set<? extends TypeMirror> getAllInterfaces(TypeMirror type, Predicate<TypeMirror>... interfaceFilters) {
        if (type == null) {
            return emptySet();
        }
        Set<TypeMirror> allInterfaces = new LinkedHashSet<>();
        getInterfaces(type).forEach(i -> {
            // Add current type's interfaces
            allInterfaces.add(i);
            // Add
            allInterfaces.addAll(getAllInterfaces(i));
        });
        // Add all super types' interfaces
        getAllSuperTypes(type).forEach(superType -> allInterfaces.addAll(getAllInterfaces(superType)));
        return filterAll(allInterfaces, interfaceFilters);
    }

    static TypeMirror findInterface(TypeMirror type, CharSequence interfaceClassName) {
        return filterFirst(getAllInterfaces(type), t -> isSameType(t, interfaceClassName));
    }

    static TypeElement getType(ProcessingEnvironment processingEnv, Type type) {
        return type == null ? null : getType(processingEnv, type.getTypeName());
    }

    static TypeElement getType(ProcessingEnvironment processingEnv, TypeMirror type) {
        return type == null ? null : getType(processingEnv, type.toString());
    }

    static TypeElement getType(ProcessingEnvironment processingEnv, CharSequence typeName) {
        if (processingEnv == null || typeName == null) {
            return null;
        }
        Elements elements = processingEnv.getElementUtils();
        return elements.getTypeElement(typeName);
    }

    static TypeElement getSuperType(TypeElement type) {
        return type == null ? null : ofTypeElement(type.getSuperclass());
    }

    static DeclaredType getSuperType(TypeMirror type) {
        TypeElement superType = getSuperType(ofTypeElement(type));
        return superType == null ? null : ofDeclaredType(superType.asType());
    }

    static Set<TypeElement> getAllSuperTypes(TypeElement type) {
        return getAllSuperTypes(type, EMPTY_ARRAY);
    }

    static Set<TypeElement> getAllSuperTypes(TypeElement type, Predicate<TypeElement>... typeFilters) {
        if (type == null) {
            return emptySet();
        }

        Set<TypeElement> allSuperTypes = new LinkedHashSet<>();
        TypeElement superType = getSuperType(type);
        if (superType != null) {
            // add super type
            allSuperTypes.add(superType);
            // add ancestors' types
            allSuperTypes.addAll(getAllSuperTypes(superType));
        }
        return filterAll(allSuperTypes, typeFilters);
    }

    static Set<DeclaredType> getAllSuperTypes(TypeMirror type) {
        return getAllSuperTypes(type, EMPTY_ARRAY);
    }

    static Set<DeclaredType> getAllSuperTypes(TypeMirror type, Predicate<DeclaredType>... typeFilters) {
        return filterAll(ofDeclaredTypes(getAllSuperTypes(ofTypeElement(type))), typeFilters);
    }

    static boolean isDeclaredType(Element element) {
        return element != null && isDeclaredType(element.asType());
    }

    static boolean isDeclaredType(TypeMirror type) {
        return type instanceof DeclaredType;
    }

    static DeclaredType ofDeclaredType(Element element) {
        return element == null ? null : ofDeclaredType(element.asType());
    }

    static DeclaredType ofDeclaredType(TypeMirror type) {
        return isDeclaredType(type) ? (DeclaredType) type : null;
    }

    static boolean isTypeElement(Element element) {
        return element instanceof TypeElement;
    }

    static boolean isTypeElement(TypeMirror type) {
        DeclaredType declaredType = ofDeclaredType(type);
        return declaredType != null && isTypeElement(declaredType.asElement());
    }

    static TypeElement ofTypeElement(Element element) {
        return isTypeElement(element) ? (TypeElement) element : null;
    }

    static TypeElement ofTypeElement(TypeMirror type) {
        DeclaredType declaredType = ofDeclaredType(type);
        if (declaredType != null) {
            return ofTypeElement(declaredType.asElement());
        }
        return null;
    }

    static Set<DeclaredType> ofDeclaredTypes(Iterable<? extends Element> elements) {
        return elements == null ?
                emptySet() :
                stream(elements.spliterator(), false)
                        .map(TypeUtils::ofTypeElement)
                        .filter(Objects::nonNull)
                        .map(Element::asType)
                        .map(TypeUtils::ofDeclaredType)
                        .filter(Objects::nonNull)
                        .collect(LinkedHashSet::new, Set::add, Set::addAll);
    }

    static Set<TypeElement> ofTypeElements(Iterable<? extends TypeMirror> types) {
        return types == null ?
                emptySet() :
                stream(types.spliterator(), false)
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

    static URL getResource(ProcessingEnvironment processingEnv, Element type) {
        return getResource(processingEnv, ofDeclaredType(type));
    }

    static URL getResource(ProcessingEnvironment processingEnv, TypeMirror type) {
        return type == null ? null : getResource(processingEnv, type.toString());
    }

    static URL getResource(ProcessingEnvironment processingEnv, CharSequence type) {
        String relativeName = getResourceName(type);
        URL resource = null;
        try {
            if (relativeName != null) {
                FileObject fileObject = processingEnv.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", relativeName);
                resource = fileObject.toUri().toURL();
                // try to open it
                resource.getContent();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return resource;
    }

    static String getResourceName(CharSequence type) {
        return type == null ? null : type.toString().replace('.', '/').concat(".class");
    }

    static String toString(TypeMirror type) {
        TypeElement element = ofTypeElement(type);
        if (element != null) {
            List<? extends TypeParameterElement> typeParameterElements = element.getTypeParameters();
            if (!typeParameterElements.isEmpty()) {
                List<TypeMirror> typeMirrors = invokeMethod(type, "getTypeArguments");
                StringBuilder typeBuilder = new StringBuilder(element.toString());
                typeBuilder.append("<");
                for (int i = 0; i < typeMirrors.size(); i++) {
                    if (i > 0) {
                        typeBuilder.append(", ");
                    }
                    typeBuilder.append(typeMirrors.get(i).toString());
                }
                typeBuilder.append(">");
                return typeBuilder.toString();
            }
        }
        return type.toString();
    }
}