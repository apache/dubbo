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

import org.apache.dubbo.metadata.annotation.processing.rest.AbstractAnnotatedMethodParameterProcessor;
import org.apache.dubbo.metadata.annotation.processing.rest.AnnotatedMethodParameterProcessor;
import org.apache.dubbo.metadata.rest.RestMethodMetadata;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationUtils.getAttribute;

/**
 * The abstract {@link AnnotatedMethodParameterProcessor} implementation for Spring Web MVC's @Request*
 */
public abstract class AbstractRequestAnnotationParameterProcessor extends AbstractAnnotatedMethodParameterProcessor {


    protected abstract void process(String name, String defaultValue, AnnotationMirror annotation,
                                    VariableElement parameter, int parameterIndex,
                                    ExecutableElement method,
                                    RestMethodMetadata restMethodMetadata);

    @Override
    protected String getAnnotationValue(AnnotationMirror annotation, VariableElement parameter, int parameterIndex) {
        // try to get "value" attribute first
        String name = super.getAnnotationValue(annotation, parameter, parameterIndex);

        // try to get "name" attribute if required
        if (isEmpty(name)) {
            name = getAttribute(annotation, "name");
        }

        // finally , try to the name of parameter
        if (isEmpty(name)) {
            name = parameter.getSimpleName().toString();
        }

        return name;
    }

    protected String getDefaultValue(AnnotationMirror annotation, VariableElement parameter, int parameterIndex) {
        String defaultValue = getAttribute(annotation, "defaultValue");
        if (isEmpty(defaultValue)) {
            defaultValue = super.getDefaultValue(annotation, parameter, parameterIndex);
        }
        return defaultValue;
    }

    protected boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }
}
