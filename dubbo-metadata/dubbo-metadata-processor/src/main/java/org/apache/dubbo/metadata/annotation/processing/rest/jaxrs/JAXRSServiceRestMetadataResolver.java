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
package org.apache.dubbo.metadata.annotation.processing.rest.jaxrs;

import org.apache.dubbo.metadata.annotation.processing.rest.AbstractServiceRestMetadataResolver;
import org.apache.dubbo.metadata.annotation.processing.rest.ServiceRestMetadataResolver;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.Set;
import java.util.stream.Stream;

import static org.apache.dubbo.common.utils.PathUtils.buildPath;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationUtils.findAnnotation;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationUtils.findMetaAnnotation;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationUtils.getAnnotation;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationUtils.getValue;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationUtils.isAnnotationPresent;
import static org.apache.dubbo.metadata.rest.RestMetadataConstants.JAX_RS.CONSUMES_ANNOTATION_CLASS_NAME;
import static org.apache.dubbo.metadata.rest.RestMetadataConstants.JAX_RS.HTTP_METHOD_ANNOTATION_CLASS_NAME;
import static org.apache.dubbo.metadata.rest.RestMetadataConstants.JAX_RS.PATH_ANNOTATION_CLASS_NAME;
import static org.apache.dubbo.metadata.rest.RestMetadataConstants.JAX_RS.PRODUCES_ANNOTATION_CLASS_NAME;

/**
 * {@link ServiceRestMetadataResolver} implementation for JAX-RS 2 and 1
 *
 * @since 2.7.6
 */
public class JAXRSServiceRestMetadataResolver extends AbstractServiceRestMetadataResolver {

    @Override
    public boolean supports(ProcessingEnvironment processingEnvironment, TypeElement serviceType) {
        return supports(serviceType);
    }

    public static boolean supports(TypeElement serviceType) {
        return isAnnotationPresent(serviceType, PATH_ANNOTATION_CLASS_NAME);
    }

    @Override
    protected boolean supports(ProcessingEnvironment processingEnv, TypeElement serviceType,
                               TypeElement serviceInterfaceType, ExecutableElement method) {
        return isAnnotationPresent(method, PATH_ANNOTATION_CLASS_NAME) ||
                isAnnotationPresent(method, HTTP_METHOD_ANNOTATION_CLASS_NAME);
    }

    @Override
    protected String resolveRequestPath(ProcessingEnvironment processingEnv, TypeElement serviceType, ExecutableElement method) {
        String pathFromType = getPathValue(processingEnv, serviceType);
        String pathFromMethod = getPathValue(method);
        return buildPath(pathFromType, pathFromMethod);
    }

    @Override
    protected String resolveRequestMethod(ProcessingEnvironment processingEnv, TypeElement serviceType, ExecutableElement method) {
        AnnotationMirror annotation = findMetaAnnotation(method, HTTP_METHOD_ANNOTATION_CLASS_NAME);
        return getValue(annotation);
    }

    @Override
    protected void processProduces(ProcessingEnvironment processingEnv, TypeElement serviceType,
                                   ExecutableElement method, Set<String> produces) {
        addAnnotationValues(method, PRODUCES_ANNOTATION_CLASS_NAME, produces);
    }

    @Override
    protected void processConsumes(ProcessingEnvironment processingEnv, TypeElement serviceType,
                                   ExecutableElement method, Set<String> consumes) {
        addAnnotationValues(method, CONSUMES_ANNOTATION_CLASS_NAME, consumes);
    }


    private void addAnnotationValues(Element element, String annotationAttributeName, Set<String> result) {
        AnnotationMirror annotation = findAnnotation(element, annotationAttributeName);
        String[] value = getValue(annotation);
        if (value != null) {
            Stream.of(value).forEach(result::add);
        }
    }

    private String getPathValue(ProcessingEnvironment processingEnv, TypeElement serviceType) {
        AnnotationMirror annotation = findAnnotation(serviceType, PATH_ANNOTATION_CLASS_NAME);
        return getValue(annotation);
    }

    private String getPathValue(AnnotatedConstruct annotatedConstruct) {
        AnnotationMirror annotation = getAnnotation(annotatedConstruct, PATH_ANNOTATION_CLASS_NAME);
        return getValue(annotation);
    }
}
