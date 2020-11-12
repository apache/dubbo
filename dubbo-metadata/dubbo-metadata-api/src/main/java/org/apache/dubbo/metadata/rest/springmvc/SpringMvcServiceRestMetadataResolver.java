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
package org.apache.dubbo.metadata.rest.springmvc;

import org.apache.dubbo.metadata.rest.AbstractServiceRestMetadataResolver;
import org.apache.dubbo.metadata.rest.ServiceRestMetadataResolver;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Set;

import static java.lang.String.valueOf;
import static java.lang.reflect.Array.getLength;
import static java.util.stream.Stream.of;
import static org.apache.dubbo.common.utils.AnnotationUtils.findAnnotation;
import static org.apache.dubbo.common.utils.AnnotationUtils.getAttribute;
import static org.apache.dubbo.common.utils.AnnotationUtils.isAnnotationPresent;
import static org.apache.dubbo.common.utils.ArrayUtils.isEmpty;
import static org.apache.dubbo.common.utils.ArrayUtils.isNotEmpty;
import static org.apache.dubbo.common.utils.MethodUtils.findMethod;
import static org.apache.dubbo.common.utils.PathUtils.buildPath;
import static org.apache.dubbo.metadata.rest.RestMetadataConstants.SPRING_MVC.ANNOTATED_ELEMENT_UTILS_CLASS;
import static org.apache.dubbo.metadata.rest.RestMetadataConstants.SPRING_MVC.CONTROLLER_ANNOTATION_CLASS;
import static org.apache.dubbo.metadata.rest.RestMetadataConstants.SPRING_MVC.REQUEST_MAPPING_ANNOTATION_CLASS;

/**
 * {@link ServiceRestMetadataResolver}
 *
 * @since 2.7.6
 */
public class SpringMvcServiceRestMetadataResolver extends AbstractServiceRestMetadataResolver {

    private static final int FIRST_ELEMENT_INDEX = 0;

    @Override
    protected boolean supports0(Class<?> serviceType) {
        return isAnnotationPresent(serviceType, CONTROLLER_ANNOTATION_CLASS);
    }

    @Override
    protected boolean isRestCapableMethod(Method serviceMethod, Class<?> serviceType, Class<?> serviceInterfaceClass) {
        return isAnnotationPresent(serviceType, REQUEST_MAPPING_ANNOTATION_CLASS) ||
                isAnnotationPresent(serviceMethod, REQUEST_MAPPING_ANNOTATION_CLASS);
    }

    @Override
    protected String resolveRequestMethod(Method serviceMethod, Class<?> serviceType, Class<?> serviceInterfaceClass) {
        Annotation requestMapping = getRequestMapping(serviceMethod);

        // httpMethod is an array of RequestMethod
        Object httpMethod = getAttribute(requestMapping, "method");

        if (httpMethod == null || getLength(httpMethod) < 1) {
            return null;
        }

        // TODO Is is required to support more request methods?
        return valueOf(Array.get(httpMethod, FIRST_ELEMENT_INDEX));
    }

    @Override
    protected String resolveRequestPath(Method serviceMethod, Class<?> serviceType, Class<?> serviceInterfaceClass) {
        String requestBasePath = resolveRequestPath(serviceType);
        String requestRelativePath = resolveRequestPath(serviceMethod);
        return buildPath(requestBasePath, requestRelativePath);
    }

    @Override
    protected void processProduces(Method serviceMethod, Class<?> serviceType, Class<?> serviceInterfaceClass, Set<String> produces) {
        addMediaTypes(serviceMethod, "produces", produces);
    }

    @Override
    protected void processConsumes(Method serviceMethod, Class<?> serviceType, Class<?> serviceInterfaceClass, Set<String> consumes) {
        addMediaTypes(serviceMethod, "consumes", consumes);
    }

    private String resolveRequestPath(AnnotatedElement annotatedElement) {
        Annotation mappingAnnotation = getRequestMapping(annotatedElement);

        // try "value" first
        String[] value = getAttribute(mappingAnnotation, "value");

        if (isEmpty(value)) { // try "path" later
            value = getAttribute(mappingAnnotation, "path");
        }

        if (isEmpty(value)) {
            return "";
        }
        // TODO Is is required to support more request paths?
        return value[FIRST_ELEMENT_INDEX];
    }

    private void addMediaTypes(Method serviceMethod, String annotationAttributeName, Set<String> mediaTypesSet) {

        Annotation mappingAnnotation = getRequestMapping(serviceMethod);

        String[] mediaTypes = getAttribute(mappingAnnotation, annotationAttributeName);

        if (isNotEmpty(mediaTypes)) {
            of(mediaTypes).forEach(mediaTypesSet::add);
        }
    }

    private Annotation getRequestMapping(AnnotatedElement annotatedElement) {
        // try "@RequestMapping" first
        Annotation requestMapping = findAnnotation(annotatedElement, REQUEST_MAPPING_ANNOTATION_CLASS);
        if (requestMapping == null) {
            // To try the meta-annotated annotation if can't be found.
            // For example, if the annotation "@GetMapping" is used in the Spring Framework is 4.2 or above,
            // because of "@GetMapping" alias for ("@AliasFor") "@RequestMapping" , both of them belongs to
            // the artifact "spring-web" which depends on "spring-core", thus Spring core's
            // AnnotatedElementUtils.findMergedAnnotation(AnnotatedElement, Class) must be involved.
            Method method = findMethod(ANNOTATED_ELEMENT_UTILS_CLASS, "findMergedAnnotation", AnnotatedElement.class, Class.class);
            if (method != null) {
                try {
                    requestMapping = (Annotation) method.invoke(null, annotatedElement, REQUEST_MAPPING_ANNOTATION_CLASS);
                } catch (Exception ignored) {
                }
            }
        }
        return requestMapping;
    }
}
