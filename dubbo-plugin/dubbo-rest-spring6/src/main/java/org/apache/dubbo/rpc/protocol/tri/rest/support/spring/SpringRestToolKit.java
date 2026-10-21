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

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.DefaultParameterNameReader;
import org.apache.dubbo.common.utils.ParameterNameReader;
import org.apache.dubbo.config.spring.extension.SpringExtensionInjector;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.rest.Messages;
import org.apache.dubbo.rpc.protocol.tri.rest.RestConstants;
import org.apache.dubbo.rpc.protocol.tri.rest.RestException;
import org.apache.dubbo.rpc.protocol.tri.rest.argument.CompositeArgumentResolver;
import org.apache.dubbo.rpc.protocol.tri.rest.argument.GeneralTypeConverter;
import org.apache.dubbo.rpc.protocol.tri.rest.argument.TypeConverter;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.MethodParameterMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.NamedValueMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ParameterMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.util.RestToolKit;
import org.apache.dubbo.rpc.protocol.tri.rest.util.RestUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.util.PropertyPlaceholderHelper;

final class SpringRestToolKit implements RestToolKit {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringRestToolKit.class);

    private final Map<MethodParameterMeta, TypeDescriptor> cache = CollectionUtils.newConcurrentHashMap();
    private final ConfigurableBeanFactory beanFactory;
    private final PropertyPlaceholderHelper placeholderHelper;
    private final ConfigurationWrapper configuration;
    private final ConversionService conversionService;
    private final TypeConverter typeConverter;
    private final BeanArgumentBinder argumentBinder;
    private final ParameterNameReader parameterNameReader;
    private final CompositeArgumentResolver argumentResolver;

    public SpringRestToolKit(FrameworkModel frameworkModel) {
        ApplicationModel applicationModel = frameworkModel.defaultApplication();
        SpringExtensionInjector injector = SpringExtensionInjector.get(applicationModel);
        ApplicationContext context = injector.getContext();
        if (context instanceof ConfigurableApplicationContext) {
            beanFactory = ((ConfigurableApplicationContext) context).getBeanFactory();
            placeholderHelper = null;
            configuration = null;
        } else {
            beanFactory = null;
            placeholderHelper = new PropertyPlaceholderHelper("${", "}", ":", true);
            configuration = new ConfigurationWrapper(applicationModel);
        }
        if (context != null && context.containsBean("mvcConversionService")) {
            conversionService = context.getBean("mvcConversionService", ConversionService.class);
        } else {
            conversionService = DefaultConversionService.getSharedInstance();
        }
        typeConverter = frameworkModel.getOrRegisterBean(GeneralTypeConverter.class);
        parameterNameReader = frameworkModel.getOrRegisterBean(DefaultParameterNameReader.class);
        argumentResolver = frameworkModel.getOrRegisterBean(CompositeArgumentResolver.class);
        argumentBinder = new BeanArgumentBinder(argumentResolver, conversionService);
    }

    @Override
    public int getDialect() {
        return RestConstants.DIALECT_SPRING_MVC;
    }

    @Override
    public String resolvePlaceholders(String text) {
        if (!RestUtils.hasPlaceholder(text)) {
            return text;
        }
        if (beanFactory != null) {
            return beanFactory.resolveEmbeddedValue(text);
        }
        return placeholderHelper.replacePlaceholders(text, configuration);
    }

    @Override
    public Object convert(Object value, ParameterMeta parameter) {
        boolean tried = false;
        if (value instanceof Collection || value instanceof Map) {
            tried = true;
            Object target = typeConverter.convert(value, parameter.getGenericType());
            if (target != null) {
                return target;
            }
        }
        if (parameter instanceof MethodParameterMeta) {
            TypeDescriptor targetType = cache.computeIfAbsent(
                    (MethodParameterMeta) parameter,
                    k -> new TypeDescriptor(new MethodParameter(k.getMethod(), k.getIndex())));
            TypeDescriptor sourceType = TypeDescriptor.forObject(value);
            if (conversionService.canConvert(sourceType, targetType)) {
                try {
                    return conversionService.convert(value, sourceType, targetType);
                } catch (Throwable t) {
                    LOGGER.debug(
                            "Spring convert value '{}' from type [{}] to type [{}] failed",
                            value,
                            value.getClass(),
                            parameter.getGenericType(),
                            t);
                }
            }
        }
        Object target = tried ? null : typeConverter.convert(value, parameter.getGenericType());
        if (target == null && value != null) {
            throw new RestException(
                    Messages.ARGUMENT_CONVERT_ERROR,
                    parameter.getName(),
                    value,
                    value.getClass(),
                    parameter.getGenericType());
        }
        return target;
    }

    @Override
    public Object bind(ParameterMeta parameter, HttpRequest request, HttpResponse response) {
        return argumentBinder.bind(parameter, request, response);
    }

    @Override
    public NamedValueMeta getNamedValueMeta(ParameterMeta parameter) {
        return argumentResolver.getNamedValueMeta(parameter);
    }

    @Override
    public String[] getParameterNames(Method method) {
        return parameterNameReader.readParameterNames(method);
    }

    @Override
    public String[] getParameterNames(Constructor<?> ctor) {
        return parameterNameReader.readParameterNames(ctor);
    }

    @Override
    public Map<String, Object> getAttributes(AnnotatedElement element, Annotation annotation) {
        return AnnotatedElementUtils.getMergedAnnotationAttributes(element, annotation.annotationType());
    }
}
