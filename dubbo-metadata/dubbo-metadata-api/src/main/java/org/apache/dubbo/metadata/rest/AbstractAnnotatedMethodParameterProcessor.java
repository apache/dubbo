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
package org.apache.dubbo.metadata.rest;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import static org.apache.dubbo.common.utils.AnnotationUtils.getValue;
import static org.apache.dubbo.metadata.rest.AnnotatedMethodParameterProcessor.buildDefaultValue;

/**
 * The abstract {@link AnnotatedMethodParameterProcessor} implementation
 *
 * @since 2.7.6
 */
public abstract class AbstractAnnotatedMethodParameterProcessor implements AnnotatedMethodParameterProcessor {

    @Override
    public void process(Annotation annotation, Parameter parameter, int parameterIndex, Method method,
                        Class<?> serviceType, Class<?> serviceInterfaceClass, RestMethodMetadata restMethodMetadata) {
        String annotationValue = getAnnotationValue(annotation, parameter, parameterIndex);
        String defaultValue = getDefaultValue(annotation, parameter, parameterIndex);
        process(annotationValue, defaultValue, annotation, parameter, parameterIndex, method, restMethodMetadata);
    }

    protected String getAnnotationValue(Annotation annotation, Parameter parameter, int parameterIndex) {
        return getValue(annotation);
    }

    protected String getDefaultValue(Annotation annotation, Parameter parameter, int parameterIndex) {
        return buildDefaultValue(parameterIndex);
    }

    protected abstract void process(String annotationValue, String defaultValue, Annotation annotation, Object parameter,
                                    int parameterIndex, Method method, RestMethodMetadata restMethodMetadata);
}
