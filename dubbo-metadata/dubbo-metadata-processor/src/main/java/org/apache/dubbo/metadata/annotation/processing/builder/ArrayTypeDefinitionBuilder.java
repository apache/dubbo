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
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeMirror;
import java.lang.reflect.Array;

import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.isArrayType;

/**
 * {@link TypeDefinitionBuilder} for Java {@link Array}
 *
 * @since 2.7.6
 */
public class ArrayTypeDefinitionBuilder implements TypeDefinitionBuilder<ArrayType> {

    @Override
    public boolean accept(ProcessingEnvironment processingEnv, TypeMirror type) {
        return isArrayType(type);
    }

    @Override
    public void build(ProcessingEnvironment processingEnv, ArrayType type, TypeDefinition typeDefinition) {
        TypeMirror componentType = type.getComponentType();
        typeDefinition.getItems().add(TypeDefinitionBuilder.build(processingEnv, componentType));
    }

    @Override
    public int getPriority() {
        return MIN_PRIORITY - 4;
    }
}
