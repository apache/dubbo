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
package org.apache.dubbo.common.convert.multiple;

import org.apache.dubbo.common.convert.StringConverter;

import java.util.Collection;
import java.util.Optional;

import static org.apache.dubbo.common.convert.Converter.getConverter;
import static org.apache.dubbo.common.utils.ClassUtils.getAllInterfaces;
import static org.apache.dubbo.common.utils.ClassUtils.isAssignableFrom;
import static org.apache.dubbo.common.utils.TypeUtils.findActualTypeArgument;

/**
 * The class to convert {@link String} to {@link Iterable}-based value
 *
 * @since 2.7.6
 */
public abstract class StringToIterableConverter<T extends Iterable> implements StringToMultiValueConverter {

    public boolean accept(Class<String> type, Class<?> multiValueType) {
        return isAssignableFrom(getSupportedType(), multiValueType);
    }

    @Override
    public final Object convert(String[] segments, int size, Class<?> multiValueType, Class<?> elementType) {

        Optional<StringConverter> stringConverter = getStringConverter(elementType);

        return stringConverter.map(converter -> {

            T convertedObject = createMultiValue(size, multiValueType);

            if (convertedObject instanceof Collection) {
                Collection collection = (Collection) convertedObject;
                for (int i = 0; i < size; i++) {
                    String segment = segments[i];
                    Object element = converter.convert(segment);
                    collection.add(element);
                }
                return collection;
            }

            return convertedObject;
        }).orElse(null);
    }

    protected abstract T createMultiValue(int size, Class<?> multiValueType);

    protected Optional<StringConverter> getStringConverter(Class<?> elementType) {
        StringConverter converter = (StringConverter) getConverter(String.class, elementType);
        return Optional.ofNullable(converter);
    }

    protected final Class<T> getSupportedType() {
        return findActualTypeArgument(getClass(), StringToIterableConverter.class, 0);
    }

    @Override
    public final int getPriority() {
        int level = getAllInterfaces(getSupportedType(), type ->
                isAssignableFrom(Iterable.class, type)).size();
        return MIN_PRIORITY - level;
    }
}
