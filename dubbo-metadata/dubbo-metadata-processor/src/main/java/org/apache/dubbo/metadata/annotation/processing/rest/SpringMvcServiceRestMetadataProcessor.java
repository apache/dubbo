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
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.Set;

import static org.apache.dubbo.common.utils.ArrayUtils.isEmpty;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationUtils.findMetaAnnotation;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationUtils.getAttribute;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationUtils.isAnnotationPresent;
import static org.apache.dubbo.metadata.util.HttpUtils.buildPath;

/**
 * {@link ServiceRestMetadataProcessor}
 *
 * @since 2.7.5
 */
public class SpringMvcServiceRestMetadataProcessor extends AbstractServiceRestMetadataProcessor {

    public static final String CONTROLLER_ANNOTATION_CLASS_NAME = "org.springframework.stereotype.Controller";

    public static final String REQUEST_MAPPING_ANNOTATION_CLASS_NAME = "org.springframework.web.bind.annotation.RequestMapping";

    @Override
    public boolean supports(ProcessingEnvironment processingEnvironment, TypeElement serviceType) {
        return super.supports(processingEnvironment, serviceType) &&
                isAnnotationPresent(serviceType, CONTROLLER_ANNOTATION_CLASS_NAME);
    }

    @Override
    protected String getRequestPath(ProcessingEnvironment processingEnv, TypeElement serviceType,
                                    ExecutableElement method) {

        String requestPathFromType = getRequestPath(serviceType);

        String requestPathFromMethod = getRequestPath(method);

        return buildPath(requestPathFromType, requestPathFromMethod);
    }

    private String getRequestPath(Element element) {
        AnnotationMirror requestMapping = findMetaAnnotation(element, REQUEST_MAPPING_ANNOTATION_CLASS_NAME);
        return getRequestPath(requestMapping);
    }


    private String getRequestPath(AnnotationMirror requestMapping) {

        // try "value" first
        String[] value = getAttribute(requestMapping, "value");

        if (isEmpty(value)) { // try "path" later
            value = getAttribute(requestMapping, "path");
        }

        if (isEmpty(value)) {
            return "";
        }
        // TODO Is is required to support more request paths?
        return value[0];
    }


    @Override
    protected String getRequestMethod(ProcessingEnvironment processingEnv, TypeElement serviceType,
                                      ExecutableElement method) {
        return null;
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
