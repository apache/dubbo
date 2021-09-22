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
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/**
 * 2015/1/27.
 */
public class CollectionTypeBuilder implements TypeBuilder {

    @Override
    public boolean accept(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }
        return Collection.class.isAssignableFrom(clazz);
    }

    @Override
    public TypeDefinition build(Type type, Class<?> clazz, Map<String, TypeDefinition> typeCache) {
        if (!(type instanceof ParameterizedType)) {
            return new TypeDefinition(clazz.getCanonicalName());
        }

        ParameterizedType parameterizedType = (ParameterizedType) type;
        Type[] actualTypeArgs = parameterizedType.getActualTypeArguments();
        if (actualTypeArgs == null || actualTypeArgs.length != 1) {
            throw new IllegalArgumentException(MessageFormat.format(
                    "[ServiceDefinitionBuilder] Collection type [{0}] with unexpected amount of arguments [{1}]."
                            + Arrays.toString(actualTypeArgs),
                    type, actualTypeArgs));
        }

        String colType = ClassUtils.getCanonicalNameForParameterizedType(parameterizedType);
        TypeDefinition td = typeCache.get(colType);
        if (td != null) {
            return td;
        }
        td = new TypeDefinition(colType);
        typeCache.put(colType, td);

        Type actualType = actualTypeArgs[0];
        TypeDefinition itemTd = null;
        if (actualType instanceof ParameterizedType) {
            // Nested collection or map.
            Class<?> rawType = (Class<?>) ((ParameterizedType) actualType).getRawType();
            itemTd = TypeDefinitionBuilder.build(actualType, rawType, typeCache);
        } else if (actualType instanceof Class<?>) {
            Class<?> actualClass = (Class<?>) actualType;
            itemTd = TypeDefinitionBuilder.build(null, actualClass, typeCache);
        }
        if (itemTd != null) {
            td.getItems().add(itemTd.getType());
        }

        return td;
    }

}
