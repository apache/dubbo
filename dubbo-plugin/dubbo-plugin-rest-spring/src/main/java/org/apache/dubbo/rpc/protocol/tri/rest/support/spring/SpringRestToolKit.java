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

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.config.spring.extension.SpringExtensionInjector;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.rest.RestUtils;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.RestToolKit;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ParameterMeta;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Map;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.util.PropertyPlaceholderHelper;

@Activate(order = 100, onClass = "org.springframework.context.ApplicationContext")
public class SpringRestToolKit implements RestToolKit {

    private final ConfigurableBeanFactory beanFactory;
    private final PropertyPlaceholderHelper placeholderHelper;
    private final ConfigurationWrapper configuration;
    private final ConversionService conversionService;
    private final ParameterNameDiscoverer discoverer;

    public SpringRestToolKit(FrameworkModel frameworkModel) {
        SpringExtensionInjector injector = SpringExtensionInjector.get(frameworkModel);
        beanFactory = injector.getInstance(ConfigurableBeanFactory.class, null);
        if (beanFactory == null) {
            placeholderHelper = new PropertyPlaceholderHelper("${", "}", ":", true);
            configuration = new ConfigurationWrapper(frameworkModel.defaultApplication());
        } else {
            placeholderHelper = null;
            configuration = null;
        }
        ConversionService cs = injector.getInstance(ConversionService.class, "mvcConversionService");
        conversionService = cs == null ? DefaultConversionService.getSharedInstance() : cs;
        discoverer = new DefaultParameterNameDiscoverer();
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
        TypeDescriptor targetType = (TypeDescriptor) parameter.getTypeDescriptor();
        if (targetType == null) {
            targetType = new TypeDescriptor(new MethodParameter(parameter.getMethod(), parameter.getIndex()));
            parameter.setTypeDescriptor(targetType);
        }
        return conversionService.convert(value, TypeDescriptor.forObject(value), targetType);
    }

    @Override
    public String[] getParameterNames(Method method) {
        return discoverer.getParameterNames(method);
    }

    @Override
    public Map<String, Object> getAttributes(AnnotatedElement element, Annotation annotation) {
        return AnnotatedElementUtils.getMergedAnnotationAttributes(element, annotation.annotationType());
    }
}
