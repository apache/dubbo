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
package org.apache.dubbo.rpc.protocol.tri.rest.argument;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.Pair;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ParameterMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.TypeParameterMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.util.TypeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"rawtypes", "unchecked"})
public final class CompositeArgumentConverter implements ArgumentConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompositeArgumentConverter.class);

    private final List<ArgumentConverter> converters;
    private final Map<Pair<Class, Class>, List<ArgumentConverter>> cache = CollectionUtils.newConcurrentHashMap();

    public CompositeArgumentConverter(FrameworkModel frameworkModel) {
        converters = frameworkModel.getActivateExtensions(ArgumentConverter.class);
    }

    @Override
    public Object convert(Object value, ParameterMeta parameter) {
        Class<?> type = parameter.getType();
        if (value == null) {
            return TypeUtils.nullDefault(type);
        }

        if (type.isInstance(value)) {
            if (parameter.getGenericType() instanceof Class) {
                return value;
            }
            return parameter.getToolKit().convert(value, parameter);
        }

        List<ArgumentConverter> converters = getSuitableConverters(value.getClass(), type);
        Object target;
        for (int i = 0, size = converters.size(); i < size; i++) {
            target = converters.get(i).convert(value, parameter);
            if (target != null) {
                return target;
            }
        }

        return parameter.getToolKit().convert(value, parameter);
    }

    public Object convert(Object value, Class<?> type) {
        if (value == null) {
            return null;
        }

        if (type.isInstance(value)) {
            return value;
        }

        TypeParameterMeta parameter = new TypeParameterMeta(type);
        List<ArgumentConverter> converters = getSuitableConverters(value.getClass(), type);
        Object target;
        for (int i = 0, size = converters.size(); i < size; i++) {
            target = converters.get(i).convert(value, parameter);
            if (target != null) {
                return target;
            }
        }

        return null;
    }

    private List<ArgumentConverter> getSuitableConverters(Class sourceType, Class targetType) {
        return cache.computeIfAbsent(Pair.of(sourceType, targetType), k -> {
            List<ArgumentConverter> result = new ArrayList<>();
            for (ArgumentConverter converter : converters) {
                Class<?> supportSourceType = TypeUtils.getSuperGenericType(converter.getClass(), 0);
                if (supportSourceType == null) {
                    continue;
                }
                Class<?> supportTargetType = TypeUtils.getSuperGenericType(converter.getClass(), 1);
                if (supportTargetType == null) {
                    continue;
                }
                if (supportSourceType.isAssignableFrom(sourceType) && targetType.isAssignableFrom(supportTargetType)) {
                    result.add(converter);
                }
            }
            if (result.isEmpty()) {
                return Collections.emptyList();
            }
            LOGGER.info("Found suitable ArgumentConverter for [{}], converters: {}", sourceType, result);
            return result;
        });
    }
}
