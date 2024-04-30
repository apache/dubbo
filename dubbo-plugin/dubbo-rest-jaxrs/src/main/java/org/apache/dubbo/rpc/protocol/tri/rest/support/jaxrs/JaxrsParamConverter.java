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

import org.apache.dubbo.common.extension.ExtensionDirector;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.Pair;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ScopeModel;
import org.apache.dubbo.rpc.protocol.tri.rest.util.TypeUtils;

import javax.ws.rs.ext.ParamConverter;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"rawtypes"})
public class JaxrsParamConverter {

    private final Map<Pair<Class<?>, Class<?>>, ParamConverter> cacheConverter = CollectionUtils.newConcurrentHashMap();

    private final List<ParamConverter> converters;

    JaxrsParamConverter(FrameworkModel frameworkModel) {
        this.converters =
                getExtensionLoader(ParamConverter.class, frameworkModel).getActivateExtensions();
    }

    boolean canConvert(Class<?> sourceType, Class<?> targetType) {
        return getConvert(sourceType, targetType) != null;
    }

    @SuppressWarnings("unchecked")
    Object convert(Class<?> sourceType, Class<?> targetType, Object value) {
        ParamConverter paramConverter = getConvert(sourceType, targetType);
        if (paramConverter == null) {
            return null;
        }

        Class<?> type = TypeUtils.getSuperGenericType(paramConverter.getClass(), 0);
        Object result = null;
        if (sourceType.isAssignableFrom(String.class) && targetType.isAssignableFrom(type)) {
            result = paramConverter.fromString((String) value);
        } else if (targetType.isAssignableFrom(String.class) && sourceType.isAssignableFrom(type)) {
            result = paramConverter.toString(value);
        }
        return result;
    }

    private ParamConverter getConvert(Class<?> sourceType, Class<?> targetType) {
        ParamConverter converter = cacheConverter.get(Pair.of(sourceType, targetType));
        if (converter != null) {
            return converter;
        }

        return cacheConverter.computeIfAbsent(Pair.of(sourceType, targetType), k -> {
            for (ParamConverter paramConverter : converters) {
                Class<?> supportType = TypeUtils.getSuperGenericType(paramConverter.getClass(), 0);
                if (supportType == null) {
                    continue;
                }
                if (sourceType == String.class && targetType.isAssignableFrom(supportType)) {
                    return paramConverter;
                }
            }
            return null;
        });
    }

    private <T> ExtensionLoader<T> getExtensionLoader(Class<T> converterClass, FrameworkModel frameworkModel) {
        ExtensionLoader<T> instance = null;
        try {
            Class<ExtensionLoader> clazz = ExtensionLoader.class;
            Constructor<ExtensionLoader> constructor =
                    clazz.getDeclaredConstructor(Class.class, ExtensionDirector.class, ScopeModel.class);
            constructor.setAccessible(true);
            instance = constructor.newInstance(converterClass, frameworkModel.getExtensionDirector(), frameworkModel);

        } catch (NoSuchMethodException
                | InstantiationException
                | IllegalAccessException
                | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        return instance;
    }
}
