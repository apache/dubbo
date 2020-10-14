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
package org.apache.dubbo.metadata.annotation.processing.rest.springmvc;

import org.apache.dubbo.metadata.annotation.processing.rest.AbstractServiceRestMetadataResolver;
import org.apache.dubbo.metadata.annotation.processing.rest.ServiceRestMetadataResolver;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import java.lang.reflect.Array;
import java.util.Set;

import static java.lang.String.valueOf;
import static java.lang.reflect.Array.getLength;
import static java.util.stream.Stream.of;
import static org.apache.dubbo.common.function.Streams.filterFirst;
import static org.apache.dubbo.common.utils.ArrayUtils.isEmpty;
import static org.apache.dubbo.common.utils.ArrayUtils.isNotEmpty;
import static org.apache.dubbo.common.utils.PathUtils.buildPath;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationUtils.findAnnotation;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationUtils.findMetaAnnotation;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationUtils.getAllAnnotations;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationUtils.getAttribute;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationUtils.isAnnotationPresent;
import static org.apache.dubbo.metadata.rest.RestMetadataConstants.SPRING_MVC.CONTROLLER_ANNOTATION_CLASS_NAME;
import static org.apache.dubbo.metadata.rest.RestMetadataConstants.SPRING_MVC.REQUEST_MAPPING_ANNOTATION_CLASS_NAME;

/**
 * {@link ServiceRestMetadataResolver}
 *
 * @since 2.7.6
 */
public class SpringMvcServiceRestMetadataResolver extends AbstractServiceRestMetadataResolver {

    private static final int FIRST_ELEMENT_INDEX = 0;

    @Override
    public boolean supports(ProcessingEnvironment processingEnvironment, TypeElement serviceType) {
        return supports(serviceType);
    }

    @Override
    protected boolean supports(ProcessingEnvironment processingEnv, TypeElement serviceType,
                               TypeElement serviceInterfaceType, ExecutableElement method) {
        return isAnnotationPresent(method, REQUEST_MAPPING_ANNOTATION_CLASS_NAME);
    }

    public static boolean supports(TypeElement serviceType) {
        return isAnnotationPresent(serviceType, CONTROLLER_ANNOTATION_CLASS_NAME);
    }

    @Override
    protected String resolveRequestPath(ProcessingEnvironment processingEnv, TypeElement serviceType,
                                        ExecutableElement method) {

        String requestPathFromType = getRequestPath(serviceType);

        String requestPathFromMethod = getRequestPath(method);

        return buildPath(requestPathFromType, requestPathFromMethod);
    }


    @Override
    protected String resolveRequestMethod(ProcessingEnvironment processingEnv, TypeElement serviceType,
                                          ExecutableElement method) {

        AnnotationMirror requestMapping = getRequestMapping(method);

        // httpMethod is an array of RequestMethod
        Object httpMethod = getAttribute(requestMapping, "method");

        if (httpMethod == null || getLength(httpMethod) < 1) {
            return null;
        }

        // TODO Is is required to support more request methods?
        return valueOf(Array.get(httpMethod, FIRST_ELEMENT_INDEX));
    }

    private AnnotationMirror getRequestMapping(Element element) {
        // try "@RequestMapping" first
        AnnotationMirror requestMapping = findAnnotation(element, REQUEST_MAPPING_ANNOTATION_CLASS_NAME);
        // try the annotation meta-annotated later
        if (requestMapping == null) {
            requestMapping = findMetaAnnotation(element, REQUEST_MAPPING_ANNOTATION_CLASS_NAME);
        }
        return requestMapping;
    }

    @Override
    protected void processProduces(ProcessingEnvironment processingEnv, TypeElement serviceType,
                                   ExecutableElement method, Set<String> produces) {
        addMediaTypes(method, "produces", produces);
    }

    @Override
    protected void processConsumes(ProcessingEnvironment processingEnv, TypeElement serviceType,
                                   ExecutableElement method, Set<String> consumes) {
        addMediaTypes(method, "consumes", consumes);
    }

    private void addMediaTypes(ExecutableElement method, String annotationAttributeName, Set<String> mediaTypesSet) {

        AnnotationMirror mappingAnnotation = getMappingAnnotation(method);

        String[] mediaTypes = getAttribute(mappingAnnotation, annotationAttributeName);

        if (isNotEmpty(mediaTypes)) {
            of(mediaTypes).forEach(mediaTypesSet::add);
        }
    }

    private AnnotationMirror getMappingAnnotation(Element element) {
        return computeIfAbsent(valueOf(element), key ->
                filterFirst(getAllAnnotations(element), annotation -> {
                    DeclaredType annotationType = annotation.getAnnotationType();
                    // try "@RequestMapping" first
                    if (REQUEST_MAPPING_ANNOTATION_CLASS_NAME.equals(annotationType.toString())) {
                        return true;
                    }
                    // try meta annotation
                    return isAnnotationPresent(annotationType.asElement(), REQUEST_MAPPING_ANNOTATION_CLASS_NAME);
                })
        );
    }

    private String getRequestPath(Element element) {
        AnnotationMirror mappingAnnotation = getMappingAnnotation(element);
        return getRequestPath(mappingAnnotation);
    }

    private String getRequestPath(AnnotationMirror mappingAnnotation) {
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
}
