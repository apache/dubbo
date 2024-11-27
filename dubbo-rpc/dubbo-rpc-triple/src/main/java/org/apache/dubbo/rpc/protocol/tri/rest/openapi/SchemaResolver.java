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
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.RadixTree;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.RadixTree.Match;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.AnnotationMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.BeanMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.BeanMeta.PropertyMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ParameterMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.TypeParameterMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.OpenAPISchemaResolver.SchemaChain;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.OpenAPISchemaResolver.SchemaContext;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Schema;
import org.apache.dubbo.rpc.protocol.tri.rest.support.basic.Annotations;
import org.apache.dubbo.rpc.protocol.tri.rest.util.RestToolKit;
import org.apache.dubbo.rpc.protocol.tri.rest.util.TypeUtils;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.apache.dubbo.rpc.protocol.tri.rest.openapi.PrimitiveSchema.ARRAY;
import static org.apache.dubbo.rpc.protocol.tri.rest.openapi.PrimitiveSchema.OBJECT;

public final class SchemaResolver {

    private final ConfigFactory configFactory;
    private final OpenAPISchemaResolver[] resolvers;
    private final OpenAPISchemaPredicate[] predicates;
    private final Map<Class<?>, Schema> schemaMap = CollectionUtils.newConcurrentHashMap();

    private volatile RadixTree<Boolean> classFilter;

    public SchemaResolver(FrameworkModel frameworkModel) {
        configFactory = frameworkModel.getOrRegisterBean(ConfigFactory.class);
        ExtensionFactory extensionFactory = frameworkModel.getOrRegisterBean(ExtensionFactory.class);
        resolvers = extensionFactory.getExtensions(OpenAPISchemaResolver.class);
        predicates = extensionFactory.getExtensions(OpenAPISchemaPredicate.class);
    }

    public Schema resolve(Type type) {
        return resolve(new TypeParameterMeta(type));
    }

    public Schema resolve(ParameterMeta parameter) {
        return new SchemaChainImpl(resolvers, this::fallbackResolve).resolve(parameter, new SchemaContextImpl());
    }

    public Schema resolve(List<ParameterMeta> parameters) {
        Schema schema = OBJECT.newSchema();
        for (ParameterMeta parameter : parameters) {
            String name = parameter.getName();
            if (name == null) {
                return ARRAY.newSchema();
            }
            schema.addProperty(name, resolve(parameter));
        }
        return schema;
    }

    private Schema fallbackResolve(ParameterMeta parameter) {
        return doResolveType(parameter.getActualGenericType(), parameter);
    }

    private Schema doResolveNestedType(Type nestedType, ParameterMeta parameter) {
        return doResolveType(nestedType, new TypeParameterMeta(parameter.getToolKit(), nestedType));
    }

