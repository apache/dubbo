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
package org.apache.dubbo.metadata.annotation.processing.rest;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationProcessorUtils.findMetaAnnotation;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationProcessorUtils.getAnnotation;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationProcessorUtils.getValue;
import static org.apache.dubbo.metadata.util.HttpUtils.HTTP_METHODS;
import static org.apache.dubbo.metadata.util.HttpUtils.buildPath;

/**
 * {@link ServiceRestMetadataProcessor} implementation for JAX-RS 2 and 1
 *
 * @since 2.7.5
 */
public class JAXRSServiceRestMetadataProcessor extends AbstractServiceRestMetadataProcessor {

    /**
     * The package name of JAX-RS
     */
    public static final String JAX_RS_PACKAGE_NAME = "javax.ws.rs";

    /**
     * The annotation name of @Path
     */
    public static final String PATH_ANNOTATION_NAME = "javax.ws.rs.Path";

    /**
     * The annotation name of @HttpMethod
     */
    public static final String HTTP_METHOD_ANNOTATION_NAME = "javax.ws.rs.HttpMethod";

    /**
     * The mapping to map the annotation class names and HTTP methods
     */
    public static final Map<String, String> HTTP_METHOD_ANNOTATIONS_MAPPING = initHttpMethodAnnotationsMapping();

    private static Map<String, String> initHttpMethodAnnotationsMapping() {
        Map<String, String> mapping = new LinkedHashMap<>();
        HTTP_METHODS.forEach(method -> mapping.computeIfAbsent(toHttpMethodAnnotationClassName(method),
                key -> method));
        return Collections.unmodifiableMap(mapping);
    }

    private static String toHttpMethodAnnotationClassName(String method) {
        return JAX_RS_PACKAGE_NAME + "." + method;
    }

    @Override
    protected String resolveRequestPath(ProcessingEnvironment processingEnv, TypeElement serviceType, ExecutableElement method) {

        String pathFromType = getPathValue(processingEnv, serviceType);

        String pathFromMethod = getPathValue(method);

        return buildPath(pathFromType, pathFromMethod);
    }

    private String getPathValue(ProcessingEnvironment processingEnv, TypeElement serviceType) {
        AnnotationMirror annotation = getAnnotation(processingEnv, serviceType, PATH_ANNOTATION_NAME);
        return getValue(annotation);
    }

    private String getPathValue(AnnotatedConstruct annotatedConstruct) {
        AnnotationMirror annotation = getAnnotation(annotatedConstruct.getAnnotationMirrors(), PATH_ANNOTATION_NAME);
        return getValue(annotation);
    }

    @Override
    protected String resolveRequestMethod(ProcessingEnvironment processingEnv, TypeElement serviceType, ExecutableElement method) {
        AnnotationMirror annotation = findMetaAnnotation(processingEnv, method, HTTP_METHOD_ANNOTATION_NAME);
        return getValue(annotation);
    }

    @Override
    protected void processRequestParameters(ProcessingEnvironment processingEnv, TypeElement serviceType,
                                            ExecutableElement method, Map<String, List<String>> parameters) {


    }

    @Override
    protected void processRequestHeaders(ProcessingEnvironment processingEnv, TypeElement serviceType,
                                         ExecutableElement method, Map<String, List<String>> headers) {

    }

    @Override
    protected void processProduces(ProcessingEnvironment processingEnv, TypeElement serviceType,
                                   ExecutableElement method, Set<String> produces) {

    }

    @Override
    protected void processConsumes(ProcessingEnvironment processingEnv, TypeElement serviceType,
                                   ExecutableElement method, Set<String> consumes) {

    }
}
