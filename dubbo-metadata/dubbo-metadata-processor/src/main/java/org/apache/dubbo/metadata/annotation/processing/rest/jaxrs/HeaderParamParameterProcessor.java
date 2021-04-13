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

import org.apache.dubbo.metadata.annotation.processing.rest.AbstractAnnotatedMethodParameterProcessor;
import org.apache.dubbo.metadata.annotation.processing.rest.AnnotatedMethodParameterProcessor;
import org.apache.dubbo.metadata.rest.RequestMetadata;
import org.apache.dubbo.metadata.rest.RestMethodMetadata;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

import static org.apache.dubbo.metadata.annotation.processing.rest.AnnotatedMethodParameterProcessor.buildDefaultValue;
import static org.apache.dubbo.metadata.rest.RestMetadataConstants.JAX_RS.HEADER_PARAM_ANNOTATION_CLASS_NAME;

/**
 * The {@link AnnotatedMethodParameterProcessor} implementation for JAX-RS's @HeaderParam
 *
 * @since 2.7.6
 */
public class HeaderParamParameterProcessor extends AbstractAnnotatedMethodParameterProcessor {

    @Override
    public String getAnnotationType() {
        return HEADER_PARAM_ANNOTATION_CLASS_NAME;
    }

    @Override
    protected void process(String headerName, String defaultValue, AnnotationMirror annotation,
                           VariableElement parameter, int parameterIndex,
                           ExecutableElement method, RestMethodMetadata restMethodMetadata) {
        RequestMetadata requestMetadata = restMethodMetadata.getRequest();
        // Add the placeholder as header value
        requestMetadata.addHeader(headerName, buildDefaultValue(parameterIndex));
    }
}
