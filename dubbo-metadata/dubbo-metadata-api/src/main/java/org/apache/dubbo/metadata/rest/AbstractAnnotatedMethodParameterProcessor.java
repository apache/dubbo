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
import static org.apache.dubbo.common.utils.ClassUtils.getClassLoader;
import static org.apache.dubbo.common.utils.ClassUtils.resolveClass;

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
        addArgInfo(parameter, parameterIndex, restMethodMetadata, annotationValue, defaultValue);
        process(annotationValue, defaultValue, annotation, parameter, parameterIndex, method, restMethodMetadata);
    }


    protected void process(String annotationValue, String defaultValue, Annotation annotation, Parameter parameter,
                           int parameterIndex, Method method, RestMethodMetadata restMethodMetadata) {

    }


    @Override
    public Class getAnnotationClass() {
        return resolveClass(getAnnotationName(), getClassLoader());
    }

    protected void addArgInfo(Parameter parameter, int parameterIndex,
                              RestMethodMetadata restMethodMetadata, String annotationValue, Object defaultValue) {
        ArgInfo argInfo = ArgInfo.build(parameterIndex, parameter)
            .setParamAnnotationType(getAnnotationClass())
            .setAnnotationNameAttribute(annotationValue).setDefaultValue(defaultValue);
        restMethodMetadata.addArgInfo(argInfo);
    }

    protected String getAnnotationValue(Annotation annotation, Parameter parameter, int parameterIndex) {
        return getValue(annotation);
    }

    protected String getDefaultValue(Annotation annotation, Parameter parameter, int parameterIndex) {
        return AnnotatedMethodParameterProcessor.buildDefaultValue(parameterIndex);
    }

}
