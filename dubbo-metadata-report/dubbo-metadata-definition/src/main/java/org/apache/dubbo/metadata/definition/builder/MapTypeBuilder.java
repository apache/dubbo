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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.MessageFormat;
import java.util.Map;

/**
 * 2015/1/27.
 */
public class MapTypeBuilder implements TypeBuilder {

    @Override
    public boolean accept(Type type, Class<?> clazz) {
        if (clazz == null) {
            return false;
        }

        if (Map.class.isAssignableFrom(clazz)) {
            return true;
        }

        return false;
    }

    @Override
    public TypeDefinition build(Type type, Class<?> clazz, Map<Class<?>, TypeDefinition> typeCache) {
        if (!(type instanceof ParameterizedType)) {
            return new TypeDefinition(clazz.getName());
        }

        ParameterizedType parameterizedType = (ParameterizedType) type;
        Type[] actualTypeArgs = parameterizedType.getActualTypeArguments();
        if (actualTypeArgs == null || actualTypeArgs.length != 2) {
            throw new IllegalArgumentException(MessageFormat.format(
                    "[ServiceDefinitionBuilder] Map type [{0}] with unexpected amount of arguments [{1}]." + actualTypeArgs, new Object[]{
                            type, actualTypeArgs}));
        }

        for (Type actualType : actualTypeArgs) {
            if (actualType instanceof ParameterizedType) {
                // Nested collection or map.
                Class<?> rawType = (Class<?>) ((ParameterizedType) actualType).getRawType();
                TypeDefinitionBuilder.build(actualType, rawType, typeCache);
            } else if (actualType instanceof Class<?>) {
                Class<?> actualClass = (Class<?>) actualType;
                if (actualClass.isArray() || actualClass.isEnum()) {
                    TypeDefinitionBuilder.build(null, actualClass, typeCache);
                } else {
                    DefaultTypeBuilder.build(actualClass, typeCache);
                }
            }
        }

        return new TypeDefinition(type.toString());
    }
}
