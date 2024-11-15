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
package org.apache.dubbo.rpc.protocol.tri.rest.openapi;

import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.BeanMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.BeanMeta.PropertyMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ParameterMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.TypeParameterMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.OpenAPISchemaResolver.Chain;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.OpenAPISchemaResolver.Context;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Schema;
import org.apache.dubbo.rpc.protocol.tri.rest.util.TypeUtils;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.apache.dubbo.rpc.protocol.tri.rest.openapi.PrimitiveSchema.ARRAY;
import static org.apache.dubbo.rpc.protocol.tri.rest.openapi.PrimitiveSchema.OBJECT;

public final class SchemaFactory {

    private final OpenAPISchemaResolver[] resolvers;
    private final OpenAPISchemaPredicate[] predicates;
    private final Map<Class<?>, Optional<Schema>> schemaMap = CollectionUtils.newConcurrentHashMap();
    private final Map<Class<?>, String> nameMap = CollectionUtils.newConcurrentHashMap();

    public SchemaFactory(FrameworkModel frameworkModel) {
        ExtensionFactory extensionFactory = frameworkModel.getOrRegisterBean(ExtensionFactory.class);
        resolvers = extensionFactory.getExtensions(OpenAPISchemaResolver.class);
        predicates = extensionFactory.getExtensions(OpenAPISchemaPredicate.class);
    }

    public Map<Class<?>, Optional<Schema>> getSchemaMap() {
        return schemaMap;
    }

    public Map<Class<?>, String> getNameMap() {
        return nameMap;
    }

    public Schema getSchema(Type type) {
        return getSchema(new TypeParameterMeta(type));
    }

    public Schema getSchema(ParameterMeta parameter) {
        return new ChainImpl(resolvers, p -> resolveSchema(p.getActualGenericType(), p))
                .resolve(parameter, new Context() {
                    @Override
                    public void defineSchema(String name, Class<?> type, Schema schema) {
                        schemaMap.putIfAbsent(type, Optional.of(schema));
                        nameMap.put(type, name);
                    }

                    @Override
                    public void defineSchema(Class<?> type, Schema schema) {
                        schemaMap.putIfAbsent(type, Optional.of(schema));
                    }

                    @Override
                    public Schema getSchema(ParameterMeta parameter) {
                        return SchemaFactory.this.getSchema(parameter);
                    }

                    @Override
                    public Schema getSchema(Type type) {
                        return SchemaFactory.this.getSchema(type);
                    }
                });
    }

    public Schema getSchema(ParameterMeta[] parameters) {
        Schema schema = OBJECT.newSchema();
        for (ParameterMeta parameter : parameters) {
            String name = parameter.getName();
            if (name == null) {
                return ARRAY.newSchema();
            }
            schema.addProperty(name, getSchema(parameter));
        }
        return schema;
    }

    private Schema resolveSchema(Type type, ParameterMeta parameter) {
        if (type instanceof Class) {
            return resolveClassSchema((Class<?>) type, parameter);
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) type;
            Type rawType = pType.getRawType();
            if (rawType instanceof Class) {
                Class<?> clazz = (Class<?>) rawType;
                Type[] argTypes = pType.getActualTypeArguments();
                if (Iterable.class.isAssignableFrom(clazz)) {
                    Type itemType = TypeUtils.getActualGenericType(argTypes[0]);
                    return ARRAY.newSchema().setItems(resolveNestedSchema(itemType, parameter));
                }

                if (Map.class.isAssignableFrom(clazz)) {
                    Schema nestedSchema = resolveNestedSchema(argTypes[1], parameter);
                    return OBJECT.newSchema().setAdditionalPropertiesSchema(nestedSchema);
                }

                return resolveClassSchema(clazz, parameter);
            }
        }
        if (type instanceof TypeVariable) {
            return resolveNestedSchema(((TypeVariable<?>) type).getBounds()[0], parameter);
        }
        if (type instanceof WildcardType) {
            return resolveNestedSchema(((WildcardType) type).getUpperBounds()[0], parameter);
        }
        if (type instanceof GenericArrayType) {
            Type nestedType = ((GenericArrayType) type).getGenericComponentType();
            return ARRAY.newSchema().setItems(resolveNestedSchema(nestedType, parameter));
        }
        return OBJECT.newSchema();
    }

    private Schema resolveClassSchema(Class<?> clazz, ParameterMeta parameter) {
        Schema schema = PrimitiveSchema.newSchemaOf(clazz);
        if (schema != null) {
            return schema;
        }

        if (clazz.isArray()) {
            return ARRAY.newSchema().setItems(resolveNestedSchema(clazz.getComponentType(), parameter));
        }

        Optional<Schema> existingSchema = schemaMap.get(clazz);
        if (existingSchema != null) {
            return existingSchema.map(s -> new Schema().setTargetSchema(s)).orElseGet(OBJECT::newSchema);
        }

        if (TypeUtils.isSystemType(clazz)) {
            schemaMap.put(clazz, Optional.empty());
            return OBJECT.newSchema();
        }

        for (OpenAPISchemaPredicate predicate : predicates) {
            if (predicate.testClass(clazz, parameter)) {
                continue;
            }
            schemaMap.put(clazz, Optional.empty());
            return OBJECT.newSchema();
        }

        if (clazz.isEnum()) {
            schema = PrimitiveSchema.STRING.newSchema().setJavaType(clazz);
            for (Object value : clazz.getEnumConstants()) {
                schema.addEnumeration(value);
            }
            schemaMap.put(clazz, Optional.of(schema));
            return schema.clone();
        }

        Schema beanSchema = OBJECT.newSchema().setJavaType(clazz);
        schemaMap.put(clazz, Optional.of(beanSchema));
        BeanMeta beanMeta = new BeanMeta(parameter.getToolKit(), clazz, true);
        out:
        for (PropertyMeta property : beanMeta.getProperties()) {
            for (OpenAPISchemaPredicate predicate : predicates) {
                if (predicate.testProperty(parameter, beanMeta, property)) {
                    continue;
                }
                continue out;
            }

            int visibility = property.getVisibility();
            if (visibility > 1 && (visibility & 1) == 1) {
                beanSchema.addProperty(property.getName(), getSchema(property));
            }
        }
        return new Schema().setTargetSchema(beanSchema);
    }

    private Schema resolveNestedSchema(Type nestedType, ParameterMeta parameter) {
        return resolveSchema(nestedType, new TypeParameterMeta(parameter.getToolKit(), nestedType));
    }

    static final class ChainImpl implements Chain {

        private final OpenAPISchemaResolver[] resolvers;
        private final Function<ParameterMeta, Schema> fallback;

        private int cursor;

        ChainImpl(OpenAPISchemaResolver[] resolvers, Function<ParameterMeta, Schema> fallback) {
            this.resolvers = resolvers;
            this.fallback = fallback;
        }

        @Override
        public Schema resolve(ParameterMeta parameter, Context context) {
            if (cursor < resolvers.length) {
                return resolvers[cursor++].resolve(parameter, context, this);
            }
            return fallback.apply(parameter);
        }
    }
}
