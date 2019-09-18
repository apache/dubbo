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
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.NoType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.StreamSupport.stream;
import static javax.lang.model.util.ElementFilter.methodsIn;

/**
 * An utilities class for {@link Processor}
 *
 * @since 2.7.5
 */
public interface AnnotationProcessorUtils {

    static Object getAttribute(AnnotationMirror annotation, String attributeName) {
        return getAttribute(annotation.getElementValues(), attributeName);
    }

    static Object getAttribute(Map<? extends ExecutableElement, ? extends AnnotationValue> attributesMap,
                               String attributeName) {
        Object attributeValue = null;
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : attributesMap.entrySet()) {
            ExecutableElement executableElement = entry.getKey();
            if (attributeName.equals(executableElement.getSimpleName().toString())) {
                attributeValue = entry.getValue().getValue();
                break;
            }
        }
        return attributeValue;
    }

    static Object getValue(AnnotationMirror annotation) {
        return getAttribute(annotation, "value");
    }

    static List<? extends Element> getMembers(ProcessingEnvironment processingEnv, TypeElement type,
                                              Type... excludedTypes) {
        Elements elements = processingEnv.getElementUtils();
        List<? extends Element> members = new LinkedList<>(elements.getAllMembers(type));

        Stream.of(excludedTypes)
                .map(Type::getTypeName)        // class names to exclude
                .map(elements::getTypeElement) // class names to TypeElements
                .map(elements::getAllMembers)  // TypeElements to Elements
                .flatMap(Collection::stream)   // flat map
                .forEach(members::remove);     // remove objects' methods

        return members;
    }

    static List<? extends ExecutableElement> getMethods(ProcessingEnvironment processingEnv, Class<?> type,
                                                        Type... excludedTypes) {
        return getMethods(processingEnv, type.getTypeName(), excludedTypes);
    }

    static List<? extends ExecutableElement> getMethods(ProcessingEnvironment processingEnv, CharSequence typeName,
                                                        Type... excludedTypes) {
        Elements elements = processingEnv.getElementUtils();
        return getMethods(processingEnv, elements.getTypeElement(typeName), excludedTypes);
    }

    static List<? extends ExecutableElement> getMethods(ProcessingEnvironment processingEnv, TypeElement type,
                                                        Type... excludedTypes) {
        return methodsIn(getMembers(processingEnv, type, excludedTypes));
    }

    static ExecutableElement getOverrideMethod(ProcessingEnvironment processingEnv, TypeElement type,
                                               ExecutableElement declaringMethod) {
        return getMethods(processingEnv, type)
                .stream()
                .filter(method -> processingEnv.getElementUtils().overrides(method, declaringMethod, type))
                .findFirst()
                .orElse(null);
    }

    static Set<TypeElement> getHierarchicalTypes(ProcessingEnvironment processingEnv, TypeElement type) {

        Set<TypeElement> hierarchicalTypes = new LinkedHashSet<>();
        // add current type
        hierarchicalTypes.add(type);
        // add all hierarchical types

        TypeMirror superClass = type.getSuperclass();

        Elements elements = processingEnv.getElementUtils();

        while (!(superClass instanceof NoType)) {

            TypeElement superType = elements.getTypeElement(superClass.toString());
            hierarchicalTypes.add(superType);

            List<? extends TypeMirror> superInterfaces = superType.getInterfaces();

            superInterfaces.stream()
                    .map(TypeMirror::toString)
                    .map(elements::getTypeElement)
                    .forEach(hierarchicalTypes::add);

            superClass = superType.getSuperclass();
        }

        return hierarchicalTypes;
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
                .collect(Collectors.toList());
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
                .collect(Collectors.toList());
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
}