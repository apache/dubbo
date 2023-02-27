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
package org.apache.dubbo.metadata.rest.jaxrs;

import org.apache.dubbo.metadata.rest.AbstractAnnotatedMethodParameterProcessor;
import org.apache.dubbo.metadata.rest.AnnotatedMethodParameterProcessor;
import org.apache.dubbo.metadata.rest.ArgInfo;
import org.apache.dubbo.metadata.rest.RestMethodMetadata;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import static org.apache.dubbo.metadata.rest.RestMetadataConstants.JAX_RS.REST_EASY_BODY_ANNOTATION_CLASS_NAME;

/**
 * The {@link AnnotatedMethodParameterProcessor} implementation for JAX-RS's @FormParam
 *
 * @since 2.7.6
 */
public class BodyParameterProcessor extends AbstractAnnotatedMethodParameterProcessor {

    @Override
    public String getAnnotationName() {
        return REST_EASY_BODY_ANNOTATION_CLASS_NAME;
    }

    @Override
    public void process(Annotation annotation, Parameter parameter, int parameterIndex, Method method, Class<?> serviceType, Class<?> serviceInterfaceClass, RestMethodMetadata restMethodMetadata) {
        ArgInfo argInfo = ArgInfo.
            build(parameterIndex, parameter).
            setParamAnnotationType(getAnnotationClass());
        restMethodMetadata.addArgInfo(argInfo);

    }


}
