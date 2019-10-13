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

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;
import static java.util.stream.StreamSupport.stream;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.lang.model.util.ElementFilter.fieldsIn;
import static javax.lang.model.util.ElementFilter.methodsIn;
import static org.apache.dubbo.common.function.Predicates.and;
import static org.apache.dubbo.common.function.Predicates.filterAll;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.ofTypeElement;

/**
 * An utilities class for {@link Processor}
 *
 * @since 2.7.5
 */
public interface AnnotationProcessorUtils {

    static List<VariableElement> getFields(ProcessingEnvironment processingEnv, TypeElement type,
                                           Predicate<VariableElement>... elementToFilters) {
        return filterAll(fieldsIn(getMembers(processingEnv, type)), elementToFilters);
    }

    static List<VariableElement> getAlDeclaredFields(ProcessingEnvironment processingEnv, TypeElement type) {
        return getHierarchicalTypes(processingEnv, type)
                .stream()
                .filter(Objects::nonNull)
                .map(t -> getFields(processingEnv, t))
                .flatMap(Collection::stream)
                .collect(toList());
    }

    static List<VariableElement> getNonStaticFields(ProcessingEnvironment processingEnv, TypeElement type) {
        return filterAll(getAlDeclaredFields(processingEnv, type), element -> !element.getModifiers().contains(STATIC));
    }

    static VariableElement getField(ProcessingEnvironment processingEnv, TypeElement type, CharSequence fieldName) {
        return getAlDeclaredFields(processingEnv, type)
                .stream()
                .filter(field -> Objects.equals(fieldName, field.getSimpleName().toString()))
                .findFirst()
                .orElse(null);
    }

    static List<ExecutableElement> getMethods(ProcessingEnvironment processingEnv, Class<?> type,
                                              Type... excludedTypes) {
        return getMethods(processingEnv, type.getTypeName(), excludedTypes);
    }

    static List<ExecutableElement> getMethods(ProcessingEnvironment processingEnv, CharSequence typeName,
                                              Type... excludedTypes) {
        Elements elements = processingEnv.getElementUtils();
        return getMethods(processingEnv, elements.getTypeElement(typeName), excludedTypes);
    }

    static List<ExecutableElement> getMethods(ProcessingEnvironment processingEnv, TypeElement type,
                                              Type... excludedTypes) {
        return methodsIn(getMembers(processingEnv, type, excludedTypes));
    }

