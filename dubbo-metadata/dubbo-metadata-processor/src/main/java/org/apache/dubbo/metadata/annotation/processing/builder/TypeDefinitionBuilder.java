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

import org.apache.dubbo.common.lang.Prioritized;
import org.apache.dubbo.metadata.definition.model.TypeDefinition;
import org.apache.dubbo.rpc.model.ApplicationModel;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import java.util.Map;

/**
 * A class builds the instance of {@link TypeDefinition}
 *
 * @since 2.7.6
 */
public interface TypeDefinitionBuilder<T extends TypeMirror> extends Prioritized {

    /**
     * Build the instance of {@link TypeDefinition} from the specified {@link Element element}
     *
     * @param processingEnv {@link ProcessingEnvironment}
     * @param element       {@link Element source element}
     * @return non-null
     */
    static TypeDefinition build(ProcessingEnvironment processingEnv, Element element, Map<String, TypeDefinition> typeCache) {
        TypeDefinition typeDefinition = build(processingEnv, element.asType(), typeCache);
        // Comment this code for the compatibility
        // typeDefinition.set$ref(element.toString());
        return typeDefinition;
    }

    /**
     * Build the instance of {@link TypeDefinition} from the specified {@link TypeMirror type}
     *
     * @param processingEnv {@link ProcessingEnvironment}
     * @param type          {@link TypeMirror type}
     * @return non-null
     */
    static TypeDefinition build(ProcessingEnvironment processingEnv, TypeMirror type, Map<String, TypeDefinition> typeCache) {
        // Build by all instances of TypeDefinitionBuilder that were loaded By Java SPI

        TypeDefinition typeDefinition = ApplicationModel.defaultModel()
                .getExtensionLoader(TypeBuilder.class)
                .getSupportedExtensionInstances()
                .stream()
//        load(TypeDefinitionBuilder.class, TypeDefinitionBuilder.class.getClassLoader())
                .filter(builder -> builder.accept(processingEnv, type))
                .findFirst()
                .map(builder -> {
                    return builder.build(processingEnv, type, typeCache);
                    // typeDefinition.setTypeBuilderName(builder.getClass().getName());
                }).orElse(null);

        if (typeDefinition != null) {
            typeCache.put(typeDefinition.getType(), typeDefinition);
        }
        return typeDefinition;
    }
}
