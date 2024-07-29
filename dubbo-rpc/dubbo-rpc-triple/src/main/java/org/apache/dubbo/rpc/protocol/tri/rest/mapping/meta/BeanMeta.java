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
package org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta;

import org.apache.dubbo.remoting.http12.rest.Param;
import org.apache.dubbo.rpc.protocol.tri.ExceptionUtils;
import org.apache.dubbo.rpc.protocol.tri.rest.util.RestToolKit;
import org.apache.dubbo.rpc.protocol.tri.rest.util.TypeUtils;

import javax.annotation.Nullable;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BeanMeta {

    private final Map<String, FieldMeta> fields = new LinkedHashMap<>();
    private final Map<String, SetMethodMeta> methods = new LinkedHashMap<>();
    private final ConstructorMeta constructor;

    public BeanMeta(RestToolKit toolKit, String prefix, Class<?> type) {
        constructor = resolveConstructor(toolKit, prefix, type);
        resolveFieldAndMethod(toolKit, prefix, type);
    }

    public BeanMeta(RestToolKit toolKit, Class<?> type) {
        this(toolKit, null, type);
    }

    public Collection<FieldMeta> getFields() {
        return fields.values();
    }

    public FieldMeta getField(String name) {
        return fields.get(name);
    }

    public Collection<SetMethodMeta> getMethods() {
        return methods.values();
    }

    public SetMethodMeta getMethod(String name) {
        return methods.get(name);
    }

    public ConstructorMeta getConstructor() {
        return constructor;
    }

    public Object newInstance() {
        return constructor.newInstance();
    }

    public static ConstructorMeta resolveConstructor(RestToolKit toolKit, String prefix, Class<?> type) {
        Constructor<?>[] constructors = type.getConstructors();
        Constructor<?> ct = null;
        if (constructors.length == 1) {
            ct = constructors[0];
        } else {
            try {
                ct = type.getDeclaredConstructor();
            } catch (NoSuchMethodException ignored) {
            }
        }
        if (ct == null) {
            throw new IllegalArgumentException("No available default constructor found in " + type);
        }
        return new ConstructorMeta(toolKit, prefix, ct);
    }

    private void resolveFieldAndMethod(RestToolKit toolKit, String prefix, Class<?> type) {
        if (type == Object.class) {
            return;
        }
        for (Field field : type.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers)) {
                continue;
            }
            if (field.getAnnotations().length == 0) {
                continue;
            }
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            FieldMeta fieldMeta = new FieldMeta(toolKit, prefix, field);
            fields.put(fieldMeta.getName(), fieldMeta);
        }
        for (Method method : type.getDeclaredMethods()) {
            if (method.getParameterCount() != 1) {
                continue;
            }
            int modifiers = method.getModifiers();
            if ((modifiers & (Modifier.PUBLIC | Modifier.ABSTRACT | Modifier.STATIC)) == Modifier.PUBLIC) {
                Parameter parameter = method.getParameters()[0];
                String name = method.getName();
                if (name.length() > 3 && name.startsWith("set")) {
                    String getMethodName = "get" + name.substring(3);
                    Method getMethod = null;
                    try {
                        getMethod = type.getDeclaredMethod(getMethodName);
                    } catch (NoSuchMethodException ignored) {
                    }
                    name = Character.toLowerCase(name.charAt(3)) + name.substring(4);
                    SetMethodMeta methodMeta = new SetMethodMeta(toolKit, method, getMethod, parameter, prefix, name);
                    methods.put(methodMeta.getName(), methodMeta);
                }
            }
        }
        resolveFieldAndMethod(toolKit, prefix, type.getSuperclass());
    }

    public static final class ConstructorMeta {

        private final Constructor<?> constructor;
        private final ConstructorParameterMeta[] parameters;

        ConstructorMeta(RestToolKit toolKit, String prefix, Constructor<?> constructor) {
            this.constructor = constructor;
            parameters = initParameters(toolKit, prefix, constructor);
        }

        public ConstructorParameterMeta[] getParameters() {
            return parameters;
        }

        private ConstructorParameterMeta[] initParameters(RestToolKit toolKit, String prefix, Constructor<?> ct) {
            Parameter[] cps = ct.getParameters();
            int len = cps.length;
            ConstructorParameterMeta[] parameters = new ConstructorParameterMeta[len];
            for (int i = 0; i < len; i++) {
                parameters[i] = new ConstructorParameterMeta(toolKit, cps[i], prefix);
            }
            return parameters;
        }

        public Object newInstance(Object... args) {
            try {
                return constructor.newInstance(args);
            } catch (Throwable t) {
                throw ExceptionUtils.wrap(t);
            }
        }
    }

    public static final class ConstructorParameterMeta extends ParameterMeta {

        private final Parameter parameter;

        ConstructorParameterMeta(RestToolKit toolKit, Parameter parameter, String prefix) {
            super(toolKit, prefix, parameter.isNamePresent() ? parameter.getName() : null);
            this.parameter = parameter;
        }

        @Override
        protected AnnotatedElement getAnnotatedElement() {
            return parameter;
        }

        @Override
        public Class<?> getType() {
            return parameter.getType();
        }

        @Override
        public Type getGenericType() {
            return parameter.getParameterizedType();
        }

        @Override
        public String getDescription() {
            return "ConstructorParameter{" + parameter + '}';
        }
    }

    public abstract static class NestableParameterMeta extends ParameterMeta {

        private NestableParameterMeta nestedMeta;
        private String finalName;

        public NestableParameterMeta(RestToolKit toolKit, String prefix, String name) {
            super(toolKit, prefix, name);
        }

        @Nullable
        @Override
        public final String getName() {
            String name = finalName;
            if (name == null) {
                AnnotationMeta<Param> param = findAnnotation(Param.class);
                if (param != null) {
                    name = param.getValue();
                }
                if (name == null || name.isEmpty()) {
                    name = super.getName();
                }
                finalName = name;
            }
            return name;
        }

        public void setValue(Object bean, Object value) {}

        public Object getValue(Object bean) {
            return null;
        }

        public final NestableParameterMeta getNestedMeta() {
            return nestedMeta;
        }

        protected final void initNestedMeta() {
            Type nestedType = null;
            Class<?> type = getType();
            if (Map.class.isAssignableFrom(type)) {
                nestedType = TypeUtils.getNestedGenericType(getGenericType(), 1);
            } else if (Collection.class.isAssignableFrom(type)) {
                nestedType = TypeUtils.getNestedGenericType(getGenericType(), 0);
            } else if (type.isArray()) {
                Type genericType = getGenericType();
                if (genericType instanceof GenericArrayType) {
                    nestedType = ((GenericArrayType) genericType).getGenericComponentType();
                } else {
                    nestedType = type.getComponentType();
                }
            }
            nestedMeta = nestedType == null ? null : new NestedMeta(getToolKit(), nestedType);
        }
    }

    public static final class FieldMeta extends NestableParameterMeta {

        private final Field field;

        FieldMeta(RestToolKit toolKit, String prefix, Field field) {
            super(toolKit, prefix, field.getName());
            this.field = field;
            initNestedMeta();
        }

        @Override
        public Class<?> getType() {
            return field.getType();
        }

        @Override
        public Type getGenericType() {
            return field.getGenericType();
        }

        @Override
        protected AnnotatedElement getAnnotatedElement() {
            return field;
        }

        public void setValue(Object bean, Object value) {
            try {
                field.set(bean, value);
            } catch (Throwable t) {
                throw ExceptionUtils.wrap(t);
            }
        }

        public Object getValue(Object bean) {
            try {
                return field.get(bean);
            } catch (Throwable t) {
                throw ExceptionUtils.wrap(t);
            }
        }

        @Override
        public String getDescription() {
            return "FieldParameter{" + field + '}';
        }
    }

    public static final class SetMethodMeta extends NestableParameterMeta {

        private final Method method;
        private final Method getMethod;
        private final Parameter parameter;

        SetMethodMeta(RestToolKit toolKit, Method m, Method gm, Parameter p, String prefix, String name) {
            super(toolKit, prefix, name);
            method = m;
            getMethod = gm;
            parameter = p;
            initNestedMeta();
        }

        @Override
        public Class<?> getType() {
            return parameter.getType();
        }

        @Override
        public Type getGenericType() {
            return parameter.getParameterizedType();
        }

        @Override
        protected AnnotatedElement getAnnotatedElement() {
            return parameter;
        }

        public void setValue(Object bean, Object value) {
            try {
                method.invoke(bean, value);
            } catch (Throwable t) {
                throw ExceptionUtils.wrap(t);
            }
        }

        public Object getValue(Object bean) {
            if (getMethod == null) {
                return null;
            }
            try {
                return getMethod.invoke(bean);
            } catch (Throwable t) {
                throw ExceptionUtils.wrap(t);
            }
        }

        @Override
        public String getDescription() {
            return "SetMethodParameter{" + method + '}';
        }
    }

    private static final class NestedMeta extends NestableParameterMeta {

        private final Class<?> type;
        private final Type genericType;

        NestedMeta(RestToolKit toolKit, Type genericType) {
            super(toolKit, null, null);
            type = TypeUtils.getActualType(genericType);
            this.genericType = genericType;
            initNestedMeta();
        }

        @Override
        public Class<?> getType() {
            return type;
        }

        @Override
        public Type getGenericType() {
            return genericType;
        }

        @Override
        protected AnnotatedElement getAnnotatedElement() {
            return null;
        }

        @Override
        public String getDescription() {
            return "NestedParameter{" + (genericType == null ? type : genericType) + '}';
        }
    }
}
