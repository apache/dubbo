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
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.Set;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationProcessorUtils.getFields;
import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationProcessorUtils.getType;

/**
 * {@link TypeDefinitionBuilder} for Java {@link Enum}
 *
 * @since 2.7.5
 */
public class EnumTypeDefinitionBuilder implements TypeDefinitionBuilder {

    @Override
    public boolean accept(ProcessingEnvironment processingEnv, TypeMirror type) {
        if (type instanceof DeclaredType) {
            DeclaredType declaredType = (DeclaredType) type;
            Element element = declaredType.asElement();
            return ElementKind.ENUM.equals(element.getKind());
        }
        return false;
    }

    @Override
    public void build(ProcessingEnvironment processingEnv, TypeMirror type, TypeDefinition typeDefinition) {
        getFields(processingEnv, getType(processingEnv, type), field -> {  // Enum's members must be public static final fields
            Set<Modifier> modifiers = field.getModifiers();
            return modifiers.contains(PUBLIC) && modifiers.contains(STATIC) && modifiers.contains(FINAL);
        })
                .stream()
                .map(Element::getSimpleName)
                .map(Name::toString)
                .forEach(typeDefinition.getEnums()::add);
    }
}
