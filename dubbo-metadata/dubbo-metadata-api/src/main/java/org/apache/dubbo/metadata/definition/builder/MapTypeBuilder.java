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
import java.util.Arrays;
import java.util.Map;

import static org.apache.dubbo.common.utils.StringUtils.replace;
import static org.apache.dubbo.common.utils.TypeUtils.getRawClass;
import static org.apache.dubbo.common.utils.TypeUtils.isClass;
import static org.apache.dubbo.common.utils.TypeUtils.isParameterizedType;

/**
 * 2015/1/27.
 */
public class MapTypeBuilder implements TypeBuilder {

    @Override
    public boolean accept(Type type, Class<?> clazz) {
        if (clazz == null) {
            return false;
        }
        return Map.class.isAssignableFrom(clazz);
    }

    @Override
    public TypeDefinition build(Type type, Class<?> clazz, Map<Class<?>, TypeDefinition> typeCache) {
        if (!(type instanceof ParameterizedType)) {
            return new TypeDefinition(clazz.getName());
        }

        ParameterizedType parameterizedType = (ParameterizedType) type;
        Type[] actualTypeArgs = parameterizedType.getActualTypeArguments();
        int actualTypeArgsLength = actualTypeArgs == null ? 0 : actualTypeArgs.length;

        if (actualTypeArgsLength != 2) {
            throw new IllegalArgumentException(MessageFormat.format(
                    "[ServiceDefinitionBuilder] Map type [{0}] with unexpected amount of arguments [{1}]."
                            + Arrays.toString(actualTypeArgs), type, actualTypeArgs));
        }

        // Change since 2.7.6
        /**
         * Replacing <code>", "</code> to <code>","</code> will not change the semantic of
         * {@link ParameterizedType#toString()}
         * @see sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl
         */
        String mapType = replace(type.toString(), ", ", ",");

        TypeDefinition typeDefinition = new TypeDefinition(mapType);

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
            typeDefinition.getItems().add(item);
        }

        return typeDefinition;
    }
}
