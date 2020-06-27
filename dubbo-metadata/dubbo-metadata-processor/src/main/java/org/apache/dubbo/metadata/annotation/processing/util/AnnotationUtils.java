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
import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.lang.Enum.valueOf;
import static java.util.Collections.emptyList;
import static org.apache.dubbo.common.function.Predicates.EMPTY_ARRAY;
import static org.apache.dubbo.common.function.Streams.filterAll;
import static org.apache.dubbo.common.function.Streams.filterFirst;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.getHierarchicalTypes;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.getType;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.isSameType;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.isTypeElement;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.ofTypeElement;

/**
 * The utilities class for annotation in the package "javax.lang.model.*"
 *
 * @since 2.7.6
 */
public interface AnnotationUtils {

    static AnnotationMirror getAnnotation(AnnotatedConstruct annotatedConstruct,
                                          Class<? extends Annotation> annotationClass) {
        return annotationClass == null ?
                null :
                getAnnotation(annotatedConstruct, annotationClass.getTypeName());
    }

    static AnnotationMirror getAnnotation(AnnotatedConstruct annotatedConstruct, CharSequence annotationClassName) {
        List<AnnotationMirror> annotations = getAnnotations(annotatedConstruct, annotationClassName);
        return annotations.isEmpty() ? null : annotations.get(0);
    }

    static List<AnnotationMirror> getAnnotations(AnnotatedConstruct annotatedConstruct, Class<? extends Annotation> annotationClass) {
        return annotationClass == null ?
                emptyList() :
                getAnnotations(annotatedConstruct, annotationClass.getTypeName());
    }

    static List<AnnotationMirror> getAnnotations(AnnotatedConstruct annotatedConstruct,
                                                 CharSequence annotationClassName) {
        return getAnnotations(annotatedConstruct,
                annotation -> isSameType(annotation.getAnnotationType(), annotationClassName));
    }

    static List<AnnotationMirror> getAnnotations(AnnotatedConstruct annotatedConstruct) {
        return getAnnotations(annotatedConstruct, EMPTY_ARRAY);
    }

    static List<AnnotationMirror> getAnnotations(AnnotatedConstruct annotatedConstruct,
                                                 Predicate<AnnotationMirror>... annotationFilters) {

        AnnotatedConstruct actualAnnotatedConstruct = annotatedConstruct;

        if (annotatedConstruct instanceof TypeMirror) {
            actualAnnotatedConstruct = ofTypeElement((TypeMirror) actualAnnotatedConstruct);
        }

        return actualAnnotatedConstruct == null ?
                emptyList() :
                filterAll((List<AnnotationMirror>) actualAnnotatedConstruct.getAnnotationMirrors(), annotationFilters);
    }

    static List<AnnotationMirror> getAllAnnotations(TypeMirror type) {
        return getAllAnnotations(ofTypeElement(type));
    }

    static List<AnnotationMirror> getAllAnnotations(Element element) {
        return getAllAnnotations(element, EMPTY_ARRAY);
    }

    static List<AnnotationMirror> getAllAnnotations(TypeMirror type, Class<? extends Annotation> annotationClass) {
        return getAllAnnotations(ofTypeElement(type), annotationClass);
    }

    static List<AnnotationMirror> getAllAnnotations(Element element, Class<? extends Annotation> annotationClass) {
        return element == null || annotationClass == null ?
                emptyList() :
                getAllAnnotations(element, annotationClass.getTypeName());
    }

    static List<AnnotationMirror> getAllAnnotations(TypeMirror type, CharSequence annotationClassName) {
        return getAllAnnotations(ofTypeElement(type), annotationClassName);
    }

    static List<AnnotationMirror> getAllAnnotations(Element element, CharSequence annotationClassName) {
        return getAllAnnotations(element, annotation -> isSameType(annotation.getAnnotationType(), annotationClassName));
    }

    static List<AnnotationMirror> getAllAnnotations(TypeMirror type, Predicate<AnnotationMirror>... annotationFilters) {
        return getAllAnnotations(ofTypeElement(type), annotationFilters);
    }