    static Set<ExecutableElement> getAllDeclaredMethods(ProcessingEnvironment processingEnv, TypeElement type,
                                                        Type... excludedTypes) {
        return getHierarchicalTypes(processingEnv, type)
                .stream()
                .map(t -> getMethods(processingEnv, t, excludedTypes))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    static ExecutableElement findMethod(ProcessingEnvironment processingEnv, TypeElement type,
                                        Predicate<ExecutableElement>... elementToFilters) {
        return getMethods(processingEnv, type)
                .stream()
                .filter(and(elementToFilters))
                .findFirst()
                .orElse(null);
    }

    static ExecutableElement findMethod(ProcessingEnvironment processingEnv, TypeElement type, String methodName,
                                        Class<?>... parameterTypes) {
        return findMethod(processingEnv, type,
                method -> Objects.equals(methodName, method.getSimpleName().toString()),
                method -> {
                    List<String> expectedParameterTypes = getMethodParameterTypes(method);
                    List<String> actualParameterTypes = of(parameterTypes).map(Class::getTypeName).collect(toList());
                    return Objects.equals(expectedParameterTypes, actualParameterTypes);
                }
        );
    }

    static ExecutableElement getOverrideMethod(ProcessingEnvironment processingEnv, TypeElement type,
                                               ExecutableElement declaringMethod) {
        return findMethod(processingEnv, type,
                method -> processingEnv.getElementUtils().overrides(method, declaringMethod, type));
    }

    static Set<ExecutableElement> getHierarchicalMethods(ProcessingEnvironment processingEnv, ExecutableElement method) {
        if (method == null) {
            return Collections.emptySet();
        }

        Set<ExecutableElement> hierarchicalMethods = new LinkedHashSet<>();

        Elements elements = processingEnv.getElementUtils();
        // add current method
        hierarchicalMethods.add(method);

        TypeElement currentType = ofTypeElement(method.getEnclosingElement());

        getHierarchicalTypes(processingEnv, currentType)
                .stream()
                .map(superType -> findMethod(processingEnv, superType,
                        overridden -> elements.overrides(method, overridden, currentType)
                ))
                .filter(Objects::nonNull)
                .forEach(hierarchicalMethods::add);

        return hierarchicalMethods;
    }

    static List<? extends Element> getMembers(ProcessingEnvironment processingEnv, TypeElement type,
                                              Type... excludedTypes) {
        return getMembers(processingEnv, type, of(excludedTypes).map(Type::getTypeName).toArray(String[]::new));
    }

    static List<? extends Element> getMembers(ProcessingEnvironment processingEnv, TypeElement type,
                                              String... excludedTypeNames) {
        Elements elements = processingEnv.getElementUtils();

        List<Element> excludedElements = of(excludedTypeNames)
                .map(elements::getTypeElement)
                .map(elements::getAllMembers)
                .flatMap(Collection::stream)
                .collect(toList());

        return getMembers(processingEnv, type, element -> !excludedElements.contains(element));
    }

    static List<? extends Element> getMembers(ProcessingEnvironment processingEnv, TypeElement type,
                                              Predicate<Element>... elementToFilters) {
        return filterAll((List<Element>) getMembers(processingEnv, type), elementToFilters);
    }

    static List<? extends Element> getMembers(ProcessingEnvironment processingEnv, TypeElement type) {
        Elements elements = processingEnv.getElementUtils();
        return elements.getAllMembers(type);
    }

    static List<String> getMethodParameterTypes(ExecutableElement method) {
        return method.getParameters()
                .stream()
                .map(Element::asType)
                .map(TypeMirror::toString)
                .collect(toList());
    }

    static Set<TypeElement> getHierarchicalTypes(ProcessingEnvironment processingEnv, TypeElement type) {
        return getHierarchicalTypes(processingEnv, type, true, true, true);
    }

    static Set<TypeElement> getHierarchicalTypes(ProcessingEnvironment processingEnv, TypeElement type,
                                                 boolean includeSelf, boolean includeSuperType,
                                                 boolean includeSuperInterfaces) {

        Set<TypeElement> hierarchicalTypes = new LinkedHashSet<>();

        if (includeSelf) {
            // add current type if included
            hierarchicalTypes.add(type);
        }

        TypeElement superType = getSuperType(processingEnv, type);

        if (includeSuperInterfaces) {
            // Add super interfaces if present
            hierarchicalTypes.addAll(getInterfaces(processingEnv, type));
        }

        if (superType == null) {
            return hierarchicalTypes;
        }

        if (includeSuperType) {
            // Add super type if present
            hierarchicalTypes.add(superType);
        }

        // add all hierarchical types
        hierarchicalTypes.addAll(getHierarchicalTypes(processingEnv, superType, includeSelf, includeSuperType,
                includeSuperInterfaces));

        return hierarchicalTypes;
    }

    static Set<TypeElement> getInterfaces(ProcessingEnvironment processingEnv, TypeElement type) {
        return type.getInterfaces()
                .stream()
                .map(interfaceType -> getType(processingEnv, interfaceType))
                .collect(Collectors.toSet());
    }

    static Set<TypeElement> getAllInterfaces(ProcessingEnvironment processingEnv, TypeElement type) {
        return getHierarchicalTypes(processingEnv, type, false, false, true);
    }

    static TypeElement getSuperType(ProcessingEnvironment processingEnv, Element element) {
        TypeElement currentType = ofTypeElement(element);
        return currentType == null ? null : getSuperType(processingEnv, currentType);
    }

    static TypeElement getSuperType(ProcessingEnvironment processingEnv, TypeElement currentType) {
        TypeMirror superClass = currentType.getSuperclass();
        if (superClass instanceof NoType) {
            return null;
        }
        return getType(processingEnv, superClass);
    }

    static TypeElement getType(ProcessingEnvironment processingEnv, Class<?> type) {
        return getType(processingEnv, type.getTypeName());
    }

    static TypeElement getType(ProcessingEnvironment processingEnv, TypeMirror type) {
        return getType(processingEnv, type.toString());
    }

    static TypeElement getType(ProcessingEnvironment processingEnv, CharSequence typeName) {
        Elements elements = processingEnv.getElementUtils();
        return elements.getTypeElement(typeName);
    }


    static List<AnnotationMirror> getAllAnnotations(ProcessingEnvironment processingEnv, Class<? extends Annotation> annotationClass) {
        return getAllAnnotations(processingEnv, annotationClass.getTypeName());
    }

    static List<AnnotationMirror> getAllAnnotations(ProcessingEnvironment processingEnv, CharSequence annotationClassName) {
        Elements elements = processingEnv.getElementUtils();
        return getAllAnnotations(processingEnv, elements.getTypeElement(annotationClassName));
    }

    static List<AnnotationMirror> getAllAnnotations(ProcessingEnvironment processingEnv, TypeElement type) {
        return getAllAnnotations(getHierarchicalTypes(processingEnv, type));
    }

    static List<AnnotationMirror> getAllAnnotations(Iterable<? extends AnnotatedConstruct> annotatedConstructs) {
        return stream(annotatedConstructs.spliterator(), false)
                .map(AnnotatedConstruct::getAnnotationMirrors)
                .flatMap(Collection::stream)
                .collect(toList());
    }

    static List<AnnotationMirror> getAnnotations(ProcessingEnvironment processingEnv, TypeElement type,
                                                 Class<? extends Annotation> annotationClass) {
        return getAnnotations(processingEnv, type, annotationClass.getTypeName());
    }

    static List<AnnotationMirror> getAnnotations(ProcessingEnvironment processingEnv, TypeElement type,
                                                 CharSequence annotationClassName) {
        return getAnnotations(getAllAnnotations(processingEnv, type), annotationClassName);
    }

    static List<AnnotationMirror> getAnnotations(Iterable<? extends AnnotationMirror> annotationMirrors,
                                                 CharSequence annotationClassName) {
        return stream(annotationMirrors.spliterator(), false)
                .filter(annotation -> Objects.equals(annotationClassName, annotation.getAnnotationType().toString()))
                .collect(toList());
    }


    static AnnotationMirror getAnnotation(ProcessingEnvironment processingEnv, TypeElement type,
                                          Class<? extends Annotation> annotationClass) {
        return getAnnotation(processingEnv, type, annotationClass.getTypeName());
    }

    static AnnotationMirror getAnnotation(ProcessingEnvironment processingEnv, TypeElement type,
                                          CharSequence annotationClassName) {
        List<AnnotationMirror> annotations = getAnnotations(processingEnv, type, annotationClassName);
        return annotations.isEmpty() ? null : annotations.get(0);
    }

    static AnnotationMirror getAnnotation(Iterable<? extends AnnotationMirror> annotationMirrors,
                                          CharSequence annotationClassName) {
        List<AnnotationMirror> annotations = getAnnotations(annotationMirrors, annotationClassName);
        return annotations.isEmpty() ? null : annotations.get(0);
    }

    static AnnotationMirror findAnnotation(ProcessingEnvironment processingEnv, ExecutableElement method,
                                           Class<? extends Annotation> annotationClass) {
        return findAnnotation(processingEnv, method, annotationClass.getTypeName());
    }

    static AnnotationMirror findAnnotation(ProcessingEnvironment processingEnv, ExecutableElement method,
                                           CharSequence annotationClassName) {
        return getAllAnnotations(getHierarchicalMethods(processingEnv, method))
                .stream()
                .filter(annotation -> Objects.equals(annotation.getAnnotationType().toString(), annotationClassName))
                .findFirst()
                .orElse(null);
    }

    static AnnotationMirror findMetaAnnotation(ProcessingEnvironment processingEnv, ExecutableElement method,
                                               Class<? extends Annotation> metaAnnotationClass) {
        return findMetaAnnotation(processingEnv, method, metaAnnotationClass.getTypeName());
    }

    static AnnotationMirror findMetaAnnotation(ProcessingEnvironment processingEnv, ExecutableElement method,
                                               CharSequence metaAnnotationClassName) {
        return getAllAnnotations(getHierarchicalMethods(processingEnv, method))
                .stream()
                .map(AnnotationMirror::getAnnotationType)
                .map(DeclaredType::asElement)
                .map(Element::getAnnotationMirrors)
                .flatMap(Collection::stream)
                .filter(metaAnnotation -> Objects.equals(metaAnnotationClassName, metaAnnotation.getAnnotationType().toString()))
                .findFirst()
                .orElse(null);
    }
}