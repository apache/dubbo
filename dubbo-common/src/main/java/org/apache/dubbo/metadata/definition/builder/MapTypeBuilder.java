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
import org.apache.dubbo.metadata.definition.util.ClassUtils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

import static org.apache.dubbo.common.utils.TypeUtils.getRawClass;
import static org.apache.dubbo.common.utils.TypeUtils.isClass;
import static org.apache.dubbo.common.utils.TypeUtils.isParameterizedType;

/**
 * 2015/1/27.
 */
public class MapTypeBuilder implements TypeBuilder {

    @Override
    public boolean accept(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }
        return Map.class.isAssignableFrom(clazz);
    }

    @Override
    public TypeDefinition build(Type type, Class<?> clazz, Map<String, TypeDefinition> typeCache) {
        if (!(type instanceof ParameterizedType)) {
            return new TypeDefinition(clazz.getCanonicalName());
        }

        ParameterizedType parameterizedType = (ParameterizedType) type;
        Type[] actualTypeArgs = parameterizedType.getActualTypeArguments();
        int actualTypeArgsLength = actualTypeArgs == null ? 0 : actualTypeArgs.length;

        String mapType = ClassUtils.getCanonicalNameForParameterizedType(parameterizedType);

        TypeDefinition td = typeCache.get(mapType);
        if (td != null) {
            return td;
        }
        td = new TypeDefinition(mapType);
        typeCache.put(mapType, td);

        for (int i = 0; i < actualTypeArgsLength; i++) {
            Type actualType = actualTypeArgs[i];
            TypeDefinition item = null;
            Class<?> rawType = getRawClass(actualType);
            if (isParameterizedType(actualType)) {
                // Nested collection or map.
                item = TypeDefinitionBuilder.build(actualType, rawType, typeCache);
            } else if (isClass(actualType)) {
                item = TypeDefinitionBuilder.build(null, rawType, typeCache);
            }
            if (item != null) {
                td.getItems().add(item.getType());
            }
        }
        return td;
    }
}