    private Schema doResolveType(Type type, ParameterMeta parameter) {
        if (type instanceof Class) {
            return doResolveClass((Class<?>) type, parameter);
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) type;
            Type rawType = pType.getRawType();
            if (rawType instanceof Class) {
                Class<?> clazz = (Class<?>) rawType;
                Type[] argTypes = pType.getActualTypeArguments();
                if (Iterable.class.isAssignableFrom(clazz)) {
                    Type itemType = TypeUtils.getActualGenericType(argTypes[0]);
                    return ARRAY.newSchema()
                            .addExtension(Constants.X_JAVA_CLASS, TypeUtils.toTypeString(type))
                            .setItems(doResolveNestedType(itemType, parameter));
                }

                if (Map.class.isAssignableFrom(clazz)) {
                    return OBJECT.newSchema()
                            .addExtension(Constants.X_JAVA_CLASS, TypeUtils.toTypeString(type))
                            .setAdditionalPropertiesSchema(doResolveNestedType(argTypes[1], parameter));
                }

                return doResolveClass(clazz, parameter);
            }
        }
        if (type instanceof TypeVariable) {
            return doResolveNestedType(((TypeVariable<?>) type).getBounds()[0], parameter);
        }
        if (type instanceof WildcardType) {
            return doResolveNestedType(((WildcardType) type).getUpperBounds()[0], parameter);
        }
        if (type instanceof GenericArrayType) {
            return ARRAY.newSchema()
                    .addExtension(Constants.X_JAVA_CLASS, TypeUtils.toTypeString(type))
                    .setItems(doResolveNestedType(((GenericArrayType) type).getGenericComponentType(), parameter));
        }
        return OBJECT.newSchema();
    }

    private Schema doResolveClass(Class<?> clazz, ParameterMeta parameter) {
        Schema schema = PrimitiveSchema.newSchemaOf(clazz);
        if (schema != null) {
            return schema;
        }

        if (clazz.isArray()) {
            schema = ARRAY.newSchema();
            if (!PrimitiveSchema.isPrimitive(clazz.getComponentType())) {
                schema.addExtension(Constants.X_JAVA_CLASS, TypeUtils.toTypeString(clazz));
            }
            return schema.setItems(doResolveNestedType(clazz.getComponentType(), parameter));
        }

        Schema existingSchema = schemaMap.get(clazz);
        if (existingSchema != null) {
            return new Schema().setTargetSchema(existingSchema);
        }

        if (isClassExcluded(clazz)) {
            schema = OBJECT.newSchema().addExtension(Constants.X_JAVA_CLASS, TypeUtils.toTypeString(clazz));
            schemaMap.put(clazz, schema);
            return schema;
        }

        TypeParameterMeta typeParameter = new TypeParameterMeta(clazz);
        for (OpenAPISchemaPredicate predicate : predicates) {
            Boolean accepted = predicate.acceptClass(clazz, typeParameter);
            if (accepted == null) {
                continue;
            }
            if (accepted) {
                break;
            } else {
                schema = OBJECT.newSchema().addExtension(Constants.X_JAVA_CLASS, TypeUtils.toTypeString(clazz));
                schemaMap.put(clazz, schema);
                return schema;
            }
        }

        if (clazz.isEnum()) {
            schema = PrimitiveSchema.STRING.newSchema().setJavaType(clazz);
            for (Object value : clazz.getEnumConstants()) {
                schema.addEnumeration(value);
            }
            schemaMap.put(clazz, schema);
            return schema.clone();
        }

        Boolean flatten = configFactory.getGlobalConfig().getSchemaFlatten();
        if (flatten == null) {
            AnnotationMeta<?> anno = typeParameter.getAnnotation(Annotations.Schema);
            flatten = anno != null && anno.getBoolean("flatten");
        }

        return new Schema().setTargetSchema(doResolveBeanClass(parameter.getToolKit(), clazz, flatten));
    }

    private Schema doResolveBeanClass(RestToolKit toolKit, Class<?> clazz, boolean flatten) {
        Schema beanSchema = OBJECT.newSchema().setJavaType(clazz);
        schemaMap.put(clazz, beanSchema);
        BeanMeta beanMeta = new BeanMeta(toolKit, clazz, flatten);
        out:
        for (PropertyMeta property : beanMeta.getProperties()) {
            boolean fallback = true;
            for (OpenAPISchemaPredicate predicate : predicates) {
                Boolean accepted = predicate.acceptProperty(beanMeta, property);
                if (accepted == null) {
                    continue;
                }
                if (accepted) {
                    fallback = false;
                    break;
                } else {
                    continue out;
                }
            }

            if (fallback) {
                int visibility = property.getVisibility();
                if ((visibility & 0b001) == 0 || (visibility & 0b110) == 0) {
                    continue;
                }
            }
            beanSchema.addProperty(property.getName(), resolve(property));
        }

        if (flatten) {
            return beanSchema;
        }
        Class<?> superClass = clazz.getSuperclass();
        if (superClass == null || superClass == Object.class || TypeUtils.isSystemType(superClass)) {
            return beanSchema;
        }

        return beanSchema.addAllOf(resolve(superClass));
    }

    private boolean isClassExcluded(Class<?> clazz) {
        RadixTree<Boolean> classFilter = this.classFilter;
        if (classFilter == null) {
            synchronized (this) {
                classFilter = this.classFilter;
                if (classFilter == null) {
                    classFilter = new RadixTree<>('.');
                    for (String prefix : TypeUtils.getSystemPrefixes()) {
                        addPath(classFilter, prefix);
                    }
                    String[] excludes = configFactory.getGlobalConfig().getSchemaClassExcludes();
                    if (excludes != null) {
                        for (String exclude : excludes) {
                            addPath(classFilter, exclude);
                        }
                    }
                    this.classFilter = classFilter;
                }
            }
        }

        List<Match<Boolean>> matches = classFilter.match('.' + clazz.getName());
        int size = matches.size();
        if (size == 0) {
            return false;
        } else if (size > 1) {
            Collections.sort(matches);
        }
        return matches.get(0).getValue();
    }

    public static void addPath(RadixTree<Boolean> tree, String path) {
        if (path == null) {
            return;
        }
        int size = path.length();
        if (size == 0) {
            return;
        }
        boolean value = true;
        if (path.charAt(0) == '!') {
            path = path.substring(1);
            size--;
            value = false;
        }
        if (path.charAt(size - 1) == '.') {
            path += "**";
        }
        tree.addPath(path, value);
    }

    private static final class SchemaChainImpl implements SchemaChain {

        private final OpenAPISchemaResolver[] resolvers;
        private final Function<ParameterMeta, Schema> fallback;
        private int cursor;

        SchemaChainImpl(OpenAPISchemaResolver[] resolvers, Function<ParameterMeta, Schema> fallback) {
            this.resolvers = resolvers;
            this.fallback = fallback;
        }

        @Override
        public Schema resolve(ParameterMeta parameter, SchemaContext context) {
            if (cursor < resolvers.length) {
                return resolvers[cursor++].resolve(parameter, context, this);
            }
            return fallback.apply(parameter);
        }
    }

    private final class SchemaContextImpl implements SchemaContext {

        @Override
        public void defineSchema(Class<?> type, Schema schema) {
            schemaMap.putIfAbsent(type, schema);
        }

        @Override
        public Schema resolve(ParameterMeta parameter) {
            return SchemaResolver.this.resolve(parameter);
        }

        @Override
        public Schema resolve(Type type) {
            return SchemaResolver.this.resolve(type);
        }
    }
}
