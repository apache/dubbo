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

import org.apache.dubbo.common.extension.ExtensionScope;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.common.lang.Prioritized;

import static org.apache.dubbo.common.utils.ClassUtils.isAssignableFrom;
import static org.apache.dubbo.common.utils.TypeUtils.findActualTypeArgument;

/**
 * A class to convert the source-typed value to the target-typed value
 *
 * @param <S> The source type
 * @param <T> The target type
 * @since 2.7.6
 */
@SPI(scope = ExtensionScope.FRAMEWORK)
@FunctionalInterface
public interface Converter<S, T> extends Prioritized {

    /**
     * Accept the source type and target type or not
     *
     * @param sourceType the source type
     * @param targetType the target type
     * @return if accepted, return <code>true</code>, or <code>false</code>
     */
    default boolean accept(Class<?> sourceType, Class<?> targetType) {
        return isAssignableFrom(sourceType, getSourceType()) && isAssignableFrom(targetType, getTargetType());
    }

    /**
     * Convert the source-typed value to the target-typed value
     *
     * @param source the source-typed value
     * @return the target-typed value
     */
    T convert(S source);

    /**
     * Get the source type
     *
     * @return non-null
     */
    default Class<S> getSourceType() {
        return findActualTypeArgument(getClass(), Converter.class, 0);
    }

    /**
     * Get the target type
     *
     * @return non-null
     */
    default Class<T> getTargetType() {
        return findActualTypeArgument(getClass(), Converter.class, 1);
    }
}
