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
package org.apache.dubbo.rpc.protocol.tri.rest.support.spring;

import org.apache.dubbo.common.beans.factory.ScopeBeanFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.rest.Messages;
import org.apache.dubbo.rpc.protocol.tri.rest.argument.ArgumentConverter;
import org.apache.dubbo.rpc.protocol.tri.rest.argument.ArgumentResolver;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ParameterMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.util.RequestUtils;
import org.apache.dubbo.rpc.protocol.tri.rest.util.RestToolKit;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Map;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.core.convert.ConversionService;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;

final class BeanArgumentBinder {

    private final ArgumentResolver argumentResolver;
    private final ArgumentConverter<?> argumentConverter;
    private final ConversionService conversionService;

    private final Map<Class<?>, ConstructorMeta> cache = CollectionUtils.newConcurrentHashMap();

    BeanArgumentBinder(FrameworkModel frameworkModel, ConversionService conversionService) {
        ScopeBeanFactory beanFactory = frameworkModel.getBeanFactory();
        argumentResolver = beanFactory.getBean(ArgumentResolver.class);
        argumentConverter = beanFactory.getBean(ArgumentConverter.class);
        this.conversionService = conversionService;
    }

    public Object bind(ParameterMeta parameter, HttpRequest request, HttpResponse response) {
        try {
            Object bean = buildBean(parameter, request, response);
            WebDataBinder binder = new WebDataBinder(bean, parameter.getName());
            binder.setConversionService(conversionService);
            binder.bind(new MutablePropertyValues(RequestUtils.getParametersMap(request)));
            BindingResult result = binder.getBindingResult();
            if (result.hasErrors()) {
                String message = "Errors binding onto object '" + result.getObjectName() + "'";
                throw new RuntimeException(message, new BindException(result));
            }
            return binder.getTarget();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Object buildBean(ParameterMeta parameter, HttpRequest request, HttpResponse response) throws Exception {
        Class<?> type = parameter.getActualType();
        if (Modifier.isAbstract(type.getModifiers())) {
            throw new IllegalStateException(Messages.PARAMETER_COULD_NOT_RESOLVED.format(parameter.getDescription()));
        }
        ConstructorMeta ct = cache.computeIfAbsent(type, k -> resolveConstructor(parameter.getToolKit(), k));
        ParameterMeta[] parameters = ct.parameters;
        int len = parameters.length;
        Object[] args = new Object[len];
        for (int i = 0; i < len; i++) {
            Object arg = argumentResolver.resolve(parameter, request, response);
            args[i] = argumentConverter.convert(arg, parameter);
        }
        return ct.newInstance(args);
    }

    private ConstructorMeta resolveConstructor(RestToolKit toolKit, Class<?> type) {
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
        return new ConstructorMeta(toolKit, ct);
    }

    private static final class ConstructorMeta {

        private final Constructor<?> constructor;
        private final ConstructorParameterMeta[] parameters;

        ConstructorMeta(RestToolKit toolKit, Constructor<?> constructor) {
            this.constructor = constructor;
            parameters = initParameters(toolKit, constructor);
        }

        private ConstructorParameterMeta[] initParameters(RestToolKit toolKit, Constructor<?> ct) {
            Parameter[] cps = ct.getParameters();
            int len = cps.length;
            ConstructorParameterMeta[] parameters = new ConstructorParameterMeta[len];
            for (int i = 0; i < len; i++) {
                parameters[i] = new ConstructorParameterMeta(toolKit, cps[i]);
            }
            return parameters;
        }

        Object newInstance(Object[] args) throws Exception {
            return constructor.newInstance(args);
        }
    }

    public static final class ConstructorParameterMeta extends ParameterMeta {

        private final Parameter parameter;

        ConstructorParameterMeta(RestToolKit toolKit, Parameter parameter) {
            super(toolKit, parameter.getName());
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
}
