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

import org.apache.dubbo.common.utils.ClassUtils;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;

public final class BeanMeta extends ParameterMeta {

    private static final boolean HAS_PB = ClassUtils.hasProtobuf();

    private final Class<?> type;
    private final boolean flatten;
    private ConstructorMeta constructor;
    private Map<String, PropertyMeta> propertyMap;

    public BeanMeta(RestToolKit toolKit, String prefix, Class<?> type, boolean flatten) {
        super(toolKit, prefix, null);
        this.type = type;
        this.flatten = flatten;
    }

    public BeanMeta(RestToolKit toolKit, Class<?> type, boolean flatten) {
        this(toolKit, null, type, flatten);
    }

    public BeanMeta(RestToolKit toolKit, String prefix, Class<?> type) {
        this(toolKit, prefix, type, true);
    }

    public BeanMeta(RestToolKit toolKit, Class<?> type) {
        this(toolKit, null, type, true);
    }

    @Override
    public Class<?> getType() {
        return type;
    }

    @Override
    public Type getGenericType() {
        return type;
    }

    @Override
    protected AnnotatedElement getAnnotatedElement() {
        return type;
    }

    public ConstructorMeta getConstructor() {
        if (constructor == null) {
            constructor = resolveConstructor(getToolKit(), getPrefix(), type);
        }
        return constructor;
    }

    public Collection<PropertyMeta> getProperties() {
        return getPropertiesMap().values();
    }

    public PropertyMeta getProperty(String name) {
        return getPropertiesMap().get(name);
    }

    private Map<String, PropertyMeta> getPropertiesMap() {
        Map<String, PropertyMeta> propertyMap = this.propertyMap;
        if (propertyMap == null) {
            propertyMap = new LinkedHashMap<>();
            resolvePropertyMap(getToolKit(), getPrefix(), type, flatten, propertyMap);
            this.propertyMap = propertyMap;
        }
        return propertyMap;
    }

    public Object newInstance() {
        return getConstructor().newInstance();
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

    public static void resolvePropertyMap(
            RestToolKit toolKit, String prefix, Class<?> type, boolean flatten, Map<String, PropertyMeta> propertyMap) {
        if (type == null || type == Object.class || TypeUtils.isSystemType(type)) {
            return;
        }

        Set<String> pbFields = null;
        if (HAS_PB && Message.class.isAssignableFrom(type)) {
            try {
                Descriptor descriptor =
                        (Descriptor) type.getMethod("getDescriptor").invoke(null);
                pbFields = descriptor.getFields().stream()
                        .map(FieldDescriptor::getName)
                        .collect(Collectors.toSet());
            } catch (Exception ignored) {
            }
        }

        Set<String> allNames = new LinkedHashSet<>();
        Map<String, Field> fieldMap = new LinkedHashMap<>();
        if (pbFields == null) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())
                        || Modifier.isTransient(field.getModifiers())
                        || field.isSynthetic()) {
                    continue;
                }
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                fieldMap.put(field.getName(), field);
                allNames.add(field.getName());
            }
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
                    if (name.length() > 3 && name.startsWith("get")) {
                        name = toName(name, 3);
                        if (pbFields == null || pbFields.contains(name)) {
                            getMethodMap.put(name, method);
                            allNames.add(name);
                        }
                    } else if (name.length() > 2 && name.startsWith("is") && returnType == Boolean.TYPE) {
                        if (pbFields == null || pbFields.contains(name)) {
                            name = toName(name, 2);
                            getMethodMap.put(name, method);
                            allNames.add(name);
                        }
                    } else if (fieldMap.containsKey(name)) {
                        // For record class
                        getMethodMap.put(name, method);
                        allNames.add(name);
                    }
                } else if (count == 1) {
                    if (name.length() > 3 && name.startsWith("set")) {
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
            int visibility = pbFields == null
                    ? (setMethod == null ? 0 : 1) << 2 | (getMethod == null ? 0 : 1) << 1 | (field == null ? 0 : 1)
                    : 0b011;
            PropertyMeta meta = new PropertyMeta(toolKit, field, getMethod, setMethod, prefix, name, visibility);
            propertyMap.put(meta.getName(), meta);
        }

        if (flatten) {
            resolvePropertyMap(toolKit, prefix, type.getSuperclass(), true, propertyMap);
        }
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
            if (len == 0) {
                return new ConstructorParameterMeta[0];
            }
            String[] parameterNames = toolKit == null ? null : toolKit.getParameterNames(ct);
            ConstructorParameterMeta[] parameters = new ConstructorParameterMeta[len];
            for (int i = 0; i < len; i++) {
                String parameterName = parameterNames == null ? null : parameterNames[i];
                parameters[i] = new ConstructorParameterMeta(toolKit, cps[i], prefix, parameterName);
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

        ConstructorParameterMeta(RestToolKit toolKit, Parameter parameter, String prefix, String name) {
            super(toolKit, prefix, name == null && parameter.isNamePresent() ? parameter.getName() : name);
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
        private final int visibility;

        PropertyMeta(RestToolKit toolKit, Field f, Method gm, Method sm, String prefix, String name, int visibility) {
            super(toolKit, prefix, name);
            this.visibility = visibility;
            field = f;
            getMethod = gm;
            setMethod = sm;
            parameter = setMethod == null ? null : setMethod.getParameters()[0];
            initNestedMeta();
        }

        public int getVisibility() {
            return visibility;
        }

        public Field getField() {
            return field;
        }

        public Method getGetMethod() {
            return getMethod;
        }

        public Method getSetMethod() {
            return setMethod;
        }

        public Parameter getParameter() {
            return parameter;
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

        @Override
        public List<? extends AnnotatedElement> getAnnotatedElements() {
            List<AnnotatedElement> elements = new ArrayList<>(3);
            if (field != null) {
                elements.add(field);
            }
            if (parameter != null) {
                elements.add(parameter);
            }
            if (getMethod != null) {
                elements.add(getMethod);
            }
            return elements;
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
