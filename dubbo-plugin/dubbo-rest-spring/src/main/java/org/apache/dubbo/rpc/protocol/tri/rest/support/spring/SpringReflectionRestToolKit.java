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

import org.apache.dubbo.common.constants.LoggerCodeConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.extension.ExtensionInjector;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.rest.RestConstants;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.MethodParameterMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ParameterMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.util.RestToolKit;
import org.apache.dubbo.rpc.protocol.tri.rest.util.RestUtils;
import org.apache.dubbo.rpc.protocol.tri.rest.util.TypeUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;

@Activate(order = 1000, onClass = "org.springframework.context.ApplicationContext")
public class SpringReflectionRestToolKit implements RestToolKit {

    private static final ErrorTypeAwareLogger LOGGER =
            LoggerFactory.getErrorTypeAwareLogger(SpringReflectionRestToolKit.class);

    private final Object beanFactory;
    private final Object placeholderHelper;
    private final ConfigurationWrapper configuration;
    private final Object conversionService;
    private final Object discoverer;

    public SpringReflectionRestToolKit(FrameworkModel frameworkModel) {
        Object beanFactory = null;
        Object placeholderHelper = null;
        ConfigurationWrapper configuration = null;
        Object conversionService = null;
        Object discoverer = null;
        try {
            ExtensionInjector injector = frameworkModel.getExtension(ExtensionInjector.class, "spring");
            beanFactory = injector.getInstance(Classes.ConfigurableBeanFactory.clazz, null);
            if (beanFactory == null) {
                placeholderHelper = Constructors.PlaceholderHelper.newInstance("${", "}", ":", true);
                configuration = new ConfigurationWrapper(frameworkModel.defaultApplication());
            }
            Object cs = injector.getInstance(Classes.ConversionService.clazz, "mvcConversionService");
            conversionService = cs == null ? Methods.getSharedInstance.invokeStatic() : cs;
            discoverer = Constructors.ParameterNameDiscoverer.newInstance();
        } catch (Throwable t) {
            LOGGER.error(
                    LoggerCodeConstants.INTERNAL_ERROR,
                    "",
                    "",
                    "Failed to initialize " + "SpringReflectionRestToolKit");
        }
        this.beanFactory = beanFactory;
        this.placeholderHelper = placeholderHelper;
        this.configuration = configuration;
        this.conversionService = conversionService;
        this.discoverer = discoverer;
    }

    @Override
    public int getDialect() {
        return RestConstants.DIALECT_SPRING_MVC;
    }

    @Override
    public String resolvePlaceholders(String text) {
        if (RestUtils.hasPlaceholder(text)) {
            if (beanFactory != null) {
                return Methods.resolveEmbeddedValue.invoke(beanFactory, text);
            }
            if (placeholderHelper != null) {
                return Methods.resolvePlaceholders.invoke(placeholderHelper, text, configuration);
            }
            throw failed();
        }
        return text;
    }

    @Override
    public Object convert(Object value, ParameterMeta parameter) {
        if (conversionService == null) {
            throw failed();
        }
        Object targetType = parameter.getTypeDescriptor();
        if (targetType == null) {
            MethodParameterMeta meta = (MethodParameterMeta) parameter;
            Object mp = Constructors.MethodParameter.newInstance(meta.getMethod(), meta.getIndex());
            targetType = Constructors.TypeDescriptor.newInstance(mp);
            parameter.setTypeDescriptor(targetType);
        }
        Object sourceType = Methods.forObject.invokeStatic(value);
        if (Boolean.TRUE.equals(Methods.canConvert.invoke(conversionService, sourceType, targetType))) {
            return Methods.convert.invoke(conversionService, value, sourceType, targetType);
        }
        return null;
    }

