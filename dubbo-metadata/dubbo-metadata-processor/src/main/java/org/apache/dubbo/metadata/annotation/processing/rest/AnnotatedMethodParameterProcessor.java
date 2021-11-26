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

import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.common.lang.Prioritized;
import org.apache.dubbo.metadata.rest.RestMethodMetadata;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

/**
 * The interface to process the parameter of method that was annotated
 *
 * @since 2.7.6
 */
@SPI
public interface AnnotatedMethodParameterProcessor extends Prioritized {

    /**
     * The string presenting the annotation type
     *
     * @return non-null
     */
    String getAnnotationType();

    /**
     * Process the specified method {@link VariableElement parameter}
     *
     * @param annotation         {@link AnnotationMirror the target annotation} whose type is {@link #getAnnotationType()}
     * @param parameter          {@link VariableElement method parameter}
     * @param parameterIndex     the index of parameter in the method
     * @param method             {@link ExecutableElement method that parameter belongs to}
     * @param restMethodMetadata {@link RestMethodMetadata the metadata is used to update}
     */
    void process(AnnotationMirror annotation, VariableElement parameter, int parameterIndex, ExecutableElement method,
                 RestMethodMetadata restMethodMetadata);


    /**
     * Build the default value
     *
     * @param parameterIndex the index of parameter
     * @return the placeholder
     */
    static String buildDefaultValue(int parameterIndex) {
        return "{" + parameterIndex + "}";
    }
}
