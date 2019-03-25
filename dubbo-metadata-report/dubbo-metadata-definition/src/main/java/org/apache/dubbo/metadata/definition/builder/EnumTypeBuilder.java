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

import org.apache.dubbo.metadata.definition.model.TypeDefinition;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * 2015/1/27.
 */
public class EnumTypeBuilder implements TypeBuilder {

    @Override
    public boolean accept(Type type, Class<?> clazz) {
        if (clazz == null) {
            return false;
        }
        return clazz.isEnum();
    }

    @Override
    public TypeDefinition build(Type type, Class<?> clazz, Map<Class<?>, TypeDefinition> typeCache) {
        TypeDefinition td = new TypeDefinition(clazz.getCanonicalName());

        try {
            Method methodValues = clazz.getDeclaredMethod("values", new Class<?>[0]);
            Object[] values = (Object[]) methodValues.invoke(clazz, new Object[0]);
            int length = values.length;
            for (int i = 0; i < length; i++) {
                Object value = values[i];
                td.getEnums().add(value.toString());
            }
        } catch (Throwable t) {
            td.setId("-1");
        }

        typeCache.put(clazz, td);
        return td;
    }

}
