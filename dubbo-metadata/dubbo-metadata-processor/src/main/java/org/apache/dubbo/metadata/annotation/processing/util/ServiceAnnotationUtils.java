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
import java.util.HashSet;
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
     */
    String SERVICE_ANNOTATION_TYPE = "org.apache.dubbo.config.annotation.Service";

    /**
     * The class name of the legacy @Service
     */
    @Deprecated
    String LEGACY_SERVICE_ANNOTATION_TYPE = "com.alibaba.dubbo.config.annotation.Service";

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

    Set<String> SUPPORTED_ANNOTATION_TYPES = unmodifiableSet(new HashSet(asList(SERVICE_ANNOTATION_TYPE, LEGACY_SERVICE_ANNOTATION_TYPE)));

    static boolean isServiceAnnotationPresent(TypeElement annotatedType) {
        return isAnnotationPresent(annotatedType, SERVICE_ANNOTATION_TYPE) ||
                isAnnotationPresent(annotatedType, LEGACY_SERVICE_ANNOTATION_TYPE);
    }

    static AnnotationMirror getAnnotation(TypeElement annotatedClass) {
        return getAnnotation(annotatedClass.getAnnotationMirrors());
    }

    static AnnotationMirror getAnnotation(Iterable<? extends AnnotationMirror> annotationMirrors) {
        AnnotationMirror matchedAnnotationMirror = null;
        for (AnnotationMirror annotationMirror : annotationMirrors) {
            String annotationType = annotationMirror.getAnnotationType().toString();
            if (SERVICE_ANNOTATION_TYPE.equals(annotationType)) {
                matchedAnnotationMirror = annotationMirror;
                break;
            } else if (LEGACY_SERVICE_ANNOTATION_TYPE.equals(annotationType)) {
                matchedAnnotationMirror = annotationMirror;
            }
        }

        if (matchedAnnotationMirror == null) {
            throw new IllegalArgumentException("The annotated element must be implemented the interface "
                    + SERVICE_ANNOTATION_TYPE + " or " + LEGACY_SERVICE_ANNOTATION_TYPE);
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
