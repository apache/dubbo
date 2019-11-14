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

import org.apache.dubbo.metadata.annotation.processing.rest.AbstractServiceRestMetadataProcessor;
import org.apache.dubbo.metadata.annotation.processing.rest.ServiceRestMetadataProcessor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.Set;
import java.util.stream.Stream;

import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationUtils.findAnnotation;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationUtils.findMetaAnnotation;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationUtils.getAnnotation;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationUtils.getValue;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationUtils.isAnnotationPresent;
import static org.apache.dubbo.metadata.util.HttpUtils.buildPath;

/**
 * {@link ServiceRestMetadataProcessor} implementation for JAX-RS 2 and 1
 *
 * @since 2.7.5
 */
public class JAXRSServiceRestMetadataProcessor extends AbstractServiceRestMetadataProcessor {

    /**
     * The annotation name of @Path
     */
    public static final String PATH_ANNOTATION_CLASS_NAME = "javax.ws.rs.Path";

    /**
     * The annotation name of @HttpMethod
     */
    public static final String HTTP_METHOD_ANNOTATION_CLASS_NAME = "javax.ws.rs.HttpMethod";

    /**
     * The annotation class name of @Produces
     */
    public static final String PRODUCES_ANNOTATION_CLASS_NAME = "javax.ws.rs.Produces";

    /**
     * The annotation class name of @Consumes
     */
    public static final String CONSUMES_ANNOTATION_CLASS_NAME = "javax.ws.rs.Consumes";

    @Override
    public boolean supports(ProcessingEnvironment processingEnvironment, TypeElement serviceType) {
        return isAnnotationPresent(serviceType, PATH_ANNOTATION_CLASS_NAME);
    }

    @Override
    protected String getRequestPath(ProcessingEnvironment processingEnv, TypeElement serviceType, ExecutableElement method) {
        String pathFromType = getPathValue(processingEnv, serviceType);
        String pathFromMethod = getPathValue(method);
        return buildPath(pathFromType, pathFromMethod);
    }

    @Override
    protected String getRequestMethod(ProcessingEnvironment processingEnv, TypeElement serviceType, ExecutableElement method) {
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
