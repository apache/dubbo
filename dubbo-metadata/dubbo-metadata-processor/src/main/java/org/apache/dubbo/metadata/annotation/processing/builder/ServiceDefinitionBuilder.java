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

import org.apache.dubbo.metadata.definition.model.ServiceDefinition;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;

import static org.apache.dubbo.metadata.annotation.processing.util.MethodUtils.getPublicNonStaticMethods;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.getHierarchicalTypes;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.getResourceName;

/**
 * A Builder for {@link ServiceDefinition}
 *
 * @see ServiceDefinition
 * @since 2.7.6
 */
public interface ServiceDefinitionBuilder {

    static ServiceDefinition build(ProcessingEnvironment processingEnv, TypeElement type) {
        ServiceDefinition serviceDefinition = new ServiceDefinition();
        serviceDefinition.setCanonicalName(type.toString());
        serviceDefinition.setCodeSource(getResourceName(type.toString()));

        // Get all super types and interface excluding the specified type
        // and then the result will be added into ServiceDefinition#getTypes()
        getHierarchicalTypes(type.asType(), Object.class)
                .stream()
                .map(t -> TypeDefinitionBuilder.build(processingEnv, t))
                .forEach(serviceDefinition.getTypes()::add);

        // Get all declared methods that will be added into ServiceDefinition#getMethods()
        getPublicNonStaticMethods(type, Object.class)
                .stream()
                .map(method -> MethodDefinitionBuilder.build(processingEnv, method))
                .forEach(serviceDefinition.getMethods()::add);

        return serviceDefinition;
    }
}
