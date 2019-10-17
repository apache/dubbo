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
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static org.apache.dubbo.common.function.Predicates.EMPTY_ARRAY;
import static org.apache.dubbo.common.function.Predicates.alwaysTrue;
import static org.apache.dubbo.common.function.Streams.filterAll;
import static org.apache.dubbo.common.function.Streams.filterFirst;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.getHierarchicalTypes;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.getType;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.isSameType;

/**
 * The utilities class for annotation in the package "javax.lang.model.*"
 *
 * @since 2.7.5
 */
public interface AnnotationUtils {

    static AnnotationMirror getAnnotation(AnnotatedConstruct annotatedConstruct,
                                          Class<? extends Annotation> annotationClass) {
        return getAnnotation(annotatedConstruct, annotationClass.getTypeName());
    }

    static AnnotationMirror getAnnotation(AnnotatedConstruct annotatedConstruct, CharSequence annotationClassName) {
        List<AnnotationMirror> annotations = getAnnotations(annotatedConstruct, annotationClassName);
        return annotations.isEmpty() ? null : annotations.get(0);
    }

    static List<AnnotationMirror> getAnnotations(AnnotatedConstruct annotatedConstruct, Class<? extends Annotation> annotationClass) {
        return getAnnotations(annotatedConstruct, annotationClass.getName());
    }

    static List<AnnotationMirror> getAnnotations(AnnotatedConstruct annotatedConstruct,
                                                 CharSequence annotationClassName) {
        return annotatedConstruct.getAnnotationMirrors()
                .stream()
                .filter(annotation -> isSameType(annotation.getAnnotationType(), annotationClassName))
                .collect(Collectors.toList());
    }

    static List<AnnotationMirror> getAllAnnotations(TypeMirror type) {
        return getAllAnnotations(type, alwaysTrue());
    }

    static List<AnnotationMirror> getAllAnnotations(TypeMirror type, Predicate<AnnotationMirror>... annotationFilters) {
        return filterAll(getHierarchicalTypes(type)
                .stream()
                .map(AnnotatedConstruct::getAnnotationMirrors)
                .flatMap(Collection::stream)
                .collect(Collectors.toList()), annotationFilters);
    }

    static List<AnnotationMirror> getAllAnnotations(Element element) {
        return getAllAnnotations(element, EMPTY_ARRAY);
    }

    static List<AnnotationMirror> getAllAnnotations(Element element, Predicate<AnnotationMirror>... annotationFilters) {
        return element == null ? emptyList() : getAllAnnotations(element.asType(), annotationFilters);
    }

    static List<AnnotationMirror> getAllAnnotations(ProcessingEnvironment processingEnv, Type type) {
        return getAllAnnotations(processingEnv, type, EMPTY_ARRAY);
    }

    static List<AnnotationMirror> getAllAnnotations(ProcessingEnvironment processingEnv, Type type,
                                                    Predicate<AnnotationMirror>... annotationFilters) {
        return getAllAnnotations(getType(processingEnv, type), annotationFilters);
    }

    static AnnotationMirror findAnnotation(TypeMirror type, CharSequence annotationClassName) {
        return filterFirst(getAllAnnotations(type, annotation -> isSameType(annotation.getAnnotationType(), annotationClassName)));
    }

    static AnnotationMirror findAnnotation(Element element, CharSequence annotationClassName) {
        return element == null ? null : findAnnotation(element.asType(), annotationClassName);
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

}
