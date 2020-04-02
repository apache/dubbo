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

import org.apache.dubbo.metadata.definition.model.TypeDefinition;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;

import static org.apache.dubbo.metadata.annotation.processing.util.FieldUtils.getNonStaticFields;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.getType;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.isClassType;

/**
 * {@link TypeDefinitionBuilder} for General Object
 *
 * @since 2.7.6
 */
public class GeneralTypeDefinitionBuilder implements DeclaredTypeDefinitionBuilder {

    @Override
    public boolean accept(ProcessingEnvironment processingEnv, DeclaredType type) {
        return isClassType(type);
    }

    @Override
    public void build(ProcessingEnvironment processingEnv, DeclaredType type, TypeDefinition typeDefinition) {

        String typeName = type.toString();

        TypeElement typeElement = getType(processingEnv, typeName);

        buildProperties(processingEnv, typeElement, typeDefinition);
    }

    protected void buildProperties(ProcessingEnvironment processingEnv, TypeElement type, TypeDefinition definition) {
        getNonStaticFields(type).forEach(field -> {
            String fieldName = field.getSimpleName().toString();
            TypeDefinition propertyType = TypeDefinitionBuilder.build(processingEnv, field);
            if (propertyType != null) {
                definition.getProperties().put(fieldName, propertyType);
            }
        });
    }

    @Override
    public int getPriority() {
        return MIN_PRIORITY;
    }
}
