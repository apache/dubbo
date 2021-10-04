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
package org.apache.dubbo.metadata.definition.builder;

import org.apache.dubbo.metadata.definition.TypeDefinitionBuilder;
import org.apache.dubbo.metadata.definition.model.TypeDefinition;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * 2015/1/27.
 */
public class ArrayTypeBuilder implements TypeBuilder {

    @Override
    public boolean accept(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }
        return clazz.isArray();
    }

    @Override
    public TypeDefinition build(Type type, Class<?> clazz, Map<String, TypeDefinition> typeCache) {
        final String canonicalName = clazz.getCanonicalName();
        TypeDefinition td = typeCache.get(canonicalName);
        if (td != null) {
            return td;
        }
        td = new TypeDefinition(canonicalName);
        typeCache.put(canonicalName, td);
        // Process the component type of array.
        Class<?> componentType = clazz.getComponentType();
        TypeDefinition itemTd = TypeDefinitionBuilder.build(componentType, componentType, typeCache);
        if (itemTd != null) {
            td.getItems().add(itemTd.getType());
        }
        return td;
    }

}
