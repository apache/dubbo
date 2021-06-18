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

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import static java.lang.String.valueOf;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationUtils.getAttribute;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationUtils.isAnnotationPresent;

/**
 * The utilities class for @Service annotation
 *
 * @since 2.7.6
 */
public interface ServiceAnnotationUtils {

    /**
     * The class name of @Service
     *
     * @deprecated Recommend {@link #DUBBO_SERVICE_ANNOTATION_TYPE}
     */
    @Deprecated
    String SERVICE_ANNOTATION_TYPE = "org.apache.dubbo.config.annotation.Service";

    /**
     * The class name of the legacy @Service
     *
     * @deprecated Recommend {@link #DUBBO_SERVICE_ANNOTATION_TYPE}
     */
    @Deprecated
    String LEGACY_SERVICE_ANNOTATION_TYPE = "com.alibaba.dubbo.config.annotation.Service";

    /**
     * The class name of @DubboService
     *
     * @since 2.7.9
     */
    String DUBBO_SERVICE_ANNOTATION_TYPE = "org.apache.dubbo.config.annotation.DubboService";

    /**
     * the attribute name of @Service.interfaceClass()
     */
    String INTERFACE_CLASS_ATTRIBUTE_NAME = "interfaceClass";

    /**
     * the attribute name of @Service.interfaceName()
     */
    String INTERFACE_NAME_ATTRIBUTE_NAME = "interfaceName";

    /**
     * the attribute name of @Service.group()
     */
    String GROUP_ATTRIBUTE_NAME = "group";

    /**
     * the attribute name of @Service.version()
     */
    String VERSION_ATTRIBUTE_NAME = "version";

    Set<String> SUPPORTED_ANNOTATION_TYPES = unmodifiableSet(new LinkedHashSet<>(asList(DUBBO_SERVICE_ANNOTATION_TYPE, SERVICE_ANNOTATION_TYPE, LEGACY_SERVICE_ANNOTATION_TYPE)));

    static boolean isServiceAnnotationPresent(TypeElement annotatedType) {
        return SUPPORTED_ANNOTATION_TYPES.stream()
                .filter(type -> isAnnotationPresent(annotatedType, type))
                .findFirst()
                .isPresent();
    }

    static AnnotationMirror getAnnotation(TypeElement annotatedClass) {
        return getAnnotation(annotatedClass.getAnnotationMirrors());
    }

    static AnnotationMirror getAnnotation(Iterable<? extends AnnotationMirror> annotationMirrors) {
        AnnotationMirror matchedAnnotationMirror = null;

        MAIN:
        for (String supportedAnnotationType : SUPPORTED_ANNOTATION_TYPES) { // Prioritized
            for (AnnotationMirror annotationMirror : annotationMirrors) {
                String annotationType = annotationMirror.getAnnotationType().toString();
                if (Objects.equals(supportedAnnotationType, annotationType)) {
                    matchedAnnotationMirror = annotationMirror;
                    break MAIN;
                }
            }
        }

        if (matchedAnnotationMirror == null) {
            throw new IllegalArgumentException("The annotated element must be annotated any of " + SUPPORTED_ANNOTATION_TYPES);
        }

        return matchedAnnotationMirror;
    }

    static String resolveServiceInterfaceName(TypeElement annotatedClass, AnnotationMirror serviceAnnotation) {
        Object interfaceClass = getAttribute(serviceAnnotation, INTERFACE_CLASS_ATTRIBUTE_NAME);

        if (interfaceClass == null) { // try to find the "interfaceName" attribute
            interfaceClass = getAttribute(serviceAnnotation, INTERFACE_NAME_ATTRIBUTE_NAME);
        }

        if (interfaceClass == null) {
            // last, get the interface class from first one
            interfaceClass = ((TypeElement) annotatedClass).getInterfaces().get(0);
        }

        return valueOf(interfaceClass);
    }

    static String getGroup(AnnotationMirror serviceAnnotation) {
        return getAttribute(serviceAnnotation, GROUP_ATTRIBUTE_NAME);
    }

    static String getVersion(AnnotationMirror serviceAnnotation) {
        return getAttribute(serviceAnnotation, VERSION_ATTRIBUTE_NAME);
    }
}
