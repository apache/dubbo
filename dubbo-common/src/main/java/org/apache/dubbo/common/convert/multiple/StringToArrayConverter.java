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

import org.apache.dubbo.common.convert.Converter;

import java.lang.reflect.Array;

import static java.lang.reflect.Array.newInstance;

/**
 * The class to convert {@link String} to array-type object
 *
 * @since 2.7.6
 */
public class StringToArrayConverter implements StringToMultiValueConverter {

    public boolean accept(Class<String> type, Class<?> multiValueType) {
        if (multiValueType != null && multiValueType.isArray()) {
            return true;
        }
        return false;
    }

    @Override
    public Object convert(String[] segments, int size, Class<?> targetType, Class<?> elementType) {

        Class<?> componentType = targetType.getComponentType();

        Converter converter = Converter.getConverter(String.class, componentType);

        Object array = newInstance(componentType, size);

        for (int i = 0; i < size; i++) {
            Array.set(array, i, converter.convert(segments[i]));
        }

        return array;
    }


    @Override
    public int getPriority() {
        return MIN_PRIORITY;
    }
}
