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

import java.lang.reflect.AnnotatedElement;
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

                Schema classSchema = doResolveClass(clazz, parameter, pType);
                for (Type argType : argTypes) {
                    Type actualArgType = TypeUtils.getActualGenericType(argType);
                    if (actualArgType != null) {
                        doResolveNestedType(actualArgType, parameter);
                    }
                }
                return classSchema;
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
        return doResolveClass(clazz, parameter, null);
    }

    private Schema doResolveClass(Class<?> clazz, ParameterMeta parameter, ParameterizedType parameterizedType) {
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

        TypeParameterMeta typeParameter = new TypeParameterMeta(parameter.getToolKit(), clazz);
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

        return new Schema()
                .setTargetSchema(doResolveBeanClass(parameter.getToolKit(), clazz, flatten, parameterizedType));
    }

    private Schema doResolveBeanClass(RestToolKit toolKit, Class<?> clazz, boolean flatten) {
        return doResolveBeanClass(toolKit, clazz, flatten, null);
    }

    private Schema doResolveBeanClass(
            RestToolKit toolKit, Class<?> clazz, boolean flatten, ParameterizedType parameterizedType) {
        Schema beanSchema = OBJECT.newSchema().setJavaType(clazz);
        schemaMap.put(clazz, beanSchema);
        BeanMeta beanMeta = new BeanMeta(toolKit, clazz, flatten);

        Map<String, Type> typeVarMap = null;
        if (parameterizedType != null) {
            TypeVariable<?>[] typeParams = clazz.getTypeParameters();
            Type[] actualArgs = parameterizedType.getActualTypeArguments();
            if (typeParams.length == actualArgs.length && typeParams.length > 0) {
                typeVarMap = CollectionUtils.newHashMap(typeParams.length);
                for (int i = 0; i < typeParams.length; i++) {
                    typeVarMap.put(typeParams[i].getName(), actualArgs[i]);
                }
            }
        }

        out:
        for (PropertyMeta property : beanMeta.getProperties()) {
            boolean fallback = true;
            for (OpenAPISchemaPredicate predicate : predicates) {
                Boolean accepted = predicate.acceptProperty(beanMeta, property);
                if (accepted == null) {
                    continue;
                }
                if (accepted) {
                    continue out;
                } else {
                    fallback = false;
                    break;
                }
            }

            if (fallback) {
                int visibility = property.getVisibility();
                if ((visibility & 0b001) == 0 || (visibility & 0b110) == 0) {
                    continue;
                }
            }

            Type originalType = property.getGenericType();
            Type substitutedType = originalType;
            if (typeVarMap != null) {
                substitutedType = substituteTypeVariables(originalType, typeVarMap);
            }

            Schema propertySchema;
            if (substitutedType != originalType) {
                ParameterMeta substitutedParam = new SubstitutedPropertyMeta(property, substitutedType);
                propertySchema = resolve(substitutedParam);
            } else {
                propertySchema = resolve(property);
            }
            beanSchema.addProperty(property.getName(), propertySchema);
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

    private Type substituteTypeVariables(Type type, Map<String, Type> typeVarMap) {
        if (type instanceof TypeVariable) {
            TypeVariable<?> typeVar = (TypeVariable<?>) type;
            Type actualType = typeVarMap.get(typeVar.getName());
            return actualType != null ? actualType : type;
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) type;
            Type[] actualArgs = pType.getActualTypeArguments();
            Type[] substitutedArgs = new Type[actualArgs.length];
            boolean changed = false;
            for (int i = 0; i < actualArgs.length; i++) {
                substitutedArgs[i] = substituteTypeVariables(actualArgs[i], typeVarMap);
                if (substitutedArgs[i] != actualArgs[i]) {
                    changed = true;
                }
            }
            if (changed) {
                return new ParameterizedTypeImpl(pType.getRawType(), substitutedArgs, pType.getOwnerType());
            }
        }
        return type;
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

    private static final class SubstitutedPropertyMeta extends ParameterMeta {
        private final PropertyMeta originalProperty;
        private final Type substitutedType;
        private final Class<?> substitutedClass;

        SubstitutedPropertyMeta(PropertyMeta originalProperty, Type substitutedType) {
            super(originalProperty.getToolKit(), originalProperty.getPrefix(), originalProperty.getName());
            this.originalProperty = originalProperty;
            this.substitutedType = substitutedType;
            this.substitutedClass = TypeUtils.getActualType(substitutedType);
        }

        @Override
        public Class<?> getType() {
            return substitutedClass;
        }

        @Override
        public Type getGenericType() {
            return substitutedType;
        }

        @Override
        protected AnnotatedElement getAnnotatedElement() {
            // Return the first annotated element from the list
            List<? extends AnnotatedElement> elements = originalProperty.getAnnotatedElements();
            return (elements != null && !elements.isEmpty()) ? elements.get(0) : null;
        }

        @Override
        public List<? extends AnnotatedElement> getAnnotatedElements() {
            // Delegate to original property to preserve all annotation sources
            return originalProperty.getAnnotatedElements();
        }

        @Override
        public String getDescription() {
            return originalProperty.getDescription();
        }

        @Override
        public int getIndex() {
            return originalProperty.getIndex();
        }
    }

    private static final class ParameterizedTypeImpl implements ParameterizedType {
        private final Type rawType;
        private final Type[] typeArguments;
        private final Type ownerType;

        ParameterizedTypeImpl(Type rawType, Type[] typeArguments, Type ownerType) {
            this.rawType = rawType;
            this.typeArguments = typeArguments;
            this.ownerType = ownerType;
        }

        @Override
        public Type[] getActualTypeArguments() {
            return typeArguments.clone();
        }

        @Override
        public Type getRawType() {
            return rawType;
        }

        @Override
        public Type getOwnerType() {
            return ownerType;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (ownerType != null) {
                sb.append(ownerType).append(".");
            }
            sb.append(rawType);
            if (typeArguments.length > 0) {
                sb.append("<");
                for (int i = 0; i < typeArguments.length; i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(typeArguments[i]);
                }
                sb.append(">");
            }
            return sb.toString();
        }
    }
}
