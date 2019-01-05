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
import org.apache.dubbo.metadata.definition.util.JaketConfigurationUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * 2015/1/27.
 */
public final class DefaultTypeBuilder {

    public static TypeDefinition build(Class<?> clazz, Map<Class<?>, TypeDefinition> typeCache) {
//        final String canonicalName = clazz.getCanonicalName();
        final String name = clazz.getName();

        TypeDefinition td = new TypeDefinition(name);
        // Try to get a cached definition
        if (typeCache.containsKey(clazz)) {
            return typeCache.get(clazz);
        }

        // Primitive type
        if (!JaketConfigurationUtils.needAnalyzing(clazz)) {
            return td;
        }

        // Custom type
        TypeDefinition ref = new TypeDefinition(name);
        ref.set$ref(name);
        typeCache.put(clazz, ref);

        List<Field> fields = ClassUtils.getNonStaticFields(clazz);
        for (Field field : fields) {
            String fieldName = field.getName();
            Class<?> fieldClass = field.getType();
            Type fieldType = field.getGenericType();

            TypeDefinition fieldTd = TypeDefinitionBuilder.build(fieldType, fieldClass, typeCache);
            td.getProperties().put(fieldName, fieldTd);
        }

        typeCache.put(clazz, td);
        return td;
    }

    private DefaultTypeBuilder() {
    }
}
