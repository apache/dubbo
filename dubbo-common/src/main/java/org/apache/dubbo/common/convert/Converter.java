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

import org.apache.dubbo.common.lang.Prioritized;

import java.util.function.Function;

/**
 * An interface converts an source instance of type {@link S} to an target instance of type {@link T}
 *
 * @param <S> the source type
 * @param <T> the target type
 * @since 2.7.5
 */
public interface Converter<S, T> extends Function<S, T>, Prioritized {

    /**
     * Tests whether or not the specified the source type and the target type
     *
     * @param sourceType the source type
     * @param targetType the target type
     * @return <code>true</code> if support to convert
     */
    default boolean accept(Class<S> sourceType, Class<T> targetType) {
        return true;
    }

    /**
     * Converts an source instance of type {@link S} to an target instance of type {@link T}
     *
     * @param source an source instance of type {@link S}
     * @return an target instance of type {@link T}
     */
    T convert(S source);

    @Override
    default T apply(S s) {
        return convert(s);
    }
}
