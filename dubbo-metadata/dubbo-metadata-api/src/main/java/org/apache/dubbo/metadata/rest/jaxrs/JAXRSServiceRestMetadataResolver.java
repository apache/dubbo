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
package org.apache.dubbo.metadata.rest.jaxrs;

import org.apache.dubbo.metadata.rest.AbstractServiceRestMetadataResolver;
import org.apache.dubbo.metadata.rest.ServiceRestMetadataResolver;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.stream.Stream;

import static org.apache.dubbo.common.utils.AnnotationUtils.findAnnotation;
import static org.apache.dubbo.common.utils.AnnotationUtils.findMetaAnnotation;
import static org.apache.dubbo.common.utils.AnnotationUtils.getValue;
import static org.apache.dubbo.common.utils.AnnotationUtils.isAnnotationPresent;
import static org.apache.dubbo.common.utils.PathUtils.buildPath;
import static org.apache.dubbo.metadata.rest.RestMetadataConstants.JAX_RS.CONSUMES_ANNOTATION_CLASS_NAME;
import static org.apache.dubbo.metadata.rest.RestMetadataConstants.JAX_RS.HTTP_METHOD_ANNOTATION_CLASS_NAME;
import static org.apache.dubbo.metadata.rest.RestMetadataConstants.JAX_RS.PATH_ANNOTATION_CLASS_NAME;
import static org.apache.dubbo.metadata.rest.RestMetadataConstants.JAX_RS.PRODUCES_ANNOTATION_CLASS_NAME;

/**
 * JAX-RS {@link ServiceRestMetadataResolver} implementation
 *
 * @since 2.7.6
 */
public class JAXRSServiceRestMetadataResolver extends AbstractServiceRestMetadataResolver {

    @Override
    protected boolean supports0(Class<?> serviceType) {
        return isAnnotationPresent(serviceType, PATH_ANNOTATION_CLASS_NAME);
    }

    @Override
    protected boolean isRestCapableMethod(Method serviceMethod, Class<?> serviceType, Class<?> serviceInterfaceClass) {
        return isAnnotationPresent(serviceMethod, HTTP_METHOD_ANNOTATION_CLASS_NAME);
    }

    @Override
    protected String resolveRequestMethod(Method serviceMethod, Class<?> serviceType, Class<?> serviceInterfaceClass) {
        Annotation httpMethod = findMetaAnnotation(serviceMethod, HTTP_METHOD_ANNOTATION_CLASS_NAME);
        return getValue(httpMethod);
    }

    @Override
    protected String resolveRequestPath(Method serviceMethod, Class<?> serviceType, Class<?> serviceInterfaceClass) {
        String requestBasePath = resolveRequestPathFromType(serviceType, serviceInterfaceClass);
        String requestRelativePath = resolveRequestPathFromMethod(serviceMethod);
        return buildPath(requestBasePath, requestRelativePath);
    }

    private String resolveRequestPathFromType(Class<?> serviceType, Class<?> serviceInterfaceClass) {
        Annotation path = findAnnotation(serviceType, PATH_ANNOTATION_CLASS_NAME);
        if (path == null) {
            path = findAnnotation(serviceInterfaceClass, PATH_ANNOTATION_CLASS_NAME);
        }
        return getValue(path);
    }

    private String resolveRequestPathFromMethod(Method serviceMethod) {
        Annotation path = findAnnotation(serviceMethod, PATH_ANNOTATION_CLASS_NAME);
        return getValue(path);
    }

    @Override
    protected void processProduces(Method serviceMethod, Class<?> serviceType, Class<?> serviceInterfaceClass,
                                   Set<String> produces) {
        addAnnotationValues(serviceMethod, PRODUCES_ANNOTATION_CLASS_NAME, produces);
    }

    @Override
    protected void processConsumes(Method serviceMethod, Class<?> serviceType, Class<?> serviceInterfaceClass,
                                   Set<String> consumes) {
        addAnnotationValues(serviceMethod, CONSUMES_ANNOTATION_CLASS_NAME, consumes);
    }

    private void addAnnotationValues(Method serviceMethod, String annotationAttributeName, Set<String> result) {
        Annotation annotation = findAnnotation(serviceMethod, annotationAttributeName);
        String[] value = getValue(annotation);
        if (value != null) {
            Stream.of(value).forEach(result::add);
        }
    }
}
