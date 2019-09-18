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

import org.apache.dubbo.metadata.rest.MethodRestMetadata;
import org.apache.dubbo.metadata.util.HttpUtils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

import static java.lang.String.valueOf;
import static org.apache.dubbo.common.utils.StringUtils.SLASH;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationProcessorUtils.getAnnotation;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationProcessorUtils.getValue;

/**
 * {@link ServiceRestMetadataProcessor} implementation for JAX-RS 2
 *
 * @since 2.7.5
 */
public class JAXRS2ServiceRestMetadataProcessor extends AbstractServiceRestMetadataProcessor {

    /**
     * The annotation name of @Path
     */
    public static final String PATH_ANNOTATION_NAME = "javax.ws.rs.Path";

    @Override
    protected void processMethod(ProcessingEnvironment processingEnv, TypeElement serviceType, ExecutableElement method,
                                 MethodRestMetadata metadata) {

    }

    private String getPath(ProcessingEnvironment processingEnv, TypeElement serviceType, ExecutableElement method) {

        String pathFromType = getPathValue(processingEnv, serviceType);

        String pathFromMethod = getPathValue(method);

        return HttpUtils.normalizePath(pathFromType + SLASH + pathFromMethod);
    }

    private String getPathValue(ProcessingEnvironment processingEnv, TypeElement serviceType) {
        AnnotationMirror annotation = getAnnotation(processingEnv, serviceType, PATH_ANNOTATION_NAME);
        return valueOf(getValue(annotation));
    }

    private String getPathValue(AnnotatedConstruct annotatedConstruct) {
        AnnotationMirror annotation = getAnnotation(annotatedConstruct.getAnnotationMirrors(), PATH_ANNOTATION_NAME);
        return valueOf(getValue(annotation));
    }

}