    @Override
    public Object bind(ParameterMeta parameter, HttpRequest request, HttpResponse response) {
        if (Classes.ServletRequest.isPresent()) {
            Object binder = Constructors.ServletRequestDataBinder.newInstance(null, parameter.getName());
            Methods.setConversionService.invoke(binder, conversionService);
            Methods.bind.invoke(binder, request);
            try {
                Methods.closeNoCatch.invoke(binder);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return Methods.getTarget.invoke(binder);
        }
        throw failed();
    }

    @Override
    public String[] getParameterNames(Method method) {
        if (discoverer == null) {
            throw failed();
        }
        return Methods.getParameterNames.invoke(discoverer, method);
    }

    @Override
    public Map<String, Object> getAttributes(AnnotatedElement element, Annotation annotation) {
        if (Methods.getMergedAnnotationAttributes.isPresent()) {
            return Methods.getMergedAnnotationAttributes.invokeStatic(element, annotation.annotationType());
        }
        throw failed();
    }

    private IllegalStateException failed() {
        return new IllegalStateException("SpringReflectionRestToolKit initialize failed");
    }

    private enum Classes {
        ConfigurableBeanFactory("beans.factory.config"),
        PropertyPlaceholderHelper("util"),
        ConversionService("core.convert"),
        DefaultConversionService("core.convert.support"),
        MethodParameter("core"),
        TypeDescriptor("core.convert"),
        DefaultParameterNameDiscoverer("core"),
        AnnotatedElementUtils("core.annotation"),
        ServletRequestDataBinder("web.bind"),
        ServletRequest(".javax.servlet.ServletRequest");

        private final Class<?> clazz;

        Classes(String pkg) {
            Class<?> clazz = null;
            try {
                String name;
                if (pkg.charAt(0) == '.') {
                    name = pkg.substring(1);
                } else {
                    name = "org.springframework." + pkg + "." + name();
                }
                clazz = TypeUtils.loadClass(name);
            } catch (Throwable ignored) {
            }
            this.clazz = clazz;
        }

        public boolean isPresent() {
            return clazz != null;
        }
    }

    @SuppressWarnings("unchecked")
    private enum Methods {
        getSharedInstance(Classes.DefaultConversionService),
        resolvePlaceholders(Classes.PropertyPlaceholderHelper, String.class, Properties.class),
        resolveEmbeddedValue(Classes.ConfigurableBeanFactory, String.class),
        forObject(Classes.TypeDescriptor, Object.class),
        canConvert(Classes.ConversionService, Object.class, Classes.TypeDescriptor.clazz, Classes.TypeDescriptor.clazz),
        convert(Classes.ConversionService, Object.class, Classes.TypeDescriptor.clazz, Classes.TypeDescriptor.clazz),
        getParameterNames(Classes.DefaultParameterNameDiscoverer, Method.class),
        getMergedAnnotationAttributes(Classes.AnnotatedElementUtils, AnnotatedElement.class, Class.class),
        bind(Classes.ServletRequestDataBinder, Classes.ServletRequest.clazz),
        closeNoCatch(Classes.ServletRequestDataBinder),
        getTarget(Classes.ServletRequestDataBinder),
        setConversionService(Classes.ServletRequestDataBinder, Classes.ConversionService.clazz);

        private final Method method;

        Methods(Classes clazz, Class<?>... parameterTypes) {
            Method method = null;
            try {
                if (clazz.isPresent()) {
                    method = clazz.clazz.getMethod(name(), parameterTypes);
                }
            } catch (Throwable ignored) {
            }
            this.method = method;
        }

        public boolean isPresent() {
            return method != null;
        }

        <T> T invoke(Object obj, Object... args) {
            if (method == null) {
                return null;
            }
            try {
                return (T) method.invoke(obj, args);
            } catch (RuntimeException t) {
                throw t;
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }

        <T> T invokeStatic(Object... args) {
            if (method == null) {
                return null;
            }
            try {
                return (T) method.invoke(null, args);
            } catch (RuntimeException t) {
                throw t;
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private enum Constructors {
        PlaceholderHelper(Classes.PropertyPlaceholderHelper, String.class, String.class, String.class, boolean.class),
        ParameterNameDiscoverer(Classes.DefaultParameterNameDiscoverer),
        MethodParameter(Classes.MethodParameter, Method.class, int.class),
        TypeDescriptor(Classes.TypeDescriptor, Classes.MethodParameter.clazz),
        ServletRequestDataBinder(Classes.ServletRequestDataBinder, Object.class, String.class);

        private final Constructor<?> constructor;

        Constructors(Classes clazz, Class<?>... parameterTypes) {
            Constructor<?> constructor = null;
            try {
                if (clazz.isPresent()) {
                    constructor = clazz.clazz.getConstructor(parameterTypes);
                }
            } catch (Throwable ignored) {
            }
            this.constructor = constructor;
        }

        public boolean isPresent() {
            return constructor != null;
        }

        <T> T newInstance(Object... args) {
            if (constructor == null) {
                return null;
            }
            try {
                return (T) constructor.newInstance(args);
            } catch (RuntimeException t) {
                throw t;
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
    }
}
