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

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.metadata.definition.builder.DefaultTypeBuilder;
import org.apache.dubbo.metadata.definition.builder.TypeBuilder;
import org.apache.dubbo.metadata.definition.model.TypeDefinition;
import org.apache.dubbo.rpc.model.FrameworkModel;

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
    public static List<TypeBuilder> BUILDERS;

    public static void initBuilders(FrameworkModel model) {
        Set<TypeBuilder> tbs = model.getExtensionLoader(TypeBuilder.class).getSupportedExtensionInstances();
        BUILDERS = new ArrayList<>(tbs);
    }

    public static TypeDefinition build(Type type, Class<?> clazz, Map<String, TypeDefinition> typeCache) {
        TypeBuilder builder = getGenericTypeBuilder(clazz);
        TypeDefinition td;

        if (clazz.isPrimitive() || ClassUtils.isSimpleType(clazz)) { // changed since 2.7.6
            td = new TypeDefinition(clazz.getCanonicalName());
            typeCache.put(clazz.getCanonicalName(), td);
        } else if (builder != null) {
            td = builder.build(type, clazz, typeCache);
        } else {
            td = DefaultTypeBuilder.build(clazz, typeCache);
        }
        return td;
    }

    private static TypeBuilder getGenericTypeBuilder(Class<?> clazz) {
        for (TypeBuilder builder : BUILDERS) {
            try {
                if (builder.accept(clazz)) {
                    return builder;
                }
            } catch (NoClassDefFoundError cnfe) {
                //ignore
                logger.info("Throw classNotFound (" + cnfe.getMessage() + ") in " + builder.getClass());
            }
        }
        return null;
    }

    private final Map<String, TypeDefinition> typeCache = new HashMap<>();

    public TypeDefinition build(Type type, Class<?> clazz) {
        return build(type, clazz, typeCache);
    }

    public List<TypeDefinition> getTypeDefinitions() {
        return new ArrayList<>(typeCache.values());
    }

}
