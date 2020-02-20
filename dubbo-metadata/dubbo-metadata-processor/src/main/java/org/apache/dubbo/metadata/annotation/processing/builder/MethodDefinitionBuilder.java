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
package org.apache.dubbo.metadata.annotation.processing.builder;

import org.apache.dubbo.metadata.definition.model.MethodDefinition;
import org.apache.dubbo.metadata.definition.model.TypeDefinition;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.dubbo.metadata.annotation.processing.util.MethodUtils.getMethodName;
import static org.apache.dubbo.metadata.annotation.processing.util.MethodUtils.getMethodParameterTypes;
import static org.apache.dubbo.metadata.annotation.processing.util.MethodUtils.getReturnType;

/**
 * A Builder class for {@link MethodDefinition}
 *
 * @see MethodDefinition
 * @since 2.7.6
 */
public interface MethodDefinitionBuilder {

    static MethodDefinition build(ProcessingEnvironment processingEnv, ExecutableElement method) {
        MethodDefinition methodDefinition = new MethodDefinition();
        methodDefinition.setName(getMethodName(method));
        methodDefinition.setReturnType(getReturnType(method));
        methodDefinition.setParameterTypes(getMethodParameterTypes(method));
        methodDefinition.setParameters(getMethodParameters(processingEnv, method));
        return methodDefinition;
    }

    static List<TypeDefinition> getMethodParameters(ProcessingEnvironment processingEnv, ExecutableElement method) {
        return method.getParameters().stream()
                .map(element -> TypeDefinitionBuilder.build(processingEnv, element))
                .collect(Collectors.toList());
    }
}
