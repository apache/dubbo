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
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.Objects;

import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationProcessorUtils.getHierarchicalTypes;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationProcessorUtils.getNonStaticFields;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationProcessorUtils.getType;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.isSimpleType;

/**
 * TODO
 *
 * @since 2.7.5
 */
public class DefaultTypeDefinitionBuilder implements TypeDefinitionBuilder {

    @Override
    public boolean accept(ProcessingEnvironment processingEnv, TypeMirror type) {
        return false;
    }

    @Override
    public void build(ProcessingEnvironment processingEnv, TypeMirror type, TypeDefinition typeDefinition) {

        String typeName = type.toString();

        TypeDefinition definition = new TypeDefinition(typeName);

        definition.set$ref(typeName);

        if (isSimpleType(type)) {
            return;
        }

        TypeElement typeElement = getType(processingEnv, typeName);

        ElementKind kind = typeElement.getKind();

        switch (kind) {
            case ENUM:
                processEnum(processingEnv, typeElement, definition);
                break;
            case CLASS:
                process(processingEnv, typeElement, definition);
                break;
        }
    }

    protected void process(ProcessingEnvironment processingEnv, TypeElement type, TypeDefinition definition) {

        processSuperTypes(processingEnv, type, definition);

        processProperties(processingEnv, type, definition);

    }

    protected void processSuperTypes(ProcessingEnvironment processingEnv, TypeElement type, TypeDefinition definition) {
        getHierarchicalTypes(processingEnv, type, false, true, true)
                .stream()
                .map(superType -> TypeDefinitionBuilder.build(processingEnv, superType))
                .filter(Objects::nonNull)
                .forEach(definition.getItems()::add);
    }

    protected void processProperties(ProcessingEnvironment processingEnv, TypeElement type, TypeDefinition definition) {

        getNonStaticFields(processingEnv, type).forEach(field -> {
            String fieldName = field.getSimpleName().toString();
            TypeDefinition propertyType = TypeDefinitionBuilder.build(processingEnv, field);
            if (propertyType != null) {
                definition.getProperties().put(fieldName, propertyType);
            }
        });
    }

    protected void processEnum(ProcessingEnvironment processingEnv, Element type, TypeDefinition definition) {

    }

}
