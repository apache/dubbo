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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class BeanMeta {

    private final Class<?> type;
    private final ConstructorMeta constructor;
    private final Map<String, PropertyMeta> properties = new LinkedHashMap<>();

    public BeanMeta(RestToolKit toolKit, String prefix, Class<?> type, boolean skipConstructor) {
        this.type = type;
        constructor = skipConstructor ? null : resolveConstructor(toolKit, null, type);
        resolveProperties(toolKit, prefix, type);
    }

    public BeanMeta(RestToolKit toolKit, Class<?> type, boolean skipConstructor) {
        this(toolKit, null, type, skipConstructor);
    }

    public BeanMeta(RestToolKit toolKit, String prefix, Class<?> type) {
        this(toolKit, prefix, type, false);
    }

    public BeanMeta(RestToolKit toolKit, Class<?> type) {
        this(toolKit, null, type, false);
    }

    public Class<?> getType() {
        return type;
    }

    public ConstructorMeta getConstructor() {
        return constructor;
    }

    public Collection<PropertyMeta> getProperties() {
        return properties.values();
    }

    public PropertyMeta getProperty(String name) {
        return properties.get(name);
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

    private void resolveProperties(RestToolKit toolKit, String prefix, Class<?> type) {
        if (type == null || type == Object.class) {
            return;
        }

        Set<String> allNames = new LinkedHashSet<>();
        Map<String, Field> fieldMap = new LinkedHashMap<>();
        for (Field field : type.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) {
                continue;
            }
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            fieldMap.put(field.getName(), field);
            allNames.add(field.getName());
        }
        Map<String, Method> getMethodMap = new LinkedHashMap<>();
        Map<String, Method> setMethodMap = new LinkedHashMap<>();
        for (Method method : type.getDeclaredMethods()) {
            int modifiers = method.getModifiers();
            if ((modifiers & (Modifier.PUBLIC | Modifier.ABSTRACT | Modifier.STATIC)) == Modifier.PUBLIC) {
                String name = method.getName();
                int count = method.getParameterCount();
                if (count == 0) {
                    Class<?> returnType = method.getReturnType();
                    if (returnType == Void.TYPE) {
                        continue;
                    }
                    if (name.startsWith("get")) {
                        name = toName(name, 3);
                        getMethodMap.put(name, method);
                        allNames.add(name);
                    } else if (name.startsWith("is") && returnType == Boolean.TYPE) {
                        name = toName(name, 2);
                        getMethodMap.put(name, method);
                        allNames.add(name);
                    }
                } else if (count == 1) {
                    if (name.startsWith("set")) {
                        name = toName(name, 3);
                        setMethodMap.put(name, method);
                        allNames.add(name);
                    }
                }
            }
        }
        for (String name : allNames) {
            Field field = fieldMap.get(name);
            Method getMethod = getMethodMap.get(name);
            Method setMethod = setMethodMap.get(name);
            PropertyMeta meta = new PropertyMeta(toolKit, field, getMethod, setMethod, prefix, name);
            properties.put(meta.getName(), meta);
        }

        resolveProperties(toolKit, prefix, type.getSuperclass());
    }

    private static String toName(String name, int index) {
        return Character.toLowerCase(name.charAt(index)) + name.substring(index + 1);
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

        public Object getValue(Object bean) {
            return null;
        }

        public void setValue(Object bean, Object value) {}

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

    public static final class PropertyMeta extends NestableParameterMeta {

        private final Field field;
        private final Method getMethod;
        private final Method setMethod;
        private final Parameter parameter;

        PropertyMeta(RestToolKit toolKit, Field f, Method gm, Method sm, String prefix, String name) {
            super(toolKit, prefix, name);
            field = f;
            getMethod = gm;
            setMethod = sm;
            parameter = setMethod == null ? null : setMethod.getParameters()[0];
            initNestedMeta();
        }

        @Override
        public Class<?> getType() {
            if (field != null) {
                return field.getType();
            }
            if (parameter != null) {
                return parameter.getType();
            }
            return getMethod.getReturnType();
        }

        @Override
        public Type getGenericType() {
            if (field != null) {
                return field.getGenericType();
            }
            if (parameter != null) {
                return parameter.getParameterizedType();
            }
            return getMethod.getGenericReturnType();
        }

        @Override
        protected AnnotatedElement getAnnotatedElement() {
            if (field != null) {
                return field;
            }
            if (parameter != null) {
                return parameter;
            }
            return getMethod;
        }

        public Object getValue(Object bean) {
            if (getMethod != null) {
                try {
                    return getMethod.invoke(bean);
                } catch (Throwable t) {
                    throw ExceptionUtils.wrap(t);
                }
            } else if (field != null) {
                try {
                    return field.get(bean);
                } catch (Throwable t) {
                    throw ExceptionUtils.wrap(t);
                }
            }
            return null;
        }

        public void setValue(Object bean, Object value) {
            if (setMethod != null) {
                try {
                    setMethod.invoke(bean, value);
                } catch (Throwable t) {
                    throw ExceptionUtils.wrap(t);
                }
            } else if (field != null) {
                try {
                    field.set(bean, value);
                } catch (Throwable t) {
                    throw ExceptionUtils.wrap(t);
                }
            }
        }

        @Override
        public String getDescription() {
            return "PropertyMeta{" + (field == null ? (parameter == null ? getMethod : parameter) : field) + '}';
        }

        public boolean canSetValue() {
            return setMethod != null || field != null;
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
            return type;
        }

        @Override
        public String getDescription() {
            return "NestedParameter{" + (genericType == null ? type : genericType) + '}';
        }
    }
}
