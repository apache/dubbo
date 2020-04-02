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

import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.common.lang.Prioritized;

import java.util.Collection;

import static org.apache.dubbo.common.utils.TypeUtils.findActualTypeArgument;

/**
 * An interface to convert the source-typed value to multiple value, e.g , Java array, {@link Collection} or
 * sub-interfaces
 *
 * @param <S> The source type
 * @since 2.7.6
 */
@SPI
public interface MultiValueConverter<S> extends Prioritized {

    /**
     * Accept the source type and target type or not
     *
     * @param sourceType     the source type
     * @param multiValueType the multi-value type
     * @return if accepted, return <code>true</code>, or <code>false</code>
     */
    boolean accept(Class<S> sourceType, Class<?> multiValueType);

    /**
     * Convert the source to be the multiple value
     *
     * @param source         the source-typed value
     * @param multiValueType the multi-value type
     * @param elementType    the element type
     * @return
     */
    Object convert(S source, Class<?> multiValueType, Class<?> elementType);

    /**
     * Get the source type
     *
     * @return non-null
     */
    default Class<S> getSourceType() {
        return findActualTypeArgument(getClass(), MultiValueConverter.class, 0);
    }

}
