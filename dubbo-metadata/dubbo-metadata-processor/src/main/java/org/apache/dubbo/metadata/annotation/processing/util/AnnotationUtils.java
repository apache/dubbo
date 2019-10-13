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
import javax.lang.model.element.ExecutableElement;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
}
