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
import java.util.List;
import java.util.Map;

import static org.apache.dubbo.metadata.rest.RestMetadataConstants.JAX_RS.DEFAULT_VALUE_ANNOTATION_CLASS_NAME;


/**
 * The {@link AnnotatedMethodParameterProcessor} implementation for JAX-RS's @DefaultValue
 * *
 *
 * @since 2.7.6
 */
public class DefaultValueParameterProcessor extends AbstractAnnotatedMethodParameterProcessor {

    @Override
    public String getAnnotationType() {
        return DEFAULT_VALUE_ANNOTATION_CLASS_NAME;
    }

    @Override
    protected void process(String annotationValue, String defaultValue, AnnotationMirror annotation, VariableElement parameter, int parameterIndex, ExecutableElement method, RestMethodMetadata restMethodMetadata) {
        RequestMetadata requestMetadata = restMethodMetadata.getRequest();

        // process the request parameters
        setDefaultValue(requestMetadata.getParams(), defaultValue, annotationValue);
        // process the request headers
        setDefaultValue(requestMetadata.getHeaders(), defaultValue, annotationValue);

    }

    private void setDefaultValue(Map<String, List<String>> source, String placeholderValue, String defaultValue) {
        OUTTER:
        for (Map.Entry<String, List<String>> entry : source.entrySet()) {
            List<String> values = entry.getValue();
            int size = values.size();
            for (int i = 0; i < size; i++) {
                String value = values.get(i);
                if (placeholderValue.equals(value)) {
                    values.set(i, defaultValue);
                    break OUTTER;
                }
            }
        }
    }

    @Override
    public int getPriority() {
        return MIN_PRIORITY;
    }
}