    static List<AnnotationMirror> getAllAnnotations(Element element, Predicate<AnnotationMirror>... annotationFilters) {

        List<AnnotationMirror> allAnnotations = isTypeElement(element) ?
                getHierarchicalTypes(ofTypeElement(element))
                        .stream()
                        .map(AnnotationUtils::getAnnotations)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList()) :
                element == null ? emptyList() : (List<AnnotationMirror>) element.getAnnotationMirrors();

        return filterAll(allAnnotations, annotationFilters);
    }

    static List<AnnotationMirror> getAllAnnotations(ProcessingEnvironment processingEnv, Type annotatedType) {
        return getAllAnnotations(processingEnv, annotatedType, EMPTY_ARRAY);
    }

    static List<AnnotationMirror> getAllAnnotations(ProcessingEnvironment processingEnv, Type annotatedType,
                                                    Predicate<AnnotationMirror>... annotationFilters) {
        return annotatedType == null ?
                emptyList() :
                getAllAnnotations(processingEnv, annotatedType.getTypeName(), annotationFilters);
    }

    static List<AnnotationMirror> getAllAnnotations(ProcessingEnvironment processingEnv, CharSequence annotatedTypeName,
                                                    Predicate<AnnotationMirror>... annotationFilters) {
        return getAllAnnotations(getType(processingEnv, annotatedTypeName), annotationFilters);
    }

    static AnnotationMirror findAnnotation(TypeMirror type, Class<? extends Annotation> annotationClass) {
        return annotationClass == null ? null : findAnnotation(type, annotationClass.getTypeName());
    }

    static AnnotationMirror findAnnotation(TypeMirror type, CharSequence annotationClassName) {
        return findAnnotation(ofTypeElement(type), annotationClassName);
    }

    static AnnotationMirror findAnnotation(Element element, Class<? extends Annotation> annotationClass) {
        return annotationClass == null ? null : findAnnotation(element, annotationClass.getTypeName());
    }

    static AnnotationMirror findAnnotation(Element element, CharSequence annotationClassName) {
        return filterFirst(getAllAnnotations(element, annotation -> isSameType(annotation.getAnnotationType(), annotationClassName)));
    }

    static AnnotationMirror findMetaAnnotation(Element annotatedConstruct, CharSequence metaAnnotationClassName) {
        return annotatedConstruct == null ?
                null :
                getAnnotations(annotatedConstruct)
                        .stream()
                        .map(annotation -> findAnnotation(annotation.getAnnotationType(), metaAnnotationClassName))
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null);
    }

    static boolean isAnnotationPresent(Element element, CharSequence annotationClassName) {
        return findAnnotation(element, annotationClassName) != null ||
                findMetaAnnotation(element, annotationClassName) != null;
    }

    static <T> T getAttribute(AnnotationMirror annotation, String attributeName) {
        return annotation == null ? null : getAttribute(annotation.getElementValues(), attributeName);
    }

    static <T> T getAttribute(Map<? extends ExecutableElement, ? extends AnnotationValue> attributesMap,
                              String attributeName) {
        T annotationValue = null;
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : attributesMap.entrySet()) {
            ExecutableElement attributeMethod = entry.getKey();
            if (Objects.equals(attributeName, attributeMethod.getSimpleName().toString())) {
                TypeMirror attributeType = attributeMethod.getReturnType();
                AnnotationValue value = entry.getValue();
                if (attributeType instanceof ArrayType) { // array-typed attribute values
                    ArrayType arrayType = (ArrayType) attributeType;
                    String componentType = arrayType.getComponentType().toString();
                    ClassLoader classLoader = AnnotationUtils.class.getClassLoader();
                    List<AnnotationValue> values = (List<AnnotationValue>) value.getValue();
                    int size = values.size();
                    try {
                        Class componentClass = classLoader.loadClass(componentType);
                        boolean isEnum = componentClass.isEnum();
                        Object array = Array.newInstance(componentClass, values.size());
                        for (int i = 0; i < size; i++) {
                            Object element = values.get(i).getValue();
                            if (isEnum) {
                                element = valueOf(componentClass, element.toString());
                            }
                            Array.set(array, i, element);
                        }
                        annotationValue = (T) array;
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    annotationValue = (T) value.getValue();
                }
                break;
            }
        }
        return annotationValue;
    }

    static <T> T getValue(AnnotationMirror annotation) {
        return (T) getAttribute(annotation, "value");
    }
}
