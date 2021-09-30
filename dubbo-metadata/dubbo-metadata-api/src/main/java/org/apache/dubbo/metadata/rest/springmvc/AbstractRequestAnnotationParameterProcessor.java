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

import org.apache.dubbo.common.utils.AnnotationUtils;
import org.apache.dubbo.metadata.rest.AbstractAnnotatedMethodParameterProcessor;
import org.apache.dubbo.metadata.rest.AnnotatedMethodParameterProcessor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.util.Objects;

import static org.apache.dubbo.common.utils.AnnotationUtils.getAttribute;

/**
 * The abstract {@link AnnotatedMethodParameterProcessor} implementation for Spring Web MVC's @Request*
 */
public abstract class AbstractRequestAnnotationParameterProcessor extends AbstractAnnotatedMethodParameterProcessor {

    @Override
    protected String getAnnotationValue(Annotation annotation, Parameter parameter, int parameterIndex) {
        // try to get "value" attribute first
        String name = super.getAnnotationValue(annotation, parameter, parameterIndex);

        // try to get "name" attribute if required
        if (isEmpty(name)) {
            name = getAttribute(annotation, "name");
        }

        // finally , try to the name of parameter
        if (isEmpty(name)) {
            name = parameter.getName();
        }

        return name;
    }

    @Override
    protected String getDefaultValue(Annotation annotation, Parameter parameter, int parameterIndex) {
        String attributeName = "defaultValue";
        String attributeValue = getAttribute(annotation, attributeName);

        if (isEmpty(attributeValue) || isDefaultValue(annotation, attributeName, attributeValue)) {
            attributeValue = super.getDefaultValue(annotation, parameter, parameterIndex);
        }
        return attributeValue;
    }

    private boolean isDefaultValue(Annotation annotation, String attributeName, Object attributeValue) {
        String defaultValue = AnnotationUtils.getDefaultValue(annotation, attributeName);
        return Objects.deepEquals(attributeValue, defaultValue);
    }

    protected boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }
}
