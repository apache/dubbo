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
package org.apache.dubbo.metadata.definition;

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.metadata.definition.builder.DefaultTypeBuilder;
import org.apache.dubbo.metadata.definition.builder.TypeBuilder;
import org.apache.dubbo.metadata.definition.model.TypeDefinition;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 2015/1/27.
 */
public class TypeDefinitionBuilder {
    private static final Logger logger = LoggerFactory.getLogger(TypeDefinitionBuilder.class);
    static final List<TypeBuilder> BUILDERS;

    static {
        ExtensionLoader<TypeBuilder> extensionLoader = ExtensionLoader.getExtensionLoader(TypeBuilder.class);
        Set<TypeBuilder> tbs = extensionLoader.getSupportedExtensionInstances();
        BUILDERS = new ArrayList<>(tbs);
    }

    public static TypeDefinition build(Type type, Class<?> clazz, Map<Class<?>, TypeDefinition> typeCache) {
        TypeBuilder builder = getGenericTypeBuilder(type, clazz);
        TypeDefinition td;
        if (builder != null) {
            td = builder.build(type, clazz, typeCache);
            td.setTypeBuilderName(builder.getClass().getName());
        } else {
            td = DefaultTypeBuilder.build(clazz, typeCache);
            td.setTypeBuilderName(DefaultTypeBuilder.class.getName());
        }
        return td;
    }

    private static TypeBuilder getGenericTypeBuilder(Type type, Class<?> clazz) {
        for (TypeBuilder builder : BUILDERS) {
            try {
                if (builder.accept(type, clazz)) {
                    return builder;
                }
            } catch (NoClassDefFoundError cnfe) {
                //ignore
                logger.info("Throw classNotFound (" + cnfe.getMessage() + ") in " + builder.getClass());
            }
        }
        return null;
    }

    private Map<Class<?>, TypeDefinition> typeCache = new HashMap<>();

    public TypeDefinition build(Type type, Class<?> clazz) {
        return build(type, clazz, typeCache);
    }

    public List<TypeDefinition> getTypeDefinitions() {
        return new ArrayList<>(typeCache.values());
    }

}
