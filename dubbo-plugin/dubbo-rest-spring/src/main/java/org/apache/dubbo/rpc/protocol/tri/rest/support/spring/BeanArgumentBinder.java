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

import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.rpc.protocol.tri.rest.Messages;
import org.apache.dubbo.rpc.protocol.tri.rest.RestException;
import org.apache.dubbo.rpc.protocol.tri.rest.argument.ArgumentResolver;
import org.apache.dubbo.rpc.protocol.tri.rest.argument.CompositeArgumentResolver;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.BeanMeta.ConstructorMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ParameterMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.util.RequestUtils;

import java.lang.reflect.Modifier;
import java.util.Map;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.core.convert.ConversionService;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DataBinder;
import org.springframework.web.bind.WebDataBinder;

import static org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.BeanMeta.resolveConstructor;

final class BeanArgumentBinder {

    private static final Map<Class<?>, ConstructorMeta> CACHE = CollectionUtils.newConcurrentHashMap();

    private final ArgumentResolver argumentResolver;
    private final ConversionService conversionService;

    BeanArgumentBinder(CompositeArgumentResolver argumentResolver, ConversionService conversionService) {
        this.argumentResolver = argumentResolver;
        this.conversionService = conversionService;
    }

    public Object bind(ParameterMeta paramMeta, HttpRequest request, HttpResponse response) {
        String name = StringUtils.defaultIf(paramMeta.getName(), DataBinder.DEFAULT_OBJECT_NAME);
        try {
            Object bean = createBean(paramMeta, request, response);
            WebDataBinder binder = new WebDataBinder(bean, name);
            binder.setConversionService(conversionService);
            binder.bind(new MutablePropertyValues(RequestUtils.getParametersMap(request)));
            BindingResult result = binder.getBindingResult();
            if (result.hasErrors()) {
                throw new BindException(result);
            }
            return binder.getTarget();
        } catch (Exception e) {
            throw new RestException(e, Messages.ARGUMENT_BIND_ERROR, name, paramMeta.getType());
        }
    }

    private Object createBean(ParameterMeta paramMeta, HttpRequest request, HttpResponse response) {
        Class<?> type = paramMeta.getActualType();
        if (Modifier.isAbstract(type.getModifiers())) {
            throw new IllegalStateException(Messages.ARGUMENT_COULD_NOT_RESOLVED.format(paramMeta.getDescription()));
        }
        ConstructorMeta ct = CACHE.computeIfAbsent(type, k -> {
            try {
                return resolveConstructor(paramMeta.getToolKit(), null, type);
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException(Messages.ARGUMENT_COULD_NOT_RESOLVED.format(paramMeta.getDescription())
                        + ", " + e.getMessage());
            }
        });
        ParameterMeta[] parameters = ct.getParameters();
        int len = parameters.length;
        if (len == 0) {
            return ct.newInstance();
        }
        Object[] args = new Object[len];
        for (int i = 0; i < len; i++) {
            ParameterMeta parameter = parameters[i];
            args[i] = parameter.isSimple() ? argumentResolver.resolve(parameter, request, response) : null;
        }
        return ct.newInstance(args);
    }
}
