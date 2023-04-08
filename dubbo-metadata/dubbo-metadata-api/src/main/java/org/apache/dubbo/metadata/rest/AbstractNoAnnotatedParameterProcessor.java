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

import org.apache.dubbo.metadata.rest.media.MediaType;

import java.lang.reflect.Parameter;
import java.util.Set;

import static org.apache.dubbo.common.utils.ClassUtils.getClassLoader;
import static org.apache.dubbo.common.utils.ClassUtils.resolveClass;

public abstract class AbstractNoAnnotatedParameterProcessor implements NoAnnotatedParameterRequestTagProcessor {

    public boolean process(Parameter parameter, int parameterIndex, RestMethodMetadata restMethodMetadata) {
        MediaType mediaType = consumerContentType();
        if (!contentTypeSupport(restMethodMetadata, mediaType, parameter.getType())) {
            return false;
        }
        boolean isFormBody = isFormContentType(restMethodMetadata);
        addArgInfo(parameter, parameterIndex, restMethodMetadata, isFormBody);
        return true;
    }

    private boolean contentTypeSupport(RestMethodMetadata restMethodMetadata, MediaType mediaType, Class paramType) {

        // @RequestParam String,number param
        if (mediaType.equals(MediaType.ALL_VALUE) && (String.class == paramType || paramType.isPrimitive() || Number.class.isAssignableFrom(paramType))) {
            return true;
        }

        Set<String> consumes = restMethodMetadata.getRequest().getConsumes();
        for (String consume : consumes) {
            if (consume.contains(mediaType.value)) {
                return true;
            }
        }

        return false;
    }

    protected boolean isFormContentType(RestMethodMetadata restMethodMetadata) {

        return false;
    }


    protected void addArgInfo(Parameter parameter, int parameterIndex,
                              RestMethodMetadata restMethodMetadata, boolean isFormBody) {
        ArgInfo argInfo = ArgInfo.build(parameterIndex, parameter)
            .setParamAnnotationType(resolveClass(defaultAnnotationClassName(restMethodMetadata), getClassLoader()))
            .setAnnotationNameAttribute(parameter.getName()).setFormContentType(isFormBody);
        restMethodMetadata.addArgInfo(argInfo);
    }
}
