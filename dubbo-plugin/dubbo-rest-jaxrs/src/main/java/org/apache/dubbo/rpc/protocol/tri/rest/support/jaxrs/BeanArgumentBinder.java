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
package org.apache.dubbo.rpc.protocol.tri.rest.support.jaxrs;

import org.apache.dubbo.common.beans.factory.ScopeBeanFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.Pair;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.rest.argument.ArgumentConverter;
import org.apache.dubbo.rpc.protocol.tri.rest.argument.ArgumentResolver;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.AnnotationMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ParameterMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.util.RestToolKit;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class BeanArgumentBinder {

    private final ArgumentResolver argumentResolver;
    private final ArgumentConverter<?> argumentConverter;

    private final Map<Pair<Class<?>, String>, BeanMeta> cache = CollectionUtils.newConcurrentHashMap();

    BeanArgumentBinder(FrameworkModel frameworkModel) {
        ScopeBeanFactory beanFactory = frameworkModel.getBeanFactory();
        argumentResolver = beanFactory.getBean(ArgumentResolver.class);
        argumentConverter = beanFactory.getBean(ArgumentConverter.class);
    }

    public Object bind(ParameterMeta parameter, HttpRequest request, HttpResponse response) {
        try {
            return resolveArgument(parameter, request, response);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Object resolveArgument(ParameterMeta meta, HttpRequest request, HttpResponse response) throws Exception {
        AnnotationMeta form = meta.getAnnotation(Annotations.Form);
        if (form != null || meta.isAnnotated(Annotations.BeanParam)) {
            String prefix = form == null ? null : form.getString("prefix");
            BeanMeta beanMeta = cache.computeIfAbsent(
                    Pair.of(meta.getActualType(), prefix),
                    k -> new BeanMeta(meta.getToolKit(), k.getValue(), k.getKey()));

            ConstructorMeta constructor = beanMeta.constructor;
            ParameterMeta[] parameters = constructor.parameters;
            int len = parameters.length;
            Object[] args = new Object[len];
            for (int i = 0; i < len; i++) {
                args[i] = resolveArgument(parameters[i], request, response);
            }
            Object bean = constructor.newInstance(args);

            for (FieldMeta fieldMeta : beanMeta.fields) {
                fieldMeta.set(bean, resolveArgument(fieldMeta, request, response));
            }

            for (SetMethodMeta methodMeta : beanMeta.methods) {
                methodMeta.invoke(bean, resolveArgument(methodMeta, request, response));
            }
            return bean;
        }

        Object arg = argumentResolver.resolve(meta, request, response);
        return argumentConverter.convert(arg, (Class) meta.getType(), meta);
    }

    private static class BeanMeta {

        private final List<FieldMeta> fields = new ArrayList<>();
        private final List<SetMethodMeta> methods = new ArrayList<>();
        private final ConstructorMeta constructor;

        BeanMeta(RestToolKit toolKit, String prefix, Class<?> type) {
            constructor = resolveConstructor(toolKit, prefix, type);
            resolveFieldAndMethod(toolKit, prefix, type);
        }

        private ConstructorMeta resolveConstructor(RestToolKit toolKit, String prefix, Class<?> type) {
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
                fields.add(new FieldMeta(toolKit, prefix, field));
            }
            for (Method method : type.getDeclaredMethods()) {
                int modifiers = method.getModifiers();
                if ((modifiers & (Modifier.PUBLIC | Modifier.ABSTRACT | Modifier.STATIC)) == Modifier.PUBLIC) {
                    Parameter parameter = method.getParameters()[0];
                    if (parameter.getAnnotations().length == 0) {
                        continue;
                    }
                    String name = method.getName();
                    if (name.length() > 3 && name.startsWith("set")) {
                        name = Character.toLowerCase(name.charAt(3)) + name.substring(4);
                        methods.add(new SetMethodMeta(toolKit, method, parameter, prefix, name));
                    }
                }
            }
            resolveFieldAndMethod(toolKit, prefix, type.getSuperclass());
        }
    }

    private static final class ConstructorMeta {

        private final Constructor<?> constructor;
        private final ConstructorParameterMeta[] parameters;

        ConstructorMeta(RestToolKit toolKit, String prefix, Constructor<?> constructor) {
            this.constructor = constructor;
            parameters = initParameters(toolKit, prefix, constructor);
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

        Object newInstance(Object[] args) throws Exception {
            return constructor.newInstance(args);
        }
    }

    public static final class ConstructorParameterMeta extends ParameterMeta {

        private final Parameter parameter;

        ConstructorParameterMeta(RestToolKit toolKit, Parameter parameter, String prefix) {
            super(toolKit, prefix, parameter.getName());
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
    }

    private static final class FieldMeta extends ParameterMeta {

        private final Field field;

        FieldMeta(RestToolKit toolKit, String prefix, Field field) {
            super(toolKit, prefix, field.getName());
            this.field = field;
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

        public void set(Object bean, Object value) throws Exception {
            field.set(bean, value);
        }
    }

    private static final class SetMethodMeta extends ParameterMeta {

        private final Method method;
        private final Parameter parameter;

        SetMethodMeta(RestToolKit toolKit, Method method, Parameter parameter, String prefix, String name) {
            super(toolKit, prefix, name);
            this.method = method;
            this.parameter = parameter;
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

        public void invoke(Object bean, Object value) throws Exception {
            method.invoke(bean, value);
        }
    }
}
