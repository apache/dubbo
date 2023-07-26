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
package org.apache.dubbo.common.convert;

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.ConcurrentHashMapUtils;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class ConverterUtil {
    private final FrameworkModel frameworkModel;
    private final ConcurrentMap<Class<?>, ConcurrentMap<Class<?>, List<Converter>>> converterCache = new ConcurrentHashMap<>();

    public ConverterUtil(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
    }

    /**
     * Get the Converter instance from {@link ExtensionLoader} with the specified source and target type
     *
     * @param sourceType the source type
     * @param targetType the target type
     * @return
     * @see ExtensionLoader#getSupportedExtensionInstances()
     */
    public Converter<?, ?> getConverter(Class<?> sourceType, Class<?> targetType) {
        ConcurrentMap<Class<?>, List<Converter>> toTargetMap = ConcurrentHashMapUtils.computeIfAbsent(converterCache, sourceType, (k) -> new ConcurrentHashMap<>());
        List<Converter> converters = ConcurrentHashMapUtils.computeIfAbsent(toTargetMap, targetType, (k) -> frameworkModel.getExtensionLoader(Converter.class)
            .getSupportedExtensionInstances()
            .stream()
            .filter(converter -> converter.accept(sourceType, targetType))
            .collect(Collectors.toList()));

        return converters.size() > 0 ? converters.get(0) : null;
    }

    /**
     * Convert the value of source to target-type value if possible
     *
     * @param source     the value of source
     * @param targetType the target type
     * @param <T>        the target type
     * @return <code>null</code> if can't be converted
     * @since 2.7.8
     */
    public <T> T convertIfPossible(Object source, Class<T> targetType) {
        Converter converter = getConverter(source.getClass(), targetType);
        if (converter != null) {
            return (T) converter.convert(source);
        }
        return null;
    }
}
